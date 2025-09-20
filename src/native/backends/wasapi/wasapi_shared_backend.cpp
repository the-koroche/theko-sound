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

#include "wasapi_utils.hpp"
#include "wasapi_bridge.hpp"

typedef struct {
    IMMDeviceEnumerator* deviceEnumerator;
} BackendContext;

extern "C" {
    JNIEXPORT void JNICALL 
    Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_nInit
    (JNIEnv* env, jobject obj) {
        Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPISharedBackend.nInit");

        ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

        HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            char* msg = format("Failed to initialize COM. (%s)", hr_msg);
            free(hr_msg);
            logger->error(env, msg);
            env->ThrowNew(exceptionsCache->audioBackendException, msg);
            free(msg);
            return;
        }

        auto* ctx = new BackendContext{ nullptr };
        ctx->deviceEnumerator = nullptr;

        hr = CoCreateInstance(
            CLSID_MMDeviceEnumerator, NULL,
            CLSCTX_ALL, IID_IMMDeviceEnumerator,
            (void**)&ctx->deviceEnumerator
        );
        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            char* msg = format("Failed to create IMMDeviceEnumerator. (%s)", hr_msg);
            free(hr_msg);
            logger->error(env, msg);
            env->ThrowNew(exceptionsCache->audioBackendException, msg);
            free(msg);
            CoUninitialize();
            return;
        }
        env->SetLongField(obj, WASAPIBackendCache::get(env)->backendContextPtr, (jlong)ctx);

        logger->debug(env, "Initialized WASAPI backend. ContextPtr: %p", ctx);
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
            logger->warn(env, "No device enumerator.");
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

        logger->debug(env, "Found %d render ports and %d capture ports. Total %d ports.", renderCount, captureCount, totalCount);

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
            logger->warn(env, "No device enumerator.");
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
            logger->warn(env, "Failed to get default audio endpoint.");
            return nullptr;
        }

        if (!pDevice) {
            logger->info(env, "No default audio endpoint.");
            return nullptr;
        }

        logger->trace(env, "Default audio endpoint ptr: %p", pDevice);

        wchar_t* idName = nullptr;
        hr = pDevice->GetId(&idName);
        if (FAILED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            logger->debug(env, "Failed to get default audio endpoint ID. %s", hr_msg);
            free(hr_msg);
            logger->debug(env, "Default audio endpoint flow: %s", flow == eRender ? "Render" : "Capture");
        } else {
            char* utf8_idName = utf16_to_utf8(idName);
            logger->debug(env, "Default audio endpoint: %s. Flow: %s", utf8_idName, flow == eRender ? "Render" : "Capture");
            free(utf8_idName);
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
            logger->warn(env, "AudioPort or AudioFormat is null.");
            return JNI_FALSE;
        }

        IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
        if (!device) {
            logger->warn(env, "Failed to get IMMDevice.");
            return JNI_FALSE;
        }

        logger->trace(env, "IMMDevice ptr: %p", device);

        WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
        if (!format) {
            logger->warn(env, "Failed to get WAVEFORMATEX.");
            return JNI_FALSE;
        }

        logger->trace(env, "WAVEFORMATEX ptr: %p", format);

        IAudioClient* audioClient = nullptr;
        HRESULT hr = device->Activate(__uuidof(IAudioClient), CLSCTX_ALL, NULL, (void**)&audioClient);
        device->Release();
        if (FAILED(hr) || audioClient == nullptr) {
            CoTaskMemFree(format);
            char* hr_msg = format_hr_msg(hr);
            logger->warn(env, "Failed to get or activate IAudioClient. (%s)", hr_msg);
            free(hr_msg);
            return JNI_FALSE;
        }

        logger->trace(env, "IAudioClient ptr: %p", audioClient);

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
                logger->trace(env, "Closest format ptr: %p", closest);
                if (atomicClosestFormat) {
                    logger->trace(env, "AtomicClosestFormat ptr: %p", atomicClosestFormat);
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
    (JNIEnv* env, jobject obj) {
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
    (JNIEnv* env, jobject obj, jobject jport, jobject jformat) {
        env->ThrowNew(ExceptionClassesCache::get(env)->unsupportedOperationException, "Not supported on this platform.");
        return JNI_FALSE;
    }
}
#endif // end of !_WIN32