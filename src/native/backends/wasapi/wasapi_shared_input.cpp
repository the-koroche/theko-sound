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

#include "org_theko_sound_backends_wasapi_WASAPISharedInput.h"

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

#define EVENT_AUDIO_DATA_READY 0
#define EVENT_STOP_REQUEST 1

namespace theko::sound::backend::wasapi::input {

class InputContext {
private:
    std::mutex logMutex;

public:
    IMMDevice* inputDevice;
    IAudioClient* audioClient;
    IAudioCaptureClient* captureClient;
    IAudioClock* audioClock;
    HANDLE events[2];
    UINT32 bufferFrameCount;
    UINT32 bytesPerFrame;
    WAVEFORMATEX* format;
    IMMDeviceEnumerator* deviceEnumerator;
    IMMNotificationClient* notificationClient;
    std::queue<std::string> notifierLogs;

    InputContext() {
        inputDevice = nullptr;
        audioClient = nullptr;
        captureClient = nullptr;
        audioClock = nullptr;
        events[0] = nullptr;
        events[1] = nullptr;
        bufferFrameCount = 0;
        bytesPerFrame = 0;
        format = nullptr;
        deviceEnumerator = nullptr;
        notificationClient = nullptr;
    }

    InputContext(const InputContext&) = delete;
    InputContext& operator=(const InputContext&) = delete;

    ~InputContext() {
        if (notificationClient) notificationClient->Release();
        if (deviceEnumerator) deviceEnumerator->Release();
        if (format) CoTaskMemFree(format);
        if (captureClient) captureClient->Release();
        if (audioClock) audioClock->Release();
        if (audioClient) audioClient->Release();
        if (inputDevice) inputDevice->Release();
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

class InputDeviceChangeNotifier : public IMMNotificationClient {
private:
    std::atomic<ULONG> refCount;
    InputContext* context;
    HANDLE hStopEvent;

public:
    InputDeviceChangeNotifier(InputContext* ctx)
        : refCount(1), context(ctx), hStopEvent(ctx->events[EVENT_STOP_REQUEST]) {}

    ~InputDeviceChangeNotifier() {}

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
        context->pushLog(msg);

        if (dwNewState == DEVICE_STATE_NOTPRESENT || dwNewState == DEVICE_STATE_UNPLUGGED) {
            interruptCapture();
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDefaultDeviceChanged(EDataFlow flow, ERole role, LPCWSTR pwstrDefaultDeviceId) override {
        std::string msg = "Default device changed: " 
                        + utf16_to_utf8(pwstrDefaultDeviceId)
                        + ", flow: " + (flow == eCapture ? "Capture" : "Render")
                        + ", role: " + std::to_string(role);
        context->pushLog(msg);
        interruptCapture();
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDeviceAdded(LPCWSTR pwstrDeviceId) override {
        std::string msg = "Device added: " 
                        + utf16_to_utf8(pwstrDeviceId);
        context->pushLog(msg);
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnDeviceRemoved(LPCWSTR pwstrDeviceId) override {
        std::string msg = "Device removed: " 
                        + utf16_to_utf8(pwstrDeviceId);
        context->pushLog(msg);
        interruptCapture();
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnPropertyValueChanged(LPCWSTR pwstrDeviceId, const PROPERTYKEY key) override {
        if (key == PKEY_AudioEngine_DeviceFormat) {
            std::string msg = "Device format changed: " 
                            + utf16_to_utf8(pwstrDeviceId);
            context->pushLog(msg);
            interruptCapture();
        } else if (key == PKEY_DeviceInterface_Enabled) {
            std::string msg = "Device interface enabled changed: " 
                            + utf16_to_utf8(pwstrDeviceId);
            context->pushLog(msg);
            interruptCapture();
        }
        return S_OK;
    }

    void interruptCapture() {
        std::string msg = "Interrupting capture due to device change";
        context->pushLog(msg);

        if (hStopEvent) {
            SetEvent(hStopEvent);
        }
    }
};

extern "C" {
    static inline void cleanupAndThrowError(JNIEnv* env, Logger* logger, InputContext* ctx, HRESULT hr, const char* msg) {
        if(ctx) {
            if(ctx->notificationClient) ctx->notificationClient->Release();
            if(ctx->deviceEnumerator) ctx->deviceEnumerator->Release();
            if(ctx->captureClient) ctx->captureClient->Release();
            if(ctx->audioClock) ctx->audioClock->Release();
            if(ctx->audioClient) ctx->audioClient->Release();
            if(ctx->inputDevice) ctx->inputDevice->Release();
            if(ctx->format) CoTaskMemFree(ctx->format);
            if(ctx->events[EVENT_AUDIO_DATA_READY]) CloseHandle(ctx->events[EVENT_AUDIO_DATA_READY]);
            if(ctx->events[EVENT_STOP_REQUEST]) CloseHandle(ctx->events[EVENT_STOP_REQUEST]);
            delete ctx;
        }
        
        const char* hr_msg = fmtHR(hr);
        logger->error(env, "%s (%s).", msg, hr_msg);
        env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, msg);
    }

    static inline void logNotifierMessages(JNIEnv* env, Logger* logger, InputContext* ctx) {
        if (!ctx) return;
        
        std::string log;
        while (ctx->popLog(log)) {
            logger->debug(env, "%s", log.c_str());
        }
    }

    JNIEXPORT jobject JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nOpen");

        if (!jport || !jformat) return nullptr;

        InputContext* context = new InputContext();
        logger->trace(env, "InputContext allocated. Pointer: %s", FORMAT_PTR(context));

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get IMMDevice.");
            return nullptr;
        }
        context->inputDevice = device;
        logger->trace(env, "IMMDevice pointer: %s", FORMAT_PTR(device));

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to get WAVEFORMATEX.");
            return nullptr;
        }
        logger->trace(env, "WAVEFORMATEX (Request): %s. Pointer: %s", WAVEFORMATEX_toText(format), FORMAT_PTR(format));

        context->audioClient = nullptr;
        HRESULT hr = context->inputDevice->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&context->audioClient);
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
            logger->info(env, "Format is not supported, using closest match: %s", WAVEFORMATEX_toText(closestFormat));
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

        context->captureClient = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioCaptureClient), (void**)&context->captureClient);
        if (FAILED(hr) || !context->captureClient) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioCaptureClient.");
            return nullptr;
        }
        logger->trace(env, "IAudioCaptureClient pointer: %s", FORMAT_PTR(context->captureClient));

