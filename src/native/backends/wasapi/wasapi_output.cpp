#include <jni.h>

#include "helper_utilities.hpp"
#include "classes_cache.hpp"

#include "org_theko_sound_backend_wasapi_WASAPISharedOutput.h"

#ifdef _WIN32
#include <windows.h>
#include <initguid.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>

#include "wasapi_utils.hpp"
#include "wasapi_bridge.hpp"

#include "logger.hpp"
#include "logger_manager.hpp"
#include <algorithm>

typedef struct {
    IMMDevice* outputDevice;
    IAudioClient* audioClient;
    IAudioRenderClient* renderClient;
    HANDLE hEvent;
    UINT32 bufferFrameCount;
    UINT32 bytesPerFrame;
    BOOL isExclusive;
    UINT32 periodInFrames;
    WAVEFORMATEX* format;
    UINT32 pendingFrames;
} OutputContext;

JNIEXPORT void JNICALL 
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_openOut0
(JNIEnv* env, jobject obj, jboolean isExclusive, jobject jport, jobject jformat, jint bufferSize) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.openOut0");

    if (!jport || !jformat) return;

    OutputContext* context = new OutputContext{ nullptr };
    logger->debug(env, "OutputContext allocated. Address: %p", context);
    context->isExclusive = isExclusive;

    IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
    if (!device) {
        delete context;
        logger->error(env, "Failed to get IMMDevice.");
        return;
    }
    logger->debug(env, "IMMDevice: %p", device);

    WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
    if (!format) {
        device->Release();
        delete context;
        logger->error(env, "Failed to get WAVEFORMATEX.");
        return;
    }
    logger->debug(env, "WAVEFORMATEX: %p", format);

    context->outputDevice = device;

    context->audioClient = nullptr;
    HRESULT hr = context->outputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
    if (FAILED(hr) || !context->audioClient) {
        device->Release();
        delete context;
        logger->error(env, "Failed to get IAudioClient. (HRESULT: 0x%08x)", hr);
        return;
    }
    logger->debug(env, "IAudioClient: %p", context->audioClient);

    WAVEFORMATEX* closestFormat = nullptr;
    hr = context->audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closestFormat);
    if (FAILED(hr)) {
        context->audioClient->Release();
        device->Release();
        delete context;
        logger->error(env, "Failed to check if format is supported. (HRESULT: 0x%08x)", hr);
        return;
    }

    if (hr == S_OK) {
        logger->debug(env, "Format is supported.");
    } else if (hr == S_FALSE && closestFormat) {
        logger->info(env, "Format is not supported, using closest match: WAVEFORMATEX[%d Hz, %d bits, %d channels]",
            closestFormat->nSamplesPerSec, closestFormat->wBitsPerSample, closestFormat->nChannels);
        logger->debug(env, "Closest format pointer: %p", closestFormat);
        
        CoTaskMemFree(format);
        format = (WAVEFORMATEX*)closestFormat;
    } else {
        logger->error(env, "Format not supported and no closest format found.");
        context->audioClient->Release();
        device->Release();
        delete context;
        return;
    }

    REFERENCE_TIME hnsPeriodicity = 10000000 / format->nSamplesPerSec;
    REFERENCE_TIME hnsBufferDuration = (REFERENCE_TIME)bufferSize * hnsPeriodicity;

    logger->debug(env, "hnsPeriodicity: %d", hnsPeriodicity);
    logger->debug(env, "hnsBufferDuration: %d", hnsBufferDuration);

    hr = context->audioClient->Initialize(
        isExclusive ? AUDCLNT_SHAREMODE_EXCLUSIVE : AUDCLNT_SHAREMODE_SHARED, 
        AUDCLNT_STREAMFLAGS_EVENTCALLBACK,
        hnsBufferDuration,
        isExclusive ? hnsPeriodicity : 0,
        format, nullptr);
    if (FAILED(hr)) {
        context->audioClient->Release();
        device->Release();
        delete context;
        logger->error(env, "Failed to initialize IAudioClient. (HRESULT: 0x%08x)", hr);
        return;
    }
    logger->debug(env, "IAudioClient initialized.");

    context->renderClient = nullptr;
    hr = context->audioClient->GetService(__uuidof(IAudioRenderClient), (void**)&context->renderClient);
    if (FAILED(hr) || !context->renderClient) {
        context->audioClient->Release();
        device->Release();
        delete context;
        logger->error(env, "Failed to get IAudioRenderClient. (HRESULT: 0x%08x)", hr);
        return;
    }
    logger->debug(env, "IAudioRenderClient: %p", context->renderClient);

    REFERENCE_TIME defaultPeriod, minPeriod;
    context->audioClient->GetDevicePeriod(&defaultPeriod, &minPeriod);
    context->periodInFrames = (isExclusive) ? 
        static_cast<UINT32>((minPeriod * format->nSamplesPerSec) / 10000000) : 0;
    logger->debug(env, "Default period in frames: %d", context->periodInFrames);

    context->hEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
    context->audioClient->SetEventHandle(context->hEvent);
    logger->debug(env, "Event handle: %p", context->hEvent);

    context->audioClient->GetBufferSize(&context->bufferFrameCount);
    context->bytesPerFrame = format->nBlockAlign;
    context->pendingFrames = 0;
    context->format = format;

    env->SetLongField(obj, getClassesCache(env)->wasapiOutput->outputContextPtr, (jlong)context);
    logger->debug(env, "Opened WASAPI output. Handle: %ls", context);
}

