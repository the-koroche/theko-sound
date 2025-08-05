#include <jni.h>

#include "helper_utilities.hpp"
#include "classes_cache.hpp"

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

#include "logger.hpp"
#include "logger_manager.hpp"

typedef struct {
    IMMDeviceEnumerator* deviceEnumerator;
} BackendContext;

JNIEXPORT void JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_initialize0
        (JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBackend.initialize0");

    HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    if (FAILED(hr)) {
        char* msg = format("Failed to initialize COM. (HRESULT: 0x%08x)", hr);
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
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
        char* msg = format("Failed to create IMMDeviceEnumerator. (HRESULT: 0x%08x)", hr);
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        free(msg);
        CoUninitialize();
        return;
    }
    env->SetLongField(obj, WASAPISharedBackendHandle, (jlong)ctx);

    logger->debug(env, "Initialized WASAPI backend. Context: %p", ctx);
}

JNIEXPORT void JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_shutdown0
        (JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBackend.shutdown0");

    auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPISharedBackendHandle);
    IMMDeviceEnumerator* deviceEnumerator = ctx->deviceEnumerator;

    if (deviceEnumerator) {
        deviceEnumerator->Release();
        deviceEnumerator = nullptr;
    }
    CoUninitialize();

    logger->debug(env, "Shutdown WASAPI backend.");
}

JNIEXPORT jobjectArray JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getAllPorts0
        (JNIEnv* env, jobject obj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBackend.getAllPorts0");

    auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPISharedBackendHandle);
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

    logger->debug(env, "Found %d render ports and %d capture ports.", renderCount, captureCount);
    logger->debug(env, "Total %d ports.", totalCount);

    jobjectArray result = env->NewObjectArray(totalCount, audioPortClazz, nullptr);
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
        logger->debug(env, "Render port #%d: %ls", i, pDevice);
        pDevice->Release();
    }
    for (UINT i = 0; i < captureCount; i++) {
        IMMDevice* pDevice = nullptr;
        captureDevices->Item(i, &pDevice);
        jobject audioPortObj = IMMDevice_to_AudioPort(env, pDevice);
        env->SetObjectArrayElement(result, index++, audioPortObj);
        logger->debug(env, "Capture port #%d: %ls", i, pDevice);
        pDevice->Release();
    }

    renderDevices->Release();
    captureDevices->Release();

    return result;
}

JNIEXPORT jobject JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getDefaultPort0
        (JNIEnv* env, jobject obj, jobject flowObj) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBackend.getDefaultPort0");
    
    if (!flowObj) return nullptr;

    auto* ctx = (BackendContext*)env->GetLongField(obj, WASAPISharedBackendHandle);
    IMMDeviceEnumerator* deviceEnumerator = ctx->deviceEnumerator;

    if (!deviceEnumerator) {
        logger->warn(env, "No device enumerator.");
        return nullptr;
    }

    jobject audioFlowOutObj = env->GetStaticObjectField(audioFlowClazz, audioFlowOut);
    jobject audioFlowInObj = env->GetStaticObjectField(audioFlowClazz, audioFlowIn);

    EDataFlow flow = eRender;
    if (env->IsSameObject(flowObj, audioFlowOutObj)) {
        flow = eRender;
    } else if (env->IsSameObject(flowObj, audioFlowInObj)) {
        flow = eCapture;
    } else {
        return nullptr;
    }

    IMMDevice* pDevice = nullptr;
    HRESULT hr = deviceEnumerator->GetDefaultAudioEndpoint(flow, eConsole, &pDevice);
    if (FAILED(hr)) {
        logger->warn(env, "Failed to get default audio endpoint.");
        return nullptr;
    }

    if (!pDevice) {
        logger->warn(env, "No default audio endpoint.");
        return nullptr;
    }

    logger->debug(env, "Default audio endpoint: %s. Flow: %d", pDevice, flow);

    jobject jAudioPort = IMMDevice_to_AudioPort(env, pDevice);
    pDevice->Release();

    return jAudioPort;
}

JNIEXPORT jboolean JNICALL
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_isFormatSupported0
        (JNIEnv* env, jobject obj, jobject jport, jobject jformat) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBackend.isFormatSupported0");

    if (!jport || !jformat) {
        logger->warn(env, "AudioPort or AudioFormat is null.");
        return JNI_FALSE;
    }

    IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
    if (!device) {
        logger->warn(env, "Failed to get IMMDevice.");
        return JNI_FALSE;
    }

    WAVEFORMATEX* format = AudioFormat_to_WAVEFORMATEX(env, jformat);
    if (!format) {
        logger->warn(env, "Failed to get WAVEFORMATEX.");
        return JNI_FALSE;
    }

    IAudioClient* audioClient = nullptr;
    HRESULT hr = device->Activate(__uuidof(IAudioClient), CLSCTX_ALL, NULL, (void**)&audioClient);
    device->Release();
    if (FAILED(hr) || audioClient == nullptr) {
        logger->warn(env, "Failed to get or activate IAudioClient. (HRESULT: 0x%08x)", hr);
        return JNI_FALSE;
    }

    hr = audioClient->IsFormatSupported(AUDCLNT_SHAREMODE_SHARED, format, NULL);
    audioClient->Release();

    CoTaskMemFree(format);

    if (FAILED(hr)) return JNI_FALSE;
    return JNI_TRUE;
}
#else // end of _WIN32
JNIEXPORT void JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_initialize0
        (JNIEnv* env, jobject obj) {
    env->ThrowNew(javaLangUnsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT void JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_shutdown0
        (JNIEnv* env, jobject obj) {
    env->ThrowNew(javaLangUnsupportedOperationException, "Not supported on this platform.");
}

JNIEXPORT jobjectArray JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getAllPorts0
        (JNIEnv* env, jobject obj) {
    env->ThrowNew(javaLangUnsupportedOperationException, "Not supported on this platform.");
    return nullptr;
}

JNIEXPORT jobject JNICALL 
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getDefaultPort0
        (JNIEnv* env, jobject obj, jobject flowObj) {
    env->ThrowNew(javaLangUnsupportedOperationException, "Not supported on this platform.");
    return nullptr;
}

JNIEXPORT jboolean JNICALL
        Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_isFormatSupported0
        (JNIEnv* env, jobject obj, jobject jport, jobject jformat) {
    env->ThrowNew(javaLangUnsupportedOperationException, "Not supported on this platform.");
    return JNI_FALSE;
}
#endif // end of !_WIN32

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }

    if (!createCache(env)) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_8;
}

void JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return;
    }
    if (!releaseCache(env)) {
        return;
    }
}