        context->audioClock = nullptr;
        hr = context->audioClient->GetService(__uuidof(IAudioClock), (void**)&context->audioClock);
        if (FAILED(hr) || !context->audioClock) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get IAudioClock.");
            return nullptr;
        }
        logger->trace(env, "IAudioClock pointer: %s", FORMAT_PTR(context->audioClock));

        context->events[EVENT_AUDIO_DATA_READY] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_AUDIO_DATA_READY]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio callback event.");
            return nullptr;
        }
        context->audioClient->SetEventHandle(context->events[EVENT_AUDIO_DATA_READY]);
        logger->trace(env, "Event handle: %s", FORMAT_PTR(context->events[EVENT_AUDIO_DATA_READY]));

        context->events[EVENT_STOP_REQUEST] = CreateEvent(NULL, TRUE, FALSE, NULL);
        if (!context->events[EVENT_STOP_REQUEST]) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create stop event.");
            return nullptr;
        }
        logger->trace(env, "Stop event handle: %s", FORMAT_PTR(context->events[EVENT_STOP_REQUEST]));

        hr = context->audioClient->GetBufferSize(&context->bufferFrameCount);
        if (FAILED(hr)) {
            cleanupAndThrowError(env, logger, context, hr, "Failed to get buffer size.");
            return nullptr;
        }
        context->bytesPerFrame = format->nBlockAlign;
        
        logger->debug(env, "Actual buffer size: %d frames", context->bufferFrameCount);

        hr = CoCreateInstance(__uuidof(MMDeviceEnumerator), NULL,
            CLSCTX_ALL, __uuidof(IMMDeviceEnumerator),
            (void**)&context->deviceEnumerator);
    
        if (SUCCEEDED(hr)) {
            InputDeviceChangeNotifier* notifier = new InputDeviceChangeNotifier(context);
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

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);
        env->SetLongField(obj, inputCache->inputContextPtr, (jlong)context);

        jobject resultAudioFormat = WAVEFORMATEX_to_AudioFormat(env, context->format);
        if (!resultAudioFormat) {
            cleanupAndThrowError(env, logger, context, E_FAIL, "Failed to create audio format.");
            return nullptr;
        }

        logger->debug(env, "Opened WASAPI input. ContextPtr: %s", FORMAT_PTR(context));

        return resultAudioFormat;
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nClose
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nClose");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->debug(env, "WASAPI input already closed.");
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

        if (context->captureClient) {
            ULONG refCount = context->captureClient->Release();
            if (refCount > 0) {
                logger->warn(env, "IAudioCaptureClient still has %lu references.", refCount);
            }
            context->captureClient = nullptr;
        }

        if (context->audioClient) {
            ULONG refCount = context->audioClient->Release();
            if (refCount > 0) {
                logger->warn(env, "IAudioClient still has %lu references.", refCount);
            }
            context->audioClient = nullptr;
        }

        if (context->inputDevice) {
            ULONG refCount = context->inputDevice->Release();
            if (refCount > 0) {
                logger->warn(env, "IMMDevice still has %lu references.", refCount);
            }
            context->inputDevice = nullptr;
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

        if (context->events[EVENT_AUDIO_DATA_READY]) {
            CloseHandle(context->events[EVENT_AUDIO_DATA_READY]);
            context->events[EVENT_AUDIO_DATA_READY] = nullptr;
        }

        if (context->events[EVENT_STOP_REQUEST]) {
            CloseHandle(context->events[EVENT_STOP_REQUEST]);
            context->events[EVENT_STOP_REQUEST] = nullptr;
        }

        delete context;
        env->SetLongField(obj, inputCache->inputContextPtr, 0);
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nStart
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nStart");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);
        
        if (context) {
            logNotifierMessages(env, logger, context);

            HRESULT hr = context->audioClient->Start();
            if (FAILED(hr)) {
                logger->error(env, "Failed to start WASAPI input.");
            } else {
                logger->debug(env, "Started WASAPI input.");
            }
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nStop
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nStop");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);
        
        if (context) {
            logNotifierMessages(env, logger, context);
            SetEvent(context->events[EVENT_STOP_REQUEST]);
            HRESULT hr = context->audioClient->Stop();
            if (FAILED(hr)) {
                logger->error(env, "Failed to stop WASAPI input.");
            } else {
                logger->debug(env, "Stopped WASAPI input.");
            }
        } else {
            logger->trace(env, "WASAPI input not opened.");
        }
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nFlush
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nFlush");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);
        
        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return;
        }

        logNotifierMessages(env, logger, context);

        UINT32 packetLength = 0;
        BYTE* pData = nullptr;
        DWORD flags = 0;
        UINT64 devicePosition = 0;
        UINT64 qpcPosition = 0;

        HRESULT hr = context->captureClient->GetBuffer(
            &pData,
            &packetLength,
            &flags,
            &devicePosition,
            &qpcPosition
        );

        if (SUCCEEDED(hr) && pData) {
            hr = context->captureClient->ReleaseBuffer(packetLength);
            if (FAILED(hr)) {
                logger->error(env, "Failed to release buffer during flush.");
            }
        }

        logger->debug(env, "Flushed WASAPI input buffer.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nDrain
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported for input.");
    }

JNIEXPORT jint JNICALL
Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nRead
(JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {

    Logger* logger = LoggerManager::getManager()
                        ->getLogger(env, "NATIVE: WASAPISharedInput.nRead");

    WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

    InputContext* context = (InputContext*)env->GetLongField(
        obj, inputCache->inputContextPtr
    );

    if (!context) {
        logger->error(env, "WASAPI input not opened.");
        return -1;
    }

    jboolean isCopy = JNI_FALSE;
    jbyte* dest = env->GetByteArrayElements(buffer, &isCopy);
    if (!dest) {
        logger->error(env, "Failed to get array elements");
        return -1;
    }

    UINT32 bytesPerFrame = context->bytesPerFrame;
    UINT32 remainingBytes = length;
    jint   destOffset     = offset;
    UINT32 totalBytesRead = 0;

    constexpr DWORD WAIT_TIMEOUT_MS = 40;

    while (remainingBytes > 0) {

        DWORD deviceState = 0;
        HRESULT hr = context->inputDevice->GetState(&deviceState);
        if (FAILED(hr) || deviceState != DEVICE_STATE_ACTIVE) {
            logNotifierMessages(env, logger, context);
            logger->error(env, "Audio device not active: state=%lu", deviceState);

            env->ReleaseByteArrayElements(buffer, dest, JNI_ABORT);
            env->ThrowNew(
                ExceptionClassesCache::get(env)->deviceInactiveException,
                "Audio device not active."
            );
            return -1;
        }

        BYTE* pData = nullptr;
        UINT32 packetLength = 0; // frames
        DWORD flags = 0;
        UINT64 devicePosition = 0;
        UINT64 qpcPosition = 0;

        hr = context->captureClient->GetBuffer(
            &pData,
            &packetLength,
            &flags,
            &devicePosition,
            &qpcPosition
        );

        if (hr == AUDCLNT_S_BUFFER_EMPTY) {

            HANDLE handles[2] = {
                context->events[EVENT_AUDIO_DATA_READY],
                context->events[EVENT_STOP_REQUEST]
            };

            DWORD waitResult =
                WaitForMultipleObjects(2, handles, FALSE, WAIT_TIMEOUT_MS);

            if (waitResult == WAIT_OBJECT_0 + 1) {
                logger->trace(env, "Read interrupted by stop event");
                break;
            }

            continue;
        }

        if (FAILED(hr)) {
            logNotifierMessages(env, logger, context);

            if (hr == AUDCLNT_E_DEVICE_INVALIDATED) {
                logger->error(env, "Device invalidated during read");

                env->ReleaseByteArrayElements(buffer, dest, JNI_ABORT);
                env->ThrowNew(
                    ExceptionClassesCache::get(env)->deviceInvalidatedException,
                    "Device invalidated during read."
                );
                return -1;
            }

            const std::string msg = formatHRMessage(hr);
            logger->error(env, "GetBuffer failed: %s", msg.c_str());

            env->ReleaseByteArrayElements(buffer, dest, JNI_ABORT);
            return -1;
        }

        const UINT32 packetBytes = packetLength * bytesPerFrame;

        const UINT32 bytesToCopy =
            (remainingBytes < packetBytes) ? remainingBytes : packetBytes;

        if (bytesToCopy > 0) {
            if (flags & AUDCLNT_BUFFERFLAGS_SILENT) {
                memset(dest + destOffset, 0, bytesToCopy);
            } else {
                memcpy(dest + destOffset, pData, bytesToCopy);
            }

            destOffset     += bytesToCopy;
            remainingBytes -= bytesToCopy;
            totalBytesRead += bytesToCopy;
        }

        hr = context->captureClient->ReleaseBuffer(packetLength);
        if (FAILED(hr)) {
            const std::string msg = formatHRMessage(hr);
            logger->error(env, "Failed to release buffer: %s", msg.c_str());
            break;
        }
    }

    env->ReleaseByteArrayElements(buffer, dest, 0);
    return totalBytesRead;
}


    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nAvailable
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nAvailable");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return -1;
        }

        logNotifierMessages(env, logger, context);

        UINT32 packetLength = 0;
        BYTE* pData = nullptr;
        DWORD flags = 0;
        UINT64 devicePosition = 0;
        UINT64 qpcPosition = 0;

        HRESULT hr = context->captureClient->GetBuffer(
            &pData,
            &packetLength,
            &flags,
            &devicePosition,
            &qpcPosition
        );

        if (hr == AUDCLNT_S_BUFFER_EMPTY) {
            return 0;
        } else if (FAILED(hr)) {
            const char* hr_msg = fmtHR(hr);
            logger->error(env, "Failed to get available data: %s", hr_msg);
            return -1;
        }

        context->captureClient->ReleaseBuffer(packetLength);

        return packetLength * context->bytesPerFrame;
    }

    JNIEXPORT jint JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetBufferSize
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nGetBufferSize");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return -1;
        }

        return (jint)context->bufferFrameCount;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetFramePosition
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nGetFramePosition");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return -1;
        }

        UINT64 position = 0;
        UINT64 qpc = 0;
        HRESULT hr = context->audioClock->GetPosition(&position, &qpc);

        if (FAILED(hr)) {
            const char* hr_msg = fmtHR(hr);
            logger->error(env, "Failed to get WASAPI input position (%s).", hr_msg);
            return -1;
        }

        return (jlong)position;
    }

    JNIEXPORT jlong JNICALL Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nGetMicrosecondLatency");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return -1;
        }

        REFERENCE_TIME latency = 0;
        HRESULT hr = context->audioClient->GetStreamLatency(&latency);
        if (FAILED(hr)) {
            const char* hr_msg = fmtHR(hr);
            const char* fmsg = format("Failed to get WASAPI input latency (%s).", hr_msg).c_str();
            logger->error(env, fmsg);
            env->ThrowNew(ExceptionClassesCache::get(env)->audioBackendException, fmsg);
            return -1;
        } else if (SUCCEEDED(hr) && latency > 0) {
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
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetCurrentAudioPort
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedInput.nGetCurrentAudioPort");

        WASAPIInputCache* inputCache = WASAPIInputCache::get(env);
        ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

        InputContext* context = (InputContext*)env->GetLongField(obj, inputCache->inputContextPtr);

        if (!context) {
            logger->error(env, "WASAPI input not opened.");
            return nullptr;
        }

        IMMDevice* device = context->inputDevice;

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
    JNIEXPORT jobject JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nOpen
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jint bufferSize) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nClose
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nStart
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nStop
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nFlush
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nDrain
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nRead
    (JNIEnv* env, jobject obj, jbyteArray buffer, jint offset, jint length) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nAvailable
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jint JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetBufferSize
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetFramePosition
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jlong JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetMicrosecondLatency
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return -1;
    }

    JNIEXPORT jobject JNICALL
    Java_org_theko_sound_backends_wasapi_WASAPISharedInput_nGetCurrentAudioPort
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }
}
#endif // end of !_WIN32
}