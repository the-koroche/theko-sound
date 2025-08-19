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

typedef struct {
    IMMDevice* outputDevice;
    IAudioClient* audioClient;
    IAudioRenderClient* renderClient;
    IAudioClock* audioClock;
    HANDLE hEvent;
    UINT32 bufferFrameCount;
    UINT32 bytesPerFrame;
    BOOL isExclusive;
    UINT32 periodInFrames;
    WAVEFORMATEX* format;
    UINT32 pendingFrames;
} OutputContext;

extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_openOut0
    (JNIEnv* env, jobject obj, jboolean isExclusive, jobject jport, jobject jformat, jint bufferSize) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.openOut0");
        ClassesCache* classesCache = getClassesCache(env);

        if (!jport || !jformat) return;

        OutputContext* context = new OutputContext{
            nullptr, nullptr, nullptr, nullptr, nullptr,
            0, 0, FALSE, 0, nullptr, 0
        };
        logger->debug(env, "OutputContext allocated. Address: %p", context);
        context->isExclusive = isExclusive;

        logger->debug(env, "isExclusive: %s", (isExclusive ? "true" : "false"));

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            delete context;
            logger->error(env, "Failed to get IMMDevice.");
            return;
        }
        logger->debug(env, "IMMDevice pointer: %p", device);

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            device->Release();
            delete context;
            logger->error(env, "Failed to get WAVEFORMATEX.");
            return;
        }
        logger->debug(env, "WAVEFORMATEX (Source) pointer: %p", format);

        context->outputDevice = device;

        context->audioClient = nullptr;
        HRESULT hr = context->outputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
        if (FAILED(hr) || !context->audioClient) {
            device->Release();
            delete context;

            char* hr_msg = format_hr_msg(hr);
            logger->error(env, "Failed to get IAudioClient. (%s)", hr_msg);
            free(hr_msg);

            return;
        }
        logger->debug(env, "IAudioClient pointer: %p", context->audioClient);

        if (!isExclusive) {
            logger->debug(env, "Check format support for non-exclusive WASAPI output.");
            WAVEFORMATEX* closestFormat = nullptr;
            hr = context->audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closestFormat);
            if (FAILED(hr)) {
                context->audioClient->Release();
                device->Release();
                delete context;

                char* hr_msg = format_hr_msg(hr);
                logger->error(env, "Failed to check if format is supported. (%s)", hr_msg);
                free(hr_msg);

                return;
            }

            if (hr == S_OK) {
                logger->debug(env, "Format is supported.");
            } else if (hr == S_FALSE && closestFormat) {
                logger->info(env, "Format is not supported, using closest match: WAVEFORMATEX[%d Hz, %d bits, %d channels, isFloat: %d, isPcm: %d]",
                    closestFormat->nSamplesPerSec, closestFormat->wBitsPerSample, closestFormat->nChannels, 
                    closestFormat->wFormatTag == WAVE_FORMAT_IEEE_FLOAT, closestFormat->wFormatTag == WAVE_FORMAT_PCM);
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
        } else {
            logger->debug(env, "Due to exclusive mode, format will not be checked.");
        }

        REFERENCE_TIME hnsPeriodicity, hnsBufferDuration;

        if (isExclusive) {
            REFERENCE_TIME defaultPeriod;
            REFERENCE_TIME minimumPeriod;
            context->audioClient->GetDevicePeriod(&defaultPeriod, &minimumPeriod);

            hnsPeriodicity = minimumPeriod;
            hnsBufferDuration = hnsPeriodicity;
        } else {
            hnsPeriodicity = 10000000 / format->nSamplesPerSec;
            hnsBufferDuration = (REFERENCE_TIME)bufferSize * hnsPeriodicity;
        }

        logger->debug(env, "hnsPeriodicity (in 100-ns): %d", hnsPeriodicity);
        logger->debug(env, "hnsBufferDuration (in 100-ns): %d", hnsBufferDuration);

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

            char* hr_msg = format_hr_msg(hr);
            logger->error(env, "Failed to initialize IAudioClient. (%s)", hr_msg);
            free(hr_msg);

            return;
        }
        logger->debug(env, "IAudioClient initialized.");

        context->renderClient = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioRenderClient), (void**)&context->renderClient);
        if (FAILED(hr) || !context->renderClient) {
            context->audioClient->Release();
            device->Release();
            delete context;

            char* hr_msg = format_hr_msg(hr);
            logger->error(env, "Failed to get IAudioRenderClient. (%s)", hr_msg);
            free(hr_msg);

            return;
        }
        logger->debug(env, "IAudioRenderClient: %p", context->renderClient);

        context->audioClock = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioClock), (void**)&context->audioClock);
        if (FAILED(hr) || !context->audioClock) {
            context->renderClient->Release();
            context->audioClient->Release();
            device->Release();
            delete context;

            char* hr_msg = format_hr_msg(hr);
            logger->error(env, "Failed to get IAudioClock. (%s)", hr_msg);
            free(hr_msg);

            return;
        }
        logger->debug(env, "IAudioClock: %p", context->audioClock);

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

        env->SetLongField(obj, classesCache->wasapiOutput->outputContextPtr, (jlong)context);
        logger->debug(env, "Opened WASAPI output. Handle: %ls", context);
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_closeOut0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.closeOut0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        
        if (context) {
            ULONG audioClockFreeResult,
                renderClientFreeResult,
                audioClientFreeResult,
                outputDeviceFreeResult;
            WINBOOL hEventCloseResult;

            if (context->audioClock) audioClockFreeResult = context->audioClock->Release();
            if (context->renderClient) renderClientFreeResult = context->renderClient->Release();
            if (context->hEvent) hEventCloseResult = CloseHandle(context->hEvent);
            if (context->audioClient) audioClientFreeResult = context->audioClient->Release();
            if (context->outputDevice) outputDeviceFreeResult = context->outputDevice->Release();
            if (context->format) CoTaskMemFree(context->format);
            delete context;
            env->SetLongField(obj, classesCache->wasapiOutput->outputContextPtr, 0);
            logger->debug(env, "IAudioClock Release result: %d", audioClockFreeResult);
            logger->debug(env, "IAudioRenderClient Release result: %d", renderClientFreeResult);
            logger->debug(env, "IAudioClient Release result: %d", audioClientFreeResult);
            logger->debug(env, "IMMDevice Release result: %d", outputDeviceFreeResult);
            logger->debug(env, "Event handle Close result: %s", (hEventCloseResult ? "true" : "false"));
            logger->debug(env, "Closed WASAPI output.");
        } else {
            logger->debug(env, "WASAPI output already closed.");
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_startOut0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.startOut0");
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
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_stopOut0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.stopOut0");
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
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_flushOut0
    (JNIEnv* env, jobject obj) {
        ClassesCache* classesCache = getClassesCache(env);
        env->ThrowNew(classesCache->javaExceptions->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_drainOut0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.drainOut0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        
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
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.availableOut0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

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
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getBufferSizeOut0
    (JNIEnv* env, jobject obj) {
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);
        return (jint)context->bufferFrameCount;
    }


    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getFramePositionOut0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.getFramePositionOut0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, 
            classesCache->wasapiOutput->outputContextPtr);

        UINT64 position = 0;
        UINT64 qpc = 0;
        HRESULT hr = context->audioClock->GetPosition(&position, &qpc);

        if (FAILED(hr)) {
            logger->error(env, "Failed to get WASAPI output position. (HRESULT: 0x%08x)", hr);
            return -1;
        }

        return (jlong)position;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getMicrosecondLatency0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.getMicrosecondLatency0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj,
            classesCache->wasapiOutput->outputContextPtr);

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
        }

        // latency in 100-ns (1e-7 sec), convert to microseconds (1e-6 sec)
        jlong latencyMicroseconds = latency / 10;
        return latencyMicroseconds;
    }

    JNIEXPORT jobject JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_getCurrentAudioPort0
    (JNIEnv* env, jobject obj) {
        Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIOutput.getCurrentAudioPort0");
        ClassesCache* classesCache = getClassesCache(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj,
            classesCache->wasapiOutput->outputContextPtr);

        IMMDevice* device = context->outputDevice;

        if (!device) {
            logger->error(env, "Failed to get IMMDevice.");
            return nullptr;
        }

        jobject jAudioPort = IMMDevice_to_AudioPort(env, device);

        if (!jAudioPort) {
            logger->error(env, "Failed to convert IMMDevice to AudioPort.");
            return nullptr;
        }
        
        return jAudioPort;
    }
}
#else // end of _WIN32
extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_openOut0
    (JNIEnv* env, jobject obj, jboolean isExclusive, jobject jport, jobject jformat, jint bufferSize) {
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