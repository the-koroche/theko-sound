#pragma once

#ifdef _WIN32
#include <windows.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <initguid.h>

#include "wasapi_utils.hpp"
#include "classes_cache.hpp"
#include "helper_utilities.hpp"

#include "logger.hpp"
#include "logger_manager.hpp"

#include <jni.h>

static jobject WAVEFORMATEX_to_AudioFormat(JNIEnv* env, const WAVEFORMATEX* waveformat) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBridge.WAVEFORMATEX_to_AudioFormat");

    if (!waveformat) return nullptr;

    const GUID SUBTYPE_PCM = {0x00000001,0x0000,0x0010,{0x80,0x00,0x00,0xaa,0x00,0x38,0x9b,0x71}};
    const GUID SUBTYPE_IEEE_FLOAT = {0x00000003,0x0000,0x0010,{0x80,0x00,0x00,0xaa,0x00,0x38,0x9b,0x71}};

    WORD formatTag = waveformat->wFormatTag;
    if (formatTag == WAVE_FORMAT_EXTENSIBLE) {
        const WAVEFORMATEXTENSIBLE* ext = (const WAVEFORMATEXTENSIBLE*)waveformat;
        if (IsEqualGUID(ext->SubFormat, SUBTYPE_PCM)) {
            formatTag = WAVE_FORMAT_PCM;
        } else if (IsEqualGUID(ext->SubFormat, SUBTYPE_IEEE_FLOAT)) {
            formatTag = WAVE_FORMAT_IEEE_FLOAT;
        } else {
            logger->warn(env, "Unsupported WAVEFORMATEXTENSIBLE subformat.");
            env->ThrowNew(orgThekoSoundUnsupportedAudioEncodingException, 
                "Unsupported WAVEFORMATEXTENSIBLE subformat.");
            return nullptr;
        }
    }

    int sampleRate = waveformat->nSamplesPerSec;
    int channels = waveformat->nChannels;
    int bits = waveformat->wBitsPerSample;
    bool bigEndian = false;

    bool isFloat = formatTag == WAVE_FORMAT_IEEE_FLOAT;
    bool isPcm = formatTag == WAVE_FORMAT_PCM;
    
    logger->debug(env, "Audio format: [Sample Rate=%d, Channels=%d, SampleSizeInBits=%d, BigEndian=%d, IsFloat=%d, IsPcm=%d]",
        sampleRate, channels, bits, bigEndian, isFloat, isPcm);

    jobject jAudioEncoding = nullptr;
    if (isFloat) {
        jAudioEncoding = audioFormatEncodingFloatObj;
        logger->debug(env, "Audio encoding: FLOAT");
    } else if (isPcm) {
        if (bits == 8) {
            jAudioEncoding = audioFormatEncodingPcmUnsignedObj;
            logger->debug(env, "Audio encoding: PCM_UNSIGNED");
        } else {
            jAudioEncoding = audioFormatEncodingPcmSignedObj;
            logger->debug(env, "Audio encoding: PCM_SIGNED");
        }
    } else {
        logger->error(env, "Unsupported audio format tag.");
        env->ThrowNew(orgThekoSoundUnsupportedAudioEncodingException, "Unsupported audio format tag.");
        return nullptr;
    }

    jobject jAudioFormat = env->NewObject(
        audioFormatClazz, audioFormatCtor,
        sampleRate, bits, channels, jAudioEncoding, bigEndian);

    logger->debug(env, "Created jAudioFormat: %p", jAudioFormat);

    return jAudioFormat;
}

static WAVEFORMATEX* AudioFormat_to_WAVEFORMATEX(JNIEnv* env, jobject audioFormat) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBridge.AudioFormat_to_WAVEFORMATEX");

    if (!audioFormat) return nullptr;

    int sampleRate = env->CallIntMethod(audioFormat, audioFormatGetSampleRate);
    checkJavaException(env);
    int bits = env->CallIntMethod(audioFormat, audioFormatGetSampleSizeInBits);
    checkJavaException(env);
    int channels = env->CallIntMethod(audioFormat, audioFormatGetChannels);
    checkJavaException(env);
    jobject audioEncoding = env->CallObjectMethod(audioFormat, audioFormatGetEncoding);
    checkJavaException(env);

    if (!audioEncoding) {
        logger->error(env, "Unsupported audio format encoding.");
        env->ThrowNew(orgThekoSoundUnsupportedAudioEncodingException, "Unsupported audio format encoding.");
        return nullptr;
    }

    bool isFloat = env->IsSameObject(audioEncoding, audioFormatEncodingFloatObj);
    bool isPcm = env->IsSameObject(audioEncoding, audioFormatEncodingPcmSignedObj) ||
            env->IsSameObject(audioEncoding, audioFormatEncodingPcmUnsignedObj);

    if (!isFloat && !isPcm) {
        logger->error(env, "Unsupported audio format encoding.");
        env->ThrowNew(orgThekoSoundUnsupportedAudioEncodingException, "Unsupported audio format encoding.");
        return nullptr;
    }

    WAVEFORMATEX* format = (WAVEFORMATEX*)CoTaskMemAlloc(sizeof(WAVEFORMATEX));
    if (!format) {
        logger->error(env, "Memory allocation failed.");
        env->ThrowNew(javaLangOutOfMemoryException, "Memory allocation failed.");
        return nullptr;
    }
    memset(format, 0, sizeof(WAVEFORMATEX));

    format->wFormatTag = isFloat ? WAVE_FORMAT_IEEE_FLOAT : WAVE_FORMAT_PCM;
    format->nChannels = channels;
    format->nSamplesPerSec = sampleRate;
    format->wBitsPerSample = bits;
    format->nBlockAlign = (channels * bits) / 8;
    format->nAvgBytesPerSec = sampleRate * format->nBlockAlign;
    format->cbSize = 0;

    env->DeleteLocalRef(audioEncoding);

    return format; // need to use 'CoTaskMemFree'
}

