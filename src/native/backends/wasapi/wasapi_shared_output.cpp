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

#include "DefaultClassesCache.hpp"
#include "WASAPIClassesCache.hpp"

#include "logger.hpp"
#include "logger_manager.hpp"

#include "org_theko_sound_backend_wasapi_WASAPISharedOutput.h"

#ifdef _WIN32
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

EXTERN_C const IID IID_IUnknown = {0x00000000, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}};

#define EVENT_AUDIO_BUFFER_READY 0
#define EVENT_STOP_REQUEST 1

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
        if (notificationClient) notificationClient->Release();
        if (deviceEnumerator) deviceEnumerator->Release();
        if (format) CoTaskMemFree(format);
        if (renderClient) renderClient->Release();
        if (audioClock) audioClock->Release();
        if (audioClient) audioClient->Release();
        if (outputDevice) outputDevice->Release();
        if (events[0]) CloseHandle(events[0]);
        if (events[1]) CloseHandle(events[1]);
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

class DeviceChangeNotifier : public IMMNotificationClient {
private:
    std::atomic<ULONG> refCount;
    OutputContext* context;
    HANDLE hStopEvent;

public:
    DeviceChangeNotifier(OutputContext* ctx)
        : refCount(1), context(ctx), hStopEvent(ctx->events[EVENT_STOP_REQUEST]) {}

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
    inline void cleanupAndThrowError(JNIEnv* env, Logger* logger, OutputContext* ctx, HRESULT hr, const char* msg) {
        if(ctx) {
            if(ctx->notificationClient) ctx->notificationClient->Release();
            if(ctx->deviceEnumerator) ctx->deviceEnumerator->Release();
            if(ctx->renderClient) ctx->renderClient->Release();
            if(ctx->audioClock) ctx->audioClock->Release();
            if(ctx->audioClient) ctx->audioClient->Release();
            if(ctx->outputDevice) ctx->outputDevice->Release();
            if(ctx->format) CoTaskMemFree(ctx->format);
            if(ctx->events[EVENT_AUDIO_BUFFER_READY]) CloseHandle(ctx->events[EVENT_AUDIO_BUFFER_READY]);
            if(ctx->events[EVENT_STOP_REQUEST]) CloseHandle(ctx->events[EVENT_STOP_REQUEST]);
            delete ctx;
        }
        
        const char* hr_msg = formatHRMessage(hr).c_str();
        logger->error(env, "%s (%s)", msg, hr_msg);
        env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, msg);
    }

    inline void logNotifierMessages(JNIEnv* env, Logger* logger, OutputContext* ctx) {
        if (!ctx) return;
        
        std::string log;
        while (ctx->popLog(log)) {
            logger->debug(env, "%s", log.c_str());
        }
    }

    JNIEXPORT jobject JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize /* in bytes */) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nOpen");

        if (!jport || !jformat) return nullptr;

        OutputContext* context = new OutputContext();
        logger->trace(env, "OutputContext allocated. Pointer: %s", FORMAT_PTR(context));

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get IMMDevice.");
            return nullptr;
        }
        context->outputDevice = device;
        logger->trace(env, "IMMDevice pointer: %s", FORMAT_PTR(device));

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get WAVEFORMATEX.");
            return nullptr;
        }
        logger->trace(env, "WAVEFORMATEX (Request): %s. Pointer: %s", WAVEFORMATEX_toText(format), FORMAT_PTR(format));

        context->audioClient = nullptr;
        HRESULT hr = context->outputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
        if (FAILED(hr) || !context->audioClient) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioClient.");
            return nullptr;
        }
        logger->trace(env, "IAudioClient pointer: %s", FORMAT_PTR(context->audioClient));

        WAVEFORMATEX* closestFormat = nullptr;
        hr = context->audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closestFormat);
        if (FAILED(hr)) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to check format support.");
            return nullptr;
        }

        if (hr == S_OK) {
            logger->trace(env, "Format is supported.");
        } else if (hr == S_FALSE && closestFormat) {
            logger->info(env, "Format is not supported, using closest match: %s" , WAVEFORMATEX_toText(closestFormat));
            logger->trace(env, "Closest format pointer: %s", FORMAT_PTR(closestFormat));
            
            CoTaskMemFree(format);
            format = (WAVEFORMATEX*)closestFormat;
        } else {
            CoTaskMemFree(closestFormat);
            CoTaskMemFree(format);
            cleanupAndThrowError(env, logger, context, hr, "Failed to check format support.");
            return nullptr;
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
            cleanupAndThrowError(env, logger, context, hr, "Failed to initialize IAudioClient.");
            return nullptr;
        }
        logger->trace(env, "IAudioClient initialized.");

        context->renderClient = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioRenderClient), (void**)&context->renderClient);
        if (FAILED(hr) || !context->renderClient) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioRenderClient.");
            return nullptr;
        }
        logger->trace(env, "IAudioRenderClient pointer: %s", FORMAT_PTR(context->renderClient));

        context->audioClock = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioClock), (void**)&context->audioClock);
        if (FAILED(hr) || !context->audioClock) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioClock.");
            return nullptr;
        }
        logger->trace(env, "IAudioClock pointer: %s", FORMAT_PTR(context->audioClock));

        context->events[EVENT_AUDIO_BUFFER_READY] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_AUDIO_BUFFER_READY]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio callback event.");
            return nullptr;
        }
        context->audioClient->SetEventHandle(context->events[EVENT_AUDIO_BUFFER_READY]);
        logger->trace(env, "Event handle: %s", FORMAT_PTR(context->events[EVENT_AUDIO_BUFFER_READY]));

        context->events[EVENT_STOP_REQUEST] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_STOP_REQUEST]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create stop event.");
            return nullptr;
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
            DeviceChangeNotifier* notifier = new DeviceChangeNotifier(context);
            hr = context->deviceEnumerator->RegisterEndpointNotificationCallback(notifier);
            
            if (SUCCEEDED(hr)) {
                context->notificationClient = notifier;
                notifier->AddRef();
                logger->debug(env, "Device change notification registered");
            } else {
                notifier->Release();
                logger->warn(env, "Failed to register device notifications");
            }
        } else {
            logger->warn(env, "Failed to create device enumerator");
        }

        env->SetLongField(obj, WASAPIOutputCache::get(env)->outputContextPtr, (jlong)context);
        logger->debug(env, "Opened WASAPI output. ContextPtr: %s", FORMAT_PTR(context));

        jobject jAudioFormat = JNIUtil_CreateGlobal(env, WAVEFORMATEX_to_AudioFormat(env, context->format));
        if (!jAudioFormat) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio format.");
            return nullptr;
        }
        return jAudioFormat;
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nClose
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nClose");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

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
        env->SetLongField(obj, outputCache->outputContextPtr, 0);
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nStart
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStart");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);
        
        if (context) {
            logNotifierMessages(env, logger, context);

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
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nStop");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);
        
        if (context) {
            logNotifierMessages(env, logger, context);
            SetEvent(context->events[EVENT_STOP_REQUEST]);
            HRESULT hr = context->audioClient->Stop();
            if (FAILED(hr)) {
                logger->error(env, "Failed to stop WASAPI output.");
            } else {
                logger->debug(env, "Stopped WASAPI render client.");
            }
            BYTE* pBuffer;
            hr = context->renderClient->GetBuffer(context->bufferFrameCount, &pBuffer);
            if (SUCCEEDED(hr)) {
                context->renderClient->ReleaseBuffer(
                    context->bufferFrameCount, 
                    AUDCLNT_BUFFERFLAGS_SILENT
                );
            } else {
                logger->error(env, "Failed to get WASAPI buffer.");
            }
            context->pendingFrames = 0;
            logger->debug(env, "Flushed WASAPI buffer.");
        } else {
            logger->trace(env, "WASAPI output not opened.");
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nFlush
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nDrain
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nDrain");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);
        
        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return;
        }

        UINT32 padding;
        do {
            DWORD deviceState;
            HRESULT hr = context->outputDevice->GetState(&deviceState);
            if (FAILED(hr) || deviceState != DEVICE_STATE_ACTIVE) {
                logNotifierMessages(env, logger, context);
                logger->error(env, "Device invalid during drain: hr=%lx, state=%lu", hr, deviceState);
                env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalid during drain");
                break;
            }
            
            hr = context->audioClient->GetCurrentPadding(&padding);
            if (FAILED(hr)) {
                logNotifierMessages(env, logger, context);
                if (hr == AUDCLNT_E_DEVICE_INVALIDATED) {
                    logger->error(env, "Device invalidated during drain");
                    env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalidated during drain");
                    break;
                }
                logger->error(env, "GetCurrentPadding failed during drain: hr=%lx", hr);
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
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nWrite
    (JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nWrite");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);
        
        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        jbyte* src = env->GetByteArrayElements(buffer, NULL);
        if (!src) {
            logger->error(env, "Failed to get array elements");
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
                    logger->error(env, "Audio device invalidated (format changed?)");
                    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                    return -1;
                }
                env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                return -1;
            }
            
            if (deviceState != DEVICE_STATE_ACTIVE) {
                logNotifierMessages(env, logger, context);
                logger->error(env, "Audio device not active: state=%lu", deviceState);
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
                    logger->error(env, "Device invalidated during GetCurrentPadding");
                    env->ReleaseByteArrayElements(buffer, src, JNI_ABORT);
                    env->ThrowNew(ExceptionClassesCache::get(env)->deviceInvalidatedException, "Device invalidated during write, in GetCurrentPadding.");
                    return -1;
                }
                const char* hr_msg = formatHRMessage(hr).c_str();
                logger->error(env, "GetCurrentPadding failed: %s", hr_msg);
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
                const char* hr_msg = formatHRMessage(hr).c_str();
                logger->error(env, "Failed to get WASAPI output buffer. (%s)", hr_msg);
                return -1;
            }

            UINT32 maxFrames = (length - offset) / context->bytesPerFrame;
            framesToWrite = std::min(framesToWrite, maxFrames);
            memcpy(dest, src + offset + framesWritten * context->bytesPerFrame, 
                framesToWrite * context->bytesPerFrame);
            
            hr = context->renderClient->ReleaseBuffer(framesToWrite, 0);
            if (FAILED(hr)) {
                const char* hr_msg = formatHRMessage(hr).c_str();
                logger->error(env, "Failed to release WASAPI output buffer. (%s)", hr_msg);
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
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nAvailable");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        logNotifierMessages(env, logger, context);

        UINT32 padding = 0;
        HRESULT hr = context->audioClient->GetCurrentPadding(&padding);
        if (FAILED(hr)) {
            const char* hr_msg = formatHRMessage(hr).c_str();
            const char* fmsg = format("Failed to get WASAPI output buffer. (%s)", hr_msg).c_str();
            logger->error(env, fmsg);
            env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, fmsg);
            return -1;
        }

        UINT32 availableFrames = context->bufferFrameCount - padding;
        if (availableFrames > INT_MAX) {
            return -1; // overflow
        }
        return (jint)availableFrames;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetBufferSize
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetBufferSize");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        return (jint)context->bufferFrameCount;
    }


    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetFramePosition
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetFramePosition");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        UINT64 position = 0;
        UINT64 qpc = 0;
        HRESULT hr = context->audioClock->GetPosition(&position, &qpc);

        if (FAILED(hr)) {
            const char* hr_msg = formatHRMessage(hr).c_str();
            logger->error(env, "Failed to get WASAPI output position. (%s)", hr_msg);
            return -1;
        }

        return (jlong)position;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetMicrosecondLatency");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return -1;
        }

        REFERENCE_TIME latency = 0;
        HRESULT hr = context->audioClient->GetStreamLatency(&latency);
        if (FAILED(hr)) {
            const char* hr_msg = formatHRMessage(hr).c_str();
            const char* fmsg = format("Failed to get WASAPI output latency. (%s)", hr_msg).c_str();
            logger->error(env, fmsg);
            env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, fmsg);
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
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedOutput.nGetCurrentAudioPort");

        WASAPIOutputCache* outputCache = WASAPIOutputCache::get(env);
        ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

        OutputContext* context = (OutputContext*)env->GetLongField(obj, outputCache->outputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI output not opened.");
            return nullptr;
        }

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
#else // end of _WIN32
extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize /* in bytes */) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nClose
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nStart
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nStop
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nFlush
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nDrain
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nWrite
    (JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nAvailable
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetBufferSize
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetFramePosition
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jobject JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedOutput_nGetCurrentAudioPort
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }
}
#endif // end of !_WIN32