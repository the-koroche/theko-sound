/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
#include <vector>

typedef struct {
    IMMDevice* outputDevice;
    IAudioClient* audioClient;
    IAudioRenderClient* renderClient;
    IAudioClock* audioClock;
    HANDLE hEvent;
    UINT32 bufferFrameCount;
    UINT32 bytesPerFrame;
    BOOL isPendingStop;
    WAVEFORMATEX* format;
    UINT32 pendingFrames;
} OutputContext;

extern "C" {
    inline void cleanupAndLogError(JNIEnv* env, Logger* logger, OutputContext* ctx, HRESULT hr, const char* msg) {
        if(ctx) {
            if(ctx->renderClient) ctx->renderClient->Release();
            if(ctx->audioClock) ctx->audioClock->Release();
            if(ctx->audioClient) ctx->audioClient->Release();
            if(ctx->outputDevice) ctx->outputDevice->Release();
            if(ctx->format) CoTaskMemFree(ctx->format);
            if(ctx->hEvent) CloseHandle(ctx->hEvent);
            delete ctx;
        }
        
        char* hr_msg = format_hr_msg(hr);
        logger->error(env, "%s (%s)", msg, hr_msg);
        free(hr_msg);
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize /* in bytes */) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nOpen");
        ClassesCache* classesCache = getClassesCache(env);

        if (!jport || !jformat) return;

        OutputContext* context = new OutputContext{
            nullptr, nullptr, nullptr, nullptr, nullptr,
            0, 0, FALSE, nullptr, 0
        };
        logger->trace(env, "OutputContext allocated. Pointer: %p", context);

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            cleanupAndLogError(env, logger, context, E_FAIL, "Failed to get IMMDevice.");
            return;
        }
        context->outputDevice = device;
        logger->trace(env, "IMMDevice pointer: %p", device);

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            cleanupAndLogError(env, logger, context, E_FAIL, "Failed to get WAVEFORMATEX.");
            return;
        }
        logger->trace(env, "WAVEFORMATEX (Source) pointer: %p", format);

        context->audioClient = nullptr;
        HRESULT hr = context->outputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
        if (FAILED(hr) || !context->audioClient) {
            cleanupAndLogError(env, logger, context, hr, "Failed to get IAudioClient.");
            return;
        }
        logger->trace(env, "IAudioClient pointer: %p", context->audioClient);

        WAVEFORMATEX* closestFormat = nullptr;
        hr = context->audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closestFormat);
        if (FAILED(hr)) {
            cleanupAndLogError(env, logger, context, hr, "Failed to check format support.");
            return;
        }

        if (hr == S_OK) {
            logger->trace(env, "Format is supported.");
        } else if (hr == S_FALSE && closestFormat) {
            logger->info(env, "Format is not supported, using closest match: WAVEFORMATEX[%d Hz, %d bits, %d channels, isFloat: %d, isPcm: %d]",
                closestFormat->nSamplesPerSec, closestFormat->wBitsPerSample, closestFormat->nChannels, 
                closestFormat->wFormatTag == WAVE_FORMAT_IEEE_FLOAT, closestFormat->wFormatTag == WAVE_FORMAT_PCM);
            logger->trace(env, "Closest format pointer: %p", closestFormat);
            
            CoTaskMemFree(format);
            format = (WAVEFORMATEX*)closestFormat;
        } else {
            logger->error(env, "Format not supported and no closest format found.");
            context->audioClient->Release();
            device->Release();
            delete context;
            return;
        }
        context->format = format;

        int bufferSizeInFrames = bufferSize / format->nBlockAlign;

        logger->debug(env, "Input buffer (in frames): %d", bufferSizeInFrames);

        REFERENCE_TIME hnsBufferDuration = (REFERENCE_TIME)((double)bufferSizeInFrames / format->nSamplesPerSec * 1e7);
        logger->debug(env, "hnsBufferDuration (in 100-ns): %lld", hnsBufferDuration);

        hr = context->audioClient->Initialize(
            AUDCLNT_SHAREMODE_SHARED, 
            AUDCLNT_STREAMFLAGS_EVENTCALLBACK,
            hnsBufferDuration,
            0,
            format, nullptr);
        if (FAILED(hr)) {
            cleanupAndLogError(env, logger, context, hr, "Failed to initialize IAudioClient.");
            return;
        }
        logger->trace(env, "IAudioClient initialized.");

        context->renderClient = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioRenderClient), (void**)&context->renderClient);
        if (FAILED(hr) || !context->renderClient) {
            cleanupAndLogError(env, logger, context, hr, "Failed to get IAudioRenderClient.");
            return;
        }
        logger->trace(env, "IAudioRenderClient pointer: %p", context->renderClient);

        context->audioClock = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioClock), (void**)&context->audioClock);
        if (FAILED(hr) || !context->audioClock) {
            cleanupAndLogError(env, logger, context, hr, "Failed to get IAudioClock.");
            return;
        }
        logger->trace(env, "IAudioClock pointer: %p", context->audioClock);

        context->hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
        context->audioClient->SetEventHandle(context->hEvent);
        logger->trace(env, "Event handle: %p", context->hEvent);

        context->audioClient->GetBufferSize(&context->bufferFrameCount);
        context->bytesPerFrame = format->nBlockAlign;
        context->pendingFrames = 0;

        context->audioClient->GetBufferSize(&context->bufferFrameCount);
        logger->debug(env, "Actual buffer size: %d frames", context->bufferFrameCount);

        env->SetLongField(obj, classesCache->wasapiOutput->outputContextPtr, (jlong)context);
        logger->debug(env, "Opened WASAPI output. ContextPtr: %p", context);
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nClose
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nClose");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->debug(env, "WASAPI output already closed.");
            return;
        }

        std::vector<std::string> errors;

        ULONG audioClockFreeResult = S_OK;
        ULONG renderClientFreeResult = S_OK;
        ULONG audioClientFreeResult = S_OK;
        ULONG outputDeviceFreeResult = S_OK;
        WINBOOL hEventCloseResult = TRUE;

        if (context->audioClock) {
            audioClockFreeResult = context->audioClock->Release();
            if (audioClockFreeResult != S_OK)
                errors.push_back("IAudioClock Release failed. ERRNO: " + std::to_string(audioClockFreeResult));
        }

        if (context->renderClient) {
            renderClientFreeResult = context->renderClient->Release();
            if (renderClientFreeResult != S_OK)
                errors.push_back("IAudioRenderClient Release failed. ERRNO: " + std::to_string(renderClientFreeResult));
        }

        if (context->audioClient) {
            audioClientFreeResult = context->audioClient->Release();
            if (audioClientFreeResult != S_OK)
                errors.push_back("IAudioClient Release failed. ERRNO: " + std::to_string(audioClientFreeResult));
        }

        if (context->outputDevice) {
            outputDeviceFreeResult = context->outputDevice->Release();
            if (outputDeviceFreeResult != S_OK)
                errors.push_back("IMMDevice Release failed. ERRNO: " + std::to_string(outputDeviceFreeResult));
        }

        if (context->hEvent) {
            hEventCloseResult = CloseHandle(context->hEvent);
            if (!hEventCloseResult)
                errors.push_back("CloseHandle failed for event handle.");
        }

        if (context->format) {
            CoTaskMemFree(context->format);
        }

        delete context;
        env->SetLongField(obj, classesCache->wasapiOutput->outputContextPtr, 0);

        if (!errors.empty()) {
            std::string combined = "Failed to close WASAPI output:\n";
            for (auto &e : errors) combined += " - " + e + "\n";
            logger->error(env, combined.c_str());
        } else {
            logger->debug(env, "Closed WASAPI output successfully.");
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nStart
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStart");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        
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
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nStop
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStop");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        
        if (context) {
            context->audioClient->Stop();
            logger->debug(env, "Stopped WASAPI render client.");
            BYTE* pBuffer;
            context->renderClient->GetBuffer(context->bufferFrameCount, &pBuffer);
            context->renderClient->ReleaseBuffer(0, AUDCLNT_BUFFERFLAGS_SILENT);
            context->pendingFrames = 0;
            logger->debug(env, "Flushed WASAPI buffer.");
        } else {
            logger->trace(env, "WASAPI output not opened.");
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nFlush
    (JNIEnv* env, jobject obj) {
        ClassesCache* classesCache = getClassesCache(env);
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nDrain
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nDrain");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        
        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return;
        }

        UINT32 padding;
        do {
            context->audioClient->GetCurrentPadding(&padding);
            if (padding == 0) break;
            WaitForSingleObject(context->hEvent, 100);
        } while (true);
        context->pendingFrames = 0;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nWrite
    (JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nWrite");

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            getClassesCache(env)->wasapiOutput->outputContextPtr);
        
        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        jbyte* src = env->GetByteArrayElements(buffer, NULL);
        UINT32 totalFrames = length / context->bytesPerFrame;
        UINT32 framesWritten = 0;

        // logger->trace(env, "Writing %d frames.", totalFrames);

        while (framesWritten < totalFrames) {
            UINT32 availableFrames;
            UINT32 padding;
            context->audioClient->GetCurrentPadding(&padding);
            availableFrames = context->bufferFrameCount - padding;
            if (availableFrames == 0) {
                WaitForSingleObject(context->hEvent, INFINITE);
                continue;
            }
            // logger->trace(env, "Available frames: %d", availableFrames);

            UINT32 framesToWrite = std::min(availableFrames, totalFrames - framesWritten);
            // logger->trace(env, "Writing %d frames.", framesToWrite);
            BYTE* dest;
            HRESULT hr = context->renderClient->GetBuffer(framesToWrite, &dest);
            if (FAILED(hr)) { 
                char* hr_msg = format_hr_msg(hr);
                logger->error(env, "Failed to get WASAPI output buffer. (%s)", hr_msg);
                free(hr_msg);
                return -1;
            }

            memcpy(dest, src + offset + framesWritten * context->bytesPerFrame, 
                framesToWrite * context->bytesPerFrame);
            
            hr = context->renderClient->ReleaseBuffer(framesToWrite, 0);
            if (FAILED(hr)) {
                char* hr_msg = format_hr_msg(hr);
                logger->error(env, "Failed to release WASAPI output buffer. (%s)", hr_msg);
                free(hr_msg);
                return -1;
            }
            framesWritten += framesToWrite;
            context->pendingFrames += framesToWrite;
        }

        env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
        return framesWritten * context->bytesPerFrame;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nAvailable
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nAvailable");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        UINT32 padding = 0;
        HRESULT hr = context->audioClient->GetCurrentPadding(&padding);
        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            char* fmsg = format("Failed to get WASAPI output buffer. (%s)", hr_msg);
            free(hr_msg);
            logger->error(env, fmsg);
            env->ThrowNew(classesCache->thekoSoundExceptions->audioBackendException, fmsg);
            free(fmsg);
            return -1;
        }

        UINT32 availableFrames = context->bufferFrameCount - padding;
        return (jint)availableFrames;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetBufferSize
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetBufferSize");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        return (jint)context->bufferFrameCount;
    }


    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetFramePosition
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetFramePosition");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        UINT64 position = 0;
        UINT64 qpc = 0;
        HRESULT hr = context->audioClock->GetPosition(&position, &qpc);

        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            logger->error(env, "Failed to get WASAPI output position. (%s)", hr_msg);
            free(hr_msg);
            return -1;
        }

        return (jlong)position;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetMicrosecondLatency");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj,
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        REFERENCE_TIME latency = 0;
        HRESULT hr = context->audioClient->GetStreamLatency(&latency);
        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            char* fmsg = format("Failed to get WASAPI output latency. (%s)", hr_msg);
            free(hr_msg);
            logger->error(env, fmsg);
            env->ThrowNew(classesCache->thekoSoundExceptions->audioBackendException, fmsg);
            free(fmsg);
            return -1;
        } else if (SUCCEEDED(hr) && latency > 0) {
            // latency in 100-ns (1e-7 sec), converted to microseconds (1e-6 sec)
            jlong latencyMicroseconds = latency / 10;
            return latencyMicroseconds;
        } else if (latency == 0) {
            double seconds = (double)context->bufferFrameCount / (double)context->format->nSamplesPerSec;
            jlong latency = (jlong)(seconds * 1000000.0);
            return latency;
        }

        return -1;
    }

    JNIEXPORT jobject JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetCurrentAudioPort
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetCurrentAudioPort");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj,
            classesCache->wasapiOutput->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return nullptr;
        }

        IMMDevice* device = context->outputDevice;

        if (!device) {
            logger->error(env, "Failed to get IMMDevice.");
            env->ThrowNew(classesCache->thekoSoundExceptions->audioBackendException, "Failed to get IMMDevice.");
            return nullptr;
        }

        jobject jAudioPort = IMMDevice_to_AudioPort(env, device);

        if (!jAudioPort) {
            logger->error(env, "Failed to convert IMMDevice to AudioPort.");
            env->ThrowNew(classesCache->thekoSoundExceptions->audioBackendException, "Failed to convert IMMDevice to AudioPort.");
            return nullptr;
        }
        
        return jAudioPort;
    }
}
#else // end of _WIN32
extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_openOut0
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_closeOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_startOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_stopOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_flushOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_drainOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_writeOut0
    (JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_availableOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getBufferSizeOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getFramePositionOut0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getMicrosecondLatency0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jobject JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getCurrentAudioPort0
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }
}
#endif // end of !_WIN32