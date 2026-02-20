/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

#include "DefaultClassesCache.hpp"

#include "logger.hpp"
#include "logger_manager.hpp"

#include "org_theko_sound_backends_wasapi_WASAPISharedOutput.h"

#include <windows.h>
#include <initguid.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <algorithm>
#include <atomic>
#include <codecvt>
#include <queue>
#include <string>
#include <vector>

#include "wasapi_utils.hpp"
#include "wasapi_bridge.hpp"

#define EVENT_AUDIO_BUFFER_READY 0
#define EVENT_STOP_REQUEST 1

namespace theko::sound::backend::wasapi::output {

class OutputContext {
private:
    std::mutex logMutex;

public:
    IMMDevice* outputDevice;
    IAudioClient* audioClient;
    IAudioRenderClient* renderClient;
    IAudioClock* audioClock;
    HANDLE events[2];
    UINT32 bufferFrameCount;
    UINT32 bytesPerFrame;
    WAVEFORMATEX* format;
    UINT32 pendingFrames;
    IMMDeviceEnumerator* deviceEnumerator;
    IMMNotificationClient* notificationClient;
    std::queue<std::string> notifierLogs;

    OutputContext() {
        outputDevice = nullptr;
        audioClient = nullptr;
        renderClient = nullptr;
        audioClock = nullptr;
        events[0] = nullptr;
        events[1] = nullptr;
        bufferFrameCount = 0;
        bytesPerFrame = 0;
        format = nullptr;
        pendingFrames = 0;
        deviceEnumerator = nullptr;
        notificationClient = nullptr;
    }

    OutputContext(const OutputContext&) = delete;
    OutputContext& operator=(const OutputContext&) = delete;

    ~OutputContext() {
        if (audioClient) {
            audioClient->Stop();
        }

        if (renderClient) renderClient->Release();
        if (audioClock) audioClock->Release();
        if (audioClient) audioClient->Release();
        if (outputDevice) outputDevice->Release();

        if (events[0]) CloseHandle(events[0]);
        if (events[1]) CloseHandle(events[1]);

        if (format) CoTaskMemFree(format);
        if (deviceEnumerator) deviceEnumerator->Release();
        if (notificationClient) notificationClient->Release();
    }

    void pushLog(const std::string& message) {
        std::lock_guard<std::mutex> lock(logMutex);
        notifierLogs.push(message);
    }

    bool popLog(std::string& message) {
        std::lock_guard<std::mutex> lock(logMutex);
        if (notifierLogs.empty()) return false;
        message = notifierLogs.front();
        notifierLogs.pop();
        return true;
    }

    bool isEmptyLogs() {
        std::lock_guard<std::mutex> lock(logMutex);
        return notifierLogs.empty();
    }
};

class OutputDeviceChangeNotifier : public IMMNotificationClient {
private:
    std::atomic<ULONG> refCount;
    OutputContext* context;
    HANDLE hStopEvent;

public:
    OutputDeviceChangeNotifier(OutputContext* ctx)
        : refCount(1), context(ctx), hStopEvent(ctx->events[EVENT_STOP_REQUEST]) {}

    ~OutputDeviceChangeNotifier() {}

    ULONG STDMETHODCALLTYPE AddRef() override {
        return ++refCount;
    }

    ULONG STDMETHODCALLTYPE Release() override {
        ULONG count = --refCount;
        if (count == 0) {
            delete this;
        }
        return count;
    }

    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void** ppv) override {
        if (riid == IID_IUnknown || riid == __uuidof(IMMNotificationClient)) {
            *ppv = this;
            AddRef();
            return S_OK;
        }
        *ppv = nullptr;
        return E_NOINTERFACE;
    }

