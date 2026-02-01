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

#include "org_theko_sound_backend_wasapi_WASAPISharedBackend.h"

#ifdef _WIN32
#include <windows.h>
#include <initguid.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>

EXTERN_C const IID IID_IUnknown = {0x00000000, 0x0000, 0x0000, {0xC0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46}};

#include "wasapi_utils.hpp"
#include "wasapi_bridge.hpp"

namespace org_theko_sound_backend_wasapi {

typedef struct {
    IMMDeviceEnumerator* deviceEnumerator;
} BackendContext;

extern "C" {
    JNIEXPORT jlong JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nInit
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nInit");

        ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

        HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
        if (FAILED(hr)) {
            logger->error(env, "Failed to initialize COM in multithreaded mode (%s).", fmtHR(hr));
            env->ThrowNew(exceptionsCache->audioBackendException, "Failed to initialize COM.");
            return 0;
        }

        auto* ctx = new BackendContext{ nullptr };
        ctx->deviceEnumerator = nullptr;

        hr = CoCreateInstance(
            CLSID_MMDeviceEnumerator, NULL,
            CLSCTX_ALL, IID_IMMDeviceEnumerator,
            (void**)&ctx->deviceEnumerator
        );
        if (FAILED(hr)) {
            logger->error(env, "Failed to create IMMDeviceEnumerator (%s).", fmtHR(hr));
            env->ThrowNew(exceptionsCache->audioBackendException, "Failed to create IMMDeviceEnumerator.");
            CoUninitialize();
            return 0;
        }

        logger->debug(env, "Initialized WASAPI backend. ContextPtr: %s", FORMAT_PTR(ctx));

        return (jlong)ctx;
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nShutdown
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nShutdown");

        auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPIBackendCache::get(env)->backendContextPtr);
        IMMDeviceEnumerator* deviceEnumerator = ctx->deviceEnumerator;

        if (deviceEnumerator) {
            logger->trace(env, "Releasing IMMDeviceEnumerator.");
            deviceEnumerator->Release();
            deviceEnumerator = nullptr;
        }
        CoUninitialize();