static wchar_t* getAudioDeviceProperty(JNIEnv* env, IMMDevice* device, const PROPERTYKEY& key) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBridge.getAudioDeviceProperty");

    if (!device) return nullptr;

    IPropertyStore* pProps = nullptr;
    HRESULT hr = device->OpenPropertyStore(STGM_READ, &pProps);
    if (FAILED(hr)) {
        logger->warn(env, "Failed to open audio device property store.");
        return nullptr;
    }

    PROPVARIANT var;
    PropVariantInit(&var);

    hr = pProps->GetValue(key, &var);
    if (SUCCEEDED(hr)) {
        wchar_t* result = nullptr;
        if (var.vt == VT_LPWSTR && var.pwszVal) {
            size_t len = wcslen(var.pwszVal) + 1;
            result = (wchar_t*)CoTaskMemAlloc(sizeof(wchar_t) * len);
            wcscpy_s(result, len, var.pwszVal);

            logger->debug(env, "Obtained audio device property (VT_LPWSTR): %ls", result);
        } else {
            logger->debug(env, "Obtained audio device property: %ls", result);
        }
        PropVariantClear(&var);

        pProps->Release();
        return result;
    }
    PropVariantClear(&var);
    pProps->Release();
    logger->info(env, "Failed to get audio device property. (HRESULT: 0x%08x)", hr);
    return nullptr;
}