    HRESULT STDMETHODCALLTYPE OnDeviceStateChanged(LPCWSTR pwstrDeviceId, DWORD dwNewState) override {
        std::string msg = "Device state changed: " + utf16_to_utf8(pwstrDeviceId)
                        + " -> " + std::to_string(dwNewState);
        context->notifierLogs.push(msg);

        if (dwNewState == DEVICE_STATE_NOTPRESENT || dwNewState == DEVICE_STATE_UNPLUGGED) {
            interruptPlayback();
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDefaultDeviceChanged(EDataFlow flow, ERole role, LPCWSTR pwstrDefaultDeviceId) override {
        std::string msg = "Default device changed: " 
                        + utf16_to_utf8(pwstrDefaultDeviceId)
                        + ", flow: " + (flow == eRender ? "Render" : "Capture")
                        + ", role: " + std::to_string(role);
        context->notifierLogs.push(msg);
        interruptPlayback();
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDeviceAdded(LPCWSTR pwstrDeviceId) override {
        std::string msg = "Device added: " 
                        + utf16_to_utf8(pwstrDeviceId);
        context->notifierLogs.push(msg);
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDeviceRemoved(LPCWSTR pwstrDeviceId) override {
        std::string msg = "Device removed: " 
                        + utf16_to_utf8(pwstrDeviceId);
        context->notifierLogs.push(msg);
        interruptPlayback();
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnPropertyValueChanged(LPCWSTR pwstrDeviceId, const PROPERTYKEY key) override {
        if (key == PKEY_AudioEngine_DeviceFormat) {
            std::string msg = "Device format changed: " 
                            + utf16_to_utf8(pwstrDeviceId);
            context->notifierLogs.push(msg);
            interruptPlayback();
        } else if (key == PKEY_DeviceInterface_Enabled) {
            std::string msg = "Device interface enabled changed: " 
                            + utf16_to_utf8(pwstrDeviceId);
            context->notifierLogs.push(msg);
            interruptPlayback();
        }
        return S_OK;
    }

    void interruptPlayback() {
        std::string msg = "Interrupting playback due to device change";
        context->notifierLogs.push(msg);

        if (hStopEvent) {
            SetEvent(hStopEvent);
        }
    }
};

extern "C" {
    static inline void cleanupContext(JNIEnv* env, Logger* logger, OutputContext* ctx) {
        if(ctx) {
            delete ctx;
        }
    }
    
    inline void cleanupAndThrowError(
        JNIEnv* env,
        Logger* logger,
        OutputContext* ctx,
        HRESULT hr,
        const char* msg
        ) {
        const char* hr_msg = fmtHR(hr);
        logger->error(env, "%s (%s).", msg, hr_msg);

        logger->trace(env, "Cleaning up output context...");
        cleanupContext(env, logger, ctx);
        logger->trace(env, "Output context cleaned up, throwing exception...");

        env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, msg);
    }

    inline void logNotifierMessages(JNIEnv* env, Logger* logger, OutputContext* ctx) {
        if (!ctx) return;
        
        std::string log;
        while (ctx->popLog(log)) {
            logger->debug(env, "%s", log.c_str());
        }
    }

    JNIEXPORT jlong JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize /* in bytes */, jobject jAtomicRefFormat) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nOpen");

        if (!jport || !jformat || !jAtomicRefFormat) return 0;

        auto context = new OutputContext();
        logger->trace(env, "OutputContext allocated. Pointer: %s", FORMAT_PTR(context));

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get IMMDevice.");
            return 0;
        }
        context->outputDevice = device;
        logger->trace(env, "IMMDevice pointer: %s", FORMAT_PTR(device));

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get WAVEFORMATEX.");
            return 0;
        }
        logger->trace(env, "WAVEFORMATEX (Request): %s. Pointer: %s", WAVEFORMATEX_toText(format), FORMAT_PTR(format));

        context->audioClient = nullptr;
        HRESULT hr = context->outputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
        if (FAILED(hr) || !context->audioClient) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioClient.");
            return 0;
        }
        logger->trace(env, "IAudioClient pointer: %s", FORMAT_PTR(context->audioClient));