        logger->debug(env, "Shutdown WASAPI backend.");
    }

    JNIEXPORT jobjectArray JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nGetAllPorts
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nGetAllPorts");

        auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPIBackendCache::get(env)->backendContextPtr);
        IMMDeviceEnumerator* deviceEnumerator = ctx->deviceEnumerator;

        if (!deviceEnumerator) {
            logger->warn(env, "No device enumerator found in context.");
            return nullptr;
        }

        IMMDeviceCollection* renderDevices = getDevicesList(deviceEnumerator, eRender);
        if (!renderDevices) return nullptr;

        IMMDeviceCollection* captureDevices = getDevicesList(deviceEnumerator, eCapture);
        if (!captureDevices) return nullptr;

        UINT renderCount = 0, captureCount = 0;
        renderDevices->GetCount(&renderCount);
        captureDevices->GetCount(&captureCount);
        UINT totalCount = renderCount + captureCount;

        logger->trace(env, "Found %d render ports and %d capture ports. Total %d ports.", renderCount, captureCount, totalCount);

        jobjectArray result = env->NewObjectArray(totalCount, AudioPortCache::get(env)->clazz, nullptr);
        if (!result) {
            logger->warn(env, "Failed to create AudioPort array.");
            return nullptr;
        }

        UINT index = 0;
        for (UINT i = 0; i < renderCount; i++) {
            IMMDevice* pDevice = nullptr;
            renderDevices->Item(i, &pDevice);
            jobject audioPortObj = IMMDevice_to_AudioPort(env, pDevice);
            env->SetObjectArrayElement(result, index++, audioPortObj);
            logger->trace(env, "Render port #%d: %ls", i, pDevice);
            pDevice->Release();
        }
        for (UINT i = 0; i < captureCount; i++) {
            IMMDevice* pDevice = nullptr;
            captureDevices->Item(i, &pDevice);
            jobject audioPortObj = IMMDevice_to_AudioPort(env, pDevice);
            env->SetObjectArrayElement(result, index++, audioPortObj);
            logger->trace(env, "Capture port #%d: %ls", i, pDevice);
            pDevice->Release();
        }

        renderDevices->Release();
        captureDevices->Release();

        return result;
    }

    JNIEXPORT jobject JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nGetDefaultPort
    (JNIEnv* env, jobject obj, jobject flowObj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nGetDefaultPort");
        
        if (!flowObj) return nullptr;

        auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPIBackendCache::get(env)->backendContextPtr);
        IMMDeviceEnumerator* deviceEnumerator = ctx->deviceEnumerator;

        if (!deviceEnumerator) {
            logger->warn(env, "No device enumerator found in context.");
            return nullptr;
        }

        AudioFlowCache* flowCache = AudioFlowCache::get(env);

        EDataFlow flow = eRender;
        if (env->IsSameObject(flowObj, flowCache->outObj)) {
            flow = eRender;
        } else if (env->IsSameObject(flowObj, flowCache->inObj)) {
            flow = eCapture;
        } else {
            return nullptr;
        }
        logger->trace(env, "Flow: %d", flow == eRender ? "Render" : "Capture");

        IMMDevice* pDevice = nullptr;
        HRESULT hr = deviceEnumerator->GetDefaultAudioEndpoint(flow, eConsole, &pDevice);
        if (FAILED(hr)) {
            logger->warn(env, "Failed to get default audio endpoint for flow %d (%s).", flow == eRender ? "Render" : "Capture", fmtHR(hr));
            return nullptr;
        }

        if (!pDevice) {
            logger->info(env, "No default audio endpoint.");
            return nullptr;
        }

        logger->trace(env, "Default audio endpoint pointer: %s", FORMAT_PTR(pDevice));

        wchar_t* idName = nullptr;
        hr = pDevice->GetId(&idName);
        if (FAILED(hr)) {
            logger->debug(env, "Failed to get default audio endpoint ID for flow %d. %s", flow == eRender ? "Render" : "Capture", fmtHR(hr));
        } else {
            const char* utf8_idName = utf16_to_utf8(idName).c_str();
            logger->trace(env, "Default audio endpoint: %s. Flow: %s", utf8_idName, flow == eRender ? "Render" : "Capture");
        }

        if (idName) {
            CoTaskMemFree(idName);
        }

        jobject jAudioPort = IMMDevice_to_AudioPort(env, pDevice);
        pDevice->Release();
        
        return jAudioPort;
    }

    JNIEXPORT jboolean JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nIsFormatSupported
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jobject atomicClosestFormat) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nIsFormatSupported");

        if (!jport || !jformat) {
            logger->info(env, "AudioPort or AudioFormat is null.");
            return JNI_FALSE;
        }

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            logger->warn(env, "Failed to get IMMDevice.");
            return JNI_FALSE;
        }

        logger->trace(env, "IMMDevice pointer: %s", FORMAT_PTR(device));

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            logger->warn(env, "Failed to get WAVEFORMATEX.");
            return JNI_FALSE;
        }

        logger->trace(env, "WAVEFORMATEX: %s. Pointer: %s", (format ? WAVEFORMATEX_toText(format) : "NULL"), FORMAT_PTR(format));

        IAudioClient* audioClient = nullptr;
        HRESULT hr = device->Activate(__uuidof(IAudioClient), CLSCTX_ALL, NULL, (void**)&audioClient);
        device->Release();
        if (FAILED(hr) || audioClient == nullptr) {
            CoTaskMemFree(format);
            logger->warn(env, "Failed to get or activate IAudioClient (%s).", fmtHR(hr));
            return JNI_FALSE;
        }

        logger->trace(env, "IAudioClient pointer: %s", FORMAT_PTR(audioClient));

        WAVEFORMATEX* closest = nullptr;
        hr = audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, &closest);
        audioClient->Release();
        CoTaskMemFree(format);

        if (hr == S_OK) {
            logger->trace(env, "Format is supported.");
            return JNI_TRUE;
        } else if (hr == S_FALSE) {
            logger->trace(env, "Format is not supported.");
            if (closest) {
                logger->trace(env, "Closest format: %s. Pointer: %s", (closest ? WAVEFORMATEX_toText(closest) : "NULL"), FORMAT_PTR(closest));
                if (atomicClosestFormat) {
                    logger->trace(env, "AtomicClosestFormat pointer: %s", FORMAT_PTR(atomicClosestFormat));
                    jobject jAudioFormat = WAVEFORMATEX_to_AudioFormat(env, closest);
                    env->CallVoidMethod(atomicClosestFormat, AtomicReferenceCache::get(env)->setMethod, jAudioFormat);
                }
                CoTaskMemFree(closest);
            }
            return JNI_FALSE;
        } else {
            if (closest) CoTaskMemFree(closest);
            logger->trace(env, "Format is not supported.");
            return JNI_FALSE;
        }
    }
}
#else // end of _WIN32
extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nInit
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nShutdown
    (JNIEnv* env, jobject obj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
    }

    JNIEXPORT jobjectArray JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nGetAllPorts
    (JNIEnv* env, jobject obj, jobject flowObj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }

    JNIEXPORT jobject JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nGetDefaultPort
    (JNIEnv* env, jobject obj, jobject flowObj) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return nullptr;
    }

    JNIEXPORT jboolean JNICALL
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nIsFormatSupported
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat, jobject atomicClosestFormat) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return JNI_FALSE;
    }
}
#endif // end of !_WIN32
}