static jobject IMMDevice_to_AudioPort(JNIEnv* env, IMMDevice* pDevice) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBridge.IMMDevice_to_AudioPort");

    if (!pDevice) return nullptr;
    
    checkJavaException(env);

    // Open property store
    IPropertyStore *pProps = nullptr;
    HRESULT hr = pDevice->OpenPropertyStore(STGM_READ, &pProps);
    if (FAILED(hr)) {
        char* msg = format("Failed to open property store. (HRESULT: 0x%08x)", hr);
        logger->debug(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        free(msg);
        return nullptr;
    }

    // Obtain device info
    wchar_t* name = getAudioDeviceProperty(env, pDevice, PKEY_Device_FriendlyName);
    wchar_t* manufacturer = getAudioDeviceProperty(env, pDevice, PKEY_Device_Manufacturer);
    // TODO: get accurate version
    wchar_t* version = nullptr;
    wchar_t* description = getAudioDeviceProperty(env, pDevice, PKEY_Device_DeviceDesc);

    if (!name) name = com_memalloc_literal(L"Unknown");
    if (!manufacturer) manufacturer = com_memalloc_literal(L"Unknown");
    if (!version) version = com_memalloc_literal(L"Unknown");
    if (!description) description = com_memalloc_literal(L"Unknown");

    logger->debug(env, "Obtained device info. Name: %ls, Manufacturer: %ls, Version: %ls, Description: %ls",
        name, manufacturer, version, description);

    jstring jName = env->NewString((const jchar*)name, wcslen(name));
    jstring jManufacturer = env->NewString((const jchar*)manufacturer, wcslen(manufacturer));
    jstring jVersion = env->NewString((const jchar*)version, wcslen(version));
    jstring jDescription = env->NewString((const jchar*)description, wcslen(description));

    // Obtain WASAPI device ID
    jstring jHandle = nullptr;
    LPWSTR deviceId = nullptr;
    hr = pDevice->GetId(&deviceId);
    if (SUCCEEDED(hr) && deviceId) {
        char* utf8_deviceId = utf16_to_utf8(deviceId);
        jHandle = env->NewStringUTF(utf8_deviceId);
        logger->debug(env, "Obtained WASAPI device ID: %s", utf8_deviceId);
        free(utf8_deviceId);
        CoTaskMemFree(deviceId);
    }

    checkJavaException(env);

    if (!jHandle) {
        const char* msg = "Failed to obtain WASAPI device ID";
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        return nullptr;
    }

    // Create WASAPI native handle
    jobject jNativeHandleObj = env->NewObject(wasapiNativeHandleClazz, wasapiNativeHandleCtor, jHandle);

    checkJavaException(env);

    // Get flow
    jobject jFlowObj = nullptr;

    IMMEndpoint *endpoint = nullptr;
    hr = pDevice->QueryInterface(IID_IMMEndpoint, (void**)&endpoint);
    if (SUCCEEDED(hr) && endpoint) {
        EDataFlow flow = eRender;
        endpoint->GetDataFlow(&flow);
        endpoint->Release();

        if (flow == eRender) {
            jFlowObj = audioFlowOutObj;
        } else if (flow == eCapture) {
            jFlowObj = audioFlowInObj;
        }
        logger->debug(env, "Obtained flow: %s", flow == eRender ? "Render" : "Capture");
    } else {
        char* msg = format("Failed to get flow. (HRESULT: 0x%08x)", hr);
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        free(msg);
        return nullptr;
    }

    checkJavaException(env); // To ensure all previous exceptions are cleared
    
    // Get is active flag
    DWORD state = 0;
    hr = pDevice->GetState(&state);
    if (FAILED(hr)) {
        char* msg = format("Failed to get device state. (HRESULT: 0x%08x)", hr);
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        free(msg);
        return nullptr;
    }

    bool isActive = (state & DEVICE_STATE_ACTIVE) != 0;
    jboolean jIsActive = isActive ? JNI_TRUE : JNI_FALSE;
    logger->debug(env, "Obtained is active flag: %s", isActive ? "true" : "false");

    checkJavaException(env); // To ensure all previous exceptions are cleared

    // Get mix format
    WAVEFORMATEX* mixFormat = getMixFormat(pDevice);
    if (!mixFormat && isActive) {
        const char* msg = "Failed to get mix format";
        logger->error(env, msg);
        env->ThrowNew(orgThekoSoundBackendAudioBackendException, msg);
        return nullptr;
    } else if (!mixFormat && !isActive) {
        logger->info(env, "Device is not active, and mix format is not available.");
    }
    jobject jAudioMixFormat = (mixFormat ?WAVEFORMATEX_to_AudioFormat(env, mixFormat) : nullptr);

    // Create AudioPort
    jobject audioPort = env->NewObject(audioPortClazz, audioPortCtor,
        jNativeHandleObj,
        jFlowObj, jIsActive, jAudioMixFormat, 
        jName, jManufacturer, jVersion, jDescription);

    // Cleanup
    CoTaskMemFree(name);
    CoTaskMemFree(manufacturer);
    CoTaskMemFree(version);
    CoTaskMemFree(description);

    CoTaskMemFree(mixFormat);

    pProps->Release();

    return audioPort;
}

static IMMDevice* AudioPort_to_IMMDevice(JNIEnv* env, jobject jAudioPort) {
    Logger* logger = getLoggerManager()->getLogger(env, "<Native> : WASAPIBridge.AudioPort_to_IMMDevice");

    if (!jAudioPort) return nullptr;

    // Check if it's an AudioPort
    if (!env->IsInstanceOf(jAudioPort, audioPortClazz)) {
        logger->warn(env, "Not an AudioPort.");
        return nullptr;
    }

    jobject jNativeHandle = env->CallObjectMethod(jAudioPort, audioPortGetLink);
    checkJavaException(env);

    // Check if it's a WASAPI native handle
    if (!jNativeHandle) return nullptr;
    if (!env->IsInstanceOf(jNativeHandle, wasapiNativeHandleClazz)) {
        logger->warn(env, "AudioPort link is not a WASAPI native handle.");
        return nullptr;
    }

    jstring jHandle = (jstring)env->CallObjectMethod(jNativeHandle, wasapiNativeHandleGetHandle);
    checkJavaException(env);
    if (!jHandle) return nullptr;

    const char *handle = env->GetStringUTFChars(jHandle, nullptr);
    const wchar_t *wHandle = utf8_to_utf16(handle);

    IMMDeviceEnumerator* pEnum = getDeviceEnumerator();
    IMMDevice* pDevice = nullptr;
    HRESULT hr = pEnum->GetDevice(wHandle, &pDevice);

    free((void*)wHandle);

    env->ReleaseStringUTFChars(jHandle, handle);
    env->DeleteLocalRef(jHandle);
    env->DeleteLocalRef(jNativeHandle);

    pEnum->Release();
    if (!SUCCEEDED(hr)) return nullptr;

    return pDevice;
}
#endif // _WIN32