        WAVEFORMATEX* closestFormat = nullptr;
        hr = context->audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closestFormat);
        if (FAILED(hr)) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to check format support.");
            return 0;
        }

        if (hr == S_OK) {
            logger->trace(env, "Format is supported.");
        } else if (hr == S_FALSE && closestFormat) {
            logger->debug(env, "Format is not supported, using closest match: %s" , WAVEFORMATEX_toText(closestFormat));
            logger->trace(env, "Closest format pointer: %s", FORMAT_PTR(closestFormat));
            
            CoTaskMemFree(format);
            format = (WAVEFORMATEX*)closestFormat;
        } else {
            CoTaskMemFree(closestFormat);
            CoTaskMemFree(format);
            cleanupAndThrowError(env, logger, context, hr, "Failed to check format support.");
            return 0;
        }
        context->format = format;

        int bufferSizeInFrames = bufferSize / format->nBlockAlign;

        logger->debug(env, "Input buffer (in frames): %d", bufferSizeInFrames);

        REFERENCE_TIME hnsBufferDuration = (REFERENCE_TIME)((double)bufferSizeInFrames / format->nSamplesPerSec * 1e7);
        logger->debug(env, "hnsBufferDuration (in 100-ns): %lld", hnsBufferDuration);
        
        logger->trace(env, "Trying to initialize IAudioClient...");
        hr = context->audioClient->Initialize(
            AUDCLNT_SHAREMODE_SHARED,
            AUDCLNT_STREAMFLAGS_EVENTCALLBACK,
            hnsBufferDuration,
            0,
            format,
            nullptr 
        );
        logger->trace(env, "IAudioClient::Initialize called. Result: %s", fmtHR(hr));
        if (FAILED(hr) || hr == AUDCLNT_E_DEVICE_IN_USE) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to initialize IAudioClient.");
            return 0;
        }
        logger->trace(env, "IAudioClient initialized.");

        context->renderClient = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioRenderClient), (void**)&context->renderClient);
        if (FAILED(hr) || !context->renderClient) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioRenderClient.");
            return 0;
        }
        logger->trace(env, "IAudioRenderClient pointer: %s", FORMAT_PTR(context->renderClient));

        context->audioClock = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioClock), (void**)&context->audioClock);
        if (FAILED(hr) || !context->audioClock) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioClock.");
            return 0;
        }
        logger->trace(env, "IAudioClock pointer: %s", FORMAT_PTR(context->audioClock));

        context->events[EVENT_AUDIO_BUFFER_READY] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_AUDIO_BUFFER_READY]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio callback event.");
            return 0;
        }
        context->audioClient->SetEventHandle(context->events[EVENT_AUDIO_BUFFER_READY]);
        logger->trace(env, "Event handle: %s", FORMAT_PTR(context->events[EVENT_AUDIO_BUFFER_READY]));

        context->events[EVENT_STOP_REQUEST] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_STOP_REQUEST]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create stop event.");
            return 0;
        }
        logger->trace(env, "Stop event handle: %s", FORMAT_PTR(context->events[EVENT_STOP_REQUEST]));

        context->audioClient->GetBufferSize(&context->bufferFrameCount);
        context->bytesPerFrame = format->nBlockAlign;
        context->pendingFrames = 0;

        context->audioClient->GetBufferSize(&context->bufferFrameCount);
        logger->debug(env, "Actual buffer size: %d frames", context->bufferFrameCount);

        hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL,
            CLSCTX_ALL, __uuidof(IMMDeviceEnumerator),
            (void**)&context->deviceEnumerator);
    
        if (SUCCEEDED(hr)) {
            OutputDeviceChangeNotifier* notifier = new OutputDeviceChangeNotifier(context);
            hr = context->deviceEnumerator->RegisterEndpointNotificationCallback(notifier);
            
            if (SUCCEEDED(hr)) {
                context->notificationClient = notifier;
                notifier->AddRef();
                logger->trace(env, "Device change notification registered");
            } else {
                notifier->Release();
                logger->warn(env, "Failed to register device notifications");
            }
        } else {
            logger->warn(env, "Failed to create device enumerator");
        }

        jobject jAudioFormat = JNIUtil_CreateGlobal(env, WAVEFORMATEX_to_AudioFormat(env, context->format));
        if (!jAudioFormat) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio format.");
            return 0;
        }
        AtomicReferenceCache* audioFormatCache = AtomicReferenceCache::get(env);
        env->CallVoidMethod(jAtomicRefFormat, audioFormatCache->setMethod, jAudioFormat);

        logger->debug(env, "Opened WASAPI output. ContextPtr: %s", FORMAT_PTR(context));

        return (jlong)context;
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nClose
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nClose");
        
        
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->debug(env, "WASAPI output already closed.");
            return;
        }

        logNotifierMessages(env, logger, context);

        if (context->audioClock) {
            ULONG refCount = context->audioClock->Release();
            if (refCount > 0) {
                logger->warn(env, "IAudioClock still has %lu references.", refCount);
            }
            context->audioClock = nullptr;
        }

        if (context->renderClient) {
            ULONG refCount = context->renderClient->Release();
            if (refCount > 0) {
                logger->warn(env, "IAudioRenderClient still has %lu references.", refCount);
            }
            context->renderClient = nullptr;
        }

        if (context->audioClient) {
            ULONG refCount = context->audioClient->Release();
            if (refCount > 0) {
                logger->warn(env, "IAudioClient still has %lu references.", refCount);
            }
            context->audioClient = nullptr;
        }

        if (context->outputDevice) {
            ULONG refCount = context->outputDevice->Release();
            if (refCount > 0) {
                logger->warn(env, "IMMDevice still has %lu references.", refCount);
            }
            context->outputDevice = nullptr;
        }

        if (context->deviceEnumerator && context->notificationClient) {
            context->deviceEnumerator->UnregisterEndpointNotificationCallback(
                context->notificationClient);
            logger->debug(env, "Device change notification unregistered");
        }

        if (context->deviceEnumerator) {
            ULONG refCount = context->deviceEnumerator->Release();
            if (refCount > 0) {
                logger->warn(env, "IMMDeviceEnumerator still has %lu references.", refCount);
            }
            context->deviceEnumerator = nullptr;
        }

        if (context->notificationClient) {
            context->notificationClient->Release();
            context->notificationClient = nullptr;
        }

        if (context->events[EVENT_AUDIO_BUFFER_READY]) {
            CloseHandle(context->events[EVENT_AUDIO_BUFFER_READY]);
            context->events[EVENT_AUDIO_BUFFER_READY] = nullptr;
        }

        if (context->events[EVENT_STOP_REQUEST]) {
            CloseHandle(context->events[EVENT_STOP_REQUEST]);
            context->events[EVENT_STOP_REQUEST] = nullptr;
        }

        delete context;
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nStart
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStart");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return;
        }
        
        if (context) {
            logNotifierMessages(env, logger, context);

            HRESULT hr = context->audioClient->Start();
            if (FAILED(hr)) {
                logger->error(env, "Failed to start WASAPI output (%s).", fmtHR(hr));
            } else {
                logger->trace(env, "Started WASAPI output.");
            }
        }
    }

    void flushBuffer(JNIEnv* env, OutputContext* context, Logger* logger) {
        if (!context) return;

        UINT32 paddingFrames = 0;
        HRESULT hrPadding = context->audioClient->GetCurrentPadding(&paddingFrames);
        if (FAILED(hrPadding)) {
            logger->debug(env, "GetCurrentPadding failed (%s).", fmtHR(hrPadding));
            return;
        }

        UINT32 framesAvailable = context->bufferFrameCount - paddingFrames;
        if (framesAvailable == 0) return;

        BYTE* pBuffer = nullptr;
        HRESULT hr = context->renderClient->GetBuffer(framesAvailable, &pBuffer);
        if (SUCCEEDED(hr)) {
            context->renderClient->ReleaseBuffer(framesAvailable, AUDCLNT_BUFFERFLAGS_SILENT);
            logger->trace(env, "Flushed WASAPI buffer with %u frames.", framesAvailable);
        } else {
            logger->trace(env, "Flush failed and was skipped (%s).", fmtHR(hr));
        }

        context->pendingFrames = 0;
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nStop
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStop");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return;
        }

        logNotifierMessages(env, logger, context);
        SetEvent(context->events[EVENT_STOP_REQUEST]);

        HRESULT hr = context->audioClient->Stop();
        if (FAILED(hr)) {
            logger->warn(env, "Failed to stop WASAPI output.");
        } else {
            logger->trace(env, "Stopped WASAPI render client.");
        }

        flushBuffer(env, context, logger);
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nFlush
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nFlush");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return;
        }

        flushBuffer(env, context, logger);
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nDrain
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nDrain");
        
        auto context = (OutputContext*)ptr;
        
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return;
        }

        UINT32 padding;
        do {
            DWORD deviceState;
            HRESULT hr = context->outputDevice->GetState(&deviceState);
            if (FAILED(hr) || deviceState != DEVICE_STATE_ACTIVE) {
                logNotifierMessages(env, logger, context);
                logger->warn(env, "Device invalidated during drain, state=%lu (%s).", deviceState, fmtHR(hr));
                env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalidated during drain");
                break;
            }
            
            hr = context->audioClient->GetCurrentPadding(&padding);
            if (FAILED(hr)) {
                logNotifierMessages(env, logger, context);
                if (hr == AUDCLNT_E_DEVICE_INVALIDATED) {
                    logger->error(env, "Device invalidated during drain (%s).", fmtHR(hr));
                    env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalidated during drain");
                    break;
                }
                logger->error(env, "GetCurrentPadding failed during drain (%s).", fmtHR(hr));
                break;
            }
            
            if (padding == 0) break;

            HANDLE handles[2] = { context->events[EVENT_AUDIO_BUFFER_READY], context->events[EVENT_STOP_REQUEST] };
            DWORD waitResult = WaitForMultipleObjects(2, handles, FALSE, 100);
            
            if (waitResult == WAIT_OBJECT_0 + 1) {
                logger->debug(env, "Drain operation interrupted by stop event");
                break;
            }
        } while (true);
        context->pendingFrames = 0;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nWrite
    (JNIEnv* env, jobject obj, jlong ptr, jbyteArray buffer, jint offset, jint length) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nWrite");
        
        auto context = (OutputContext*)ptr;
        
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return -1;
        }

        jbyte* src = env->GetByteArrayElements(buffer, NULL);
        if (!src) {
            logger->error(env, "Failed to get array elements from byte array.");
            return -1;
        }
        UINT32 totalFrames = length / context->bytesPerFrame;
        UINT32 framesWritten = 0;

        while (framesWritten < totalFrames) {
            DWORD deviceState;
            HRESULT hr = context->outputDevice->GetState(&deviceState);
            if (FAILED(hr)) {
                logNotifierMessages(env, logger, context);
                if (hr == AUDCLNT_E_DEVICE_INVALIDATED) {
                    logger->error(env, "Audio device invalidated during write, in GetState (%s).", fmtHR(hr));
                    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                    return -1;
                }
                logger->warn(env, "Audio device GetState failed (%s).", fmtHR(hr));
                env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                return -1;
            }
            
            if (deviceState != DEVICE_STATE_ACTIVE) {
                logNotifierMessages(env, logger, context);
                logger->error(env, "Audio device not active, state=%lu", deviceState);
                env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                env->ThrowNew(ExceptionClassesCache::get(env)->deviceInactiveException, "Audio device not active.");
                return -1;
            }

            UINT32 availableFrames;
            UINT32 padding;
            hr = context->audioClient->GetCurrentPadding(&padding); 
            if (FAILED(hr)) {
                logNotifierMessages(env, logger, context);
                if (hr == AUDCLNT_E_DEVICE_INVALIDATED) {
                    logger->error(env, "Device invalidated during write, in GetCurrentPadding (%s).", fmtHR(hr));
                    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                    env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalidated during write, in GetCurrentPadding.");
                    return -1;
                }
                logger->error(env, "GetCurrentPadding in write failed (%s).", fmtHR(hr));
                env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, "GetCurrentPadding in write failed.");
                return -1;
            }
            availableFrames = context->bufferFrameCount - padding;
            if (availableFrames == 0) {
                DWORD waitResult = WaitForMultipleObjects(2, context->events, FALSE, INFINITE);
                
                if (waitResult == WAIT_OBJECT_0 + 1) {
                    logger->trace(env, "Write operation interrupted by stop event");
                    break;
                }
                else if (waitResult != WAIT_OBJECT_0) {
                    logger->error(env, "WaitForMultipleObjects failed: %lu", GetLastError());
                    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                    return -1;
                }
                continue;
            }

            UINT32 framesToWrite = std::min(availableFrames, totalFrames - framesWritten);
            BYTE* dest;
            hr = context->renderClient->GetBuffer(framesToWrite, &dest);
            if (FAILED(hr)) { 
                logger->error(env, "Failed to get WASAPI output buffer (%s).", fmtHR(hr));
                return -1;
            }

            UINT32 maxFrames = (length - offset) / context->bytesPerFrame;
            framesToWrite = std::min(framesToWrite, maxFrames);
            memcpy(dest, src + offset + framesWritten * context->bytesPerFrame, 
                framesToWrite * context->bytesPerFrame);
            
            hr = context->renderClient->ReleaseBuffer(framesToWrite, 0);
            if (FAILED(hr)) {
                logger->error(env, "Failed to release WASAPI output buffer (%s).", fmtHR(hr));
                return -1;
            }
            framesWritten += framesToWrite;
            context->pendingFrames += framesToWrite;
        }

        env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
        int32_t result = (int32_t)framesWritten * context->bytesPerFrame;
        if (result != length)
            logger->trace(env, "Wrote %d bytes of %d.", result, length);
        return result;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nAvailable
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nAvailable");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return -1;
        }

        logNotifierMessages(env, logger, context);

        UINT32 padding = 0;
        HRESULT hr = context->audioClient->GetCurrentPadding(&padding);
        if (FAILED(hr)) {
            logger->error(env, "Failed to get WASAPI output buffer (%s).", fmtHR(hr));
            env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, "Failed to get WASAPI output buffer.");
            return -1;
        }

        UINT32 availableFrames = context->bufferFrameCount - padding;
        if (availableFrames > INT_MAX) {
            logger->debug(env, "WASAPI output buffer overflow.");
            return -1; // overflow
        }
        return (jint)availableFrames;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nGetBufferSize
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetBufferSize");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return -1;
        }

        return (jint)context->bufferFrameCount;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nGetFramePosition
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetFramePosition");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return -1;
        }

        UINT64 position = 0;
        UINT64 qpc = 0;
        HRESULT hr = context->audioClock->GetPosition(&position, &qpc);

        if (FAILED(hr)) {
            logger->error(env, "Failed to get WASAPI output position (%s).", fmtHR(hr));
            return -1;
        }

        return (jlong)position;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetMicrosecondLatency");
        auto context = (OutputContext*)ptr;
        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return -1;
        }

        REFERENCE_TIME latency = 0;
        HRESULT hr = context->audioClient->GetStreamLatency(&latency);
        if (FAILED(hr)) {
            logger->warn(env, "Failed to get WASAPI output latency (%s).", fmtHR(hr));
            env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, "Failed to get WASAPI output latency.");
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
    Java_org_theko_sound_backends_wasapi_WASAPISharedOutput_nGetCurrentAudioPort
    (JNIEnv* env, jobject obj, jlong ptr) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetCurrentAudioPort");
        auto context = (OutputContext*)ptr;

        if (!context) {
            logger->info(env, "WASAPI output not opened.");
            return nullptr;
        }

        
        ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

        IMMDevice* device = context->outputDevice;
        if (!device) {
            logger->error(env, "Failed to get IMMDevice.");
            env->ThrowNew(exceptionsCache->audioBackendException, "Failed to get IMMDevice.");
            return nullptr;
        }

        jobject jAudioPort = IMMDevice_to_AudioPort(env, device);

        if (!jAudioPort) {
            logger->error(env, "Failed to convert IMMDevice to AudioPort.");
            env->ThrowNew(exceptionsCache->audioBackendException, "Failed to convert IMMDevice to AudioPort.");
            return nullptr;
        }
        
        return jAudioPort;
    }
}
}