JNIEXPORT void JNICALL 
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_closeOut0
(JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.closeOut0");

    OutputContext* context = (OutputContext*)env->GetLongField(obj, 
        getClassesCache(env)->wasapiOutput->outputContextPtr);
    
    if (context) {
        if (context->renderClient) context->renderClient->Release();
        if (context->audioClient) context->audioClient->Release();
        if (context->outputDevice) context->outputDevice->Release();
        delete context;
        env->SetLongField(obj, getClassesCache(env)->wasapiOutput->outputContextPtr, 0);
        logger->debug(env, "Closed WASAPI output.");
    } else {
        logger->debug(env, "WASAPI output already closed.");
    }
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_startOut0
(JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.startOut0");

    OutputContext* context = (OutputContext*)env->GetLongField(obj, 
        getClassesCache(env)->wasapiOutput->outputContextPtr);
    
    if (context) {
        HRESULT hr = context->audioClient->Start();
        if (FAILED(hr)) {
            logger->error(env, "Failed to start WASAPI output.");
        } else {
            logger->debug(env, "Started WASAPI output.");
        }
    }
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_stopOut0
(JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.stopOut0");

    OutputContext* context = (OutputContext*)env->GetLongField(obj, 
        getClassesCache(env)->wasapiOutput->outputContextPtr);
    
    if (context) {
        context->audioClient->Stop();
        logger->debug(env, "Stopped WASAPI render client.");
        BYTE* pBuffer;
        context->renderClient->GetBuffer(context->bufferFrameCount, &pBuffer);
        context->renderClient->ReleaseBuffer(0, AUDCLNT_BUFFERFLAGS_SILENT);
        context->pendingFrames = 0;
        logger->debug(env, "Flushed WASAPI buffer.");
    }
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_flushOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_drainOut0
(JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.drainOut0");

    OutputContext* context = (OutputContext*)env->GetLongField(obj, 
        getClassesCache(env)->wasapiOutput->outputContextPtr);
    
    if (!context) {
        logger->warn(env, "WASAPI output not opened.");
        return;
    }

    if (context->isExclusive) {
        UINT32 latencyFrames = context->pendingFrames;
        DWORD sleepMs = (latencyFrames * 1000) / context->format->nSamplesPerSec;
        Sleep(sleepMs);
    } else {
        UINT32 padding;
        do {
            context->audioClient->GetCurrentPadding(&padding);
            if (padding == 0) break;
            WaitForSingleObject(context->hEvent, 100);
        } while (true);
    }
    context->pendingFrames = 0;
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_writeOut0
(JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.writeOut0");

    OutputContext* context = (OutputContext*)env->GetLongField(obj, 
        getClassesCache(env)->wasapiOutput->outputContextPtr);
    
    if (!context) {
        logger->warn(env, "WASAPI output not opened.");
        return -1;
    }

    jbyte* src = env->GetByteArrayElements(buffer, NULL);
    UINT32 totalFrames = length / context->bytesPerFrame;
    UINT32 framesWritten = 0;

    while (framesWritten < totalFrames) {
        UINT32 availableFrames;
        if (context->isExclusive) {
            WaitForSingleObject(context->hEvent, INFINITE);
            availableFrames = context->periodInFrames;
        } else {
            UINT32 padding;
            context->audioClient->GetCurrentPadding(&padding);
            availableFrames = context->bufferFrameCount - padding;
            if (availableFrames == 0) {
                WaitForSingleObject(context->hEvent, INFINITE);
                continue;
            }
        }

        UINT32 framesToWrite = std::min(availableFrames, totalFrames - framesWritten);
        BYTE* dest;
        HRESULT hr = context->renderClient->GetBuffer(framesToWrite, &dest);
        if (FAILED(hr)) { 
            logger->error(env, "Failed to get WASAPI output buffer. (HRESULT: 0x%08x)", hr);
            return -1;
        }

        memcpy(dest, src + offset + framesWritten * context->bytesPerFrame, 
               framesToWrite * context->bytesPerFrame);
        
        context->renderClient->ReleaseBuffer(framesToWrite, 0);
        framesWritten += framesToWrite;
        context->pendingFrames += framesToWrite;
    }

    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
    return framesWritten * context->bytesPerFrame;
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_availableOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getBufferSizeOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jlong JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getFramePositionOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jlong JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getMicrosecondLatency0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jobject JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getCurrentAudioPort0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return nullptr;
}
#else // end of _WIN32
JNIEXPORT void JNICALL 
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_openOut0
(JNIEnv* env, jobject obj, jboolean isExclusive, jobject jport, jobject jformat, jint bufferSize) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL 
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_closeOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_startOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_stopOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_flushOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_drainOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_writeOut0
(JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_availableOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jint JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getBufferSizeOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jlong JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getFramePositionOut0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jlong JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getMicrosecondLatency0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return -1;
}

JNIEXPORT jobject JNICALL
Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getCurrentAudioPort0
(JNIEnv* env, jobject obj) {
    env->ThrowNew(getClassesCache(env)->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    return nullptr;
}
#endif // end of !_WIN32