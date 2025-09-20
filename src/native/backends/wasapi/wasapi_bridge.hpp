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

#pragma once

#ifdef _WIN32
#include <windows.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <initguid.h>

#include <jni.h>

#include "wasapi_utils.hpp"
#include "helper_utilities.hpp"

#include "DefaultClassesCache.hpp"
#include "WASAPIClassesCache.hpp"

#include "logger.hpp"
#include "logger_manager.hpp"

/**
 * Converts a native WAVEFORMATEX object to a org.theko.sound.AudioFormat object.
 *
 * The conversion process involves retrieving the sample rate, number of channels, and number of bits per sample from the WAVEFORMATEX object.
 * The audio encoding is retrieved and checked to be either PCM (signed/unsigned) or IEEE Float.
 * If the audio encoding is not supported, an exception is thrown.
 *
 * @param env the JNI environment.
 * @param waveformat the native WAVEFORMATEX object to be converted.
 * @return a pointer to the AudioFormat object (jobject), or nullptr if the conversion fails.
 */
static jobject WAVEFORMATEX_to_AudioFormat(JNIEnv* env, const WAVEFORMATEX* waveformat) {
    Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPIBridge.WAVEFORMATEX -> AudioFormat");

    if (!waveformat) return nullptr;

    const GUID SUBTYPE_PCM = {0x00000001,0x0000,0x0010,{0x80,0x00,0x00,0xaa,0x00,0x38,0x9b,0x71}};
    const GUID SUBTYPE_IEEE_FLOAT = {0x00000003,0x0000,0x0010,{0x80,0x00,0x00,0xaa,0x00,0x38,0x9b,0x71}};

    ExceptionClassesCache* exceptionClasses = ExceptionClassesCache::get(env);
    AudioFormatEncodingCache* encodingCache = AudioFormatEncodingCache::get(env);
    AudioFormatCache* formatCache = AudioFormatCache::get(env);

    WORD formatTag = waveformat->wFormatTag;
    if (formatTag == WAVE_FORMAT_EXTENSIBLE) {
        const WAVEFORMATEXTENSIBLE* ext = (const WAVEFORMATEXTENSIBLE*)waveformat;
        if (IsEqualGUID(ext->SubFormat, SUBTYPE_PCM)) {
            formatTag = WAVE_FORMAT_PCM;
        } else if (IsEqualGUID(ext->SubFormat, SUBTYPE_IEEE_FLOAT)) {
            formatTag = WAVE_FORMAT_IEEE_FLOAT;
        } else {
            logger->warn(env, "Unsupported WAVEFORMATEXTENSIBLE subformat.");
            env->ThrowNew(exceptionClasses->unsupportedAudioEncodingException, 
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

    logger->trace(env, "Audio Format info: formatTag=%d, sampleRate=%d, bits=%d, channels=%d, isFloat=%d, isPcm=%d",
            formatTag, sampleRate, bits, channels, isFloat, isPcm);

    jobject jAudioEncoding = nullptr;
    if (isFloat) {
        jAudioEncoding = encodingCache->pcmFloatObj;
        logger->trace(env, "Audio Format encoding: PCM_FLOAT");
    } else if (isPcm) {
        if (bits == 8) {
            jAudioEncoding = encodingCache->pcmUnsignedObj;
            logger->trace(env, "Audio Format encoding: PCM_UNSIGNED");
        } else {
            jAudioEncoding = encodingCache->pcmSignedObj;
            logger->trace(env, "Audio Format encoding: PCM_SIGNED");
        }
    } else {
        logger->error(env, "Unsupported audio format tag.");
        env->ThrowNew(exceptionClasses->unsupportedAudioFormatException, "Unsupported audio format tag.");
        return nullptr;
    }

    jobject jAudioFormat = env->NewObject(
        formatCache->clazz, 
        formatCache->ctor,
        sampleRate, bits, channels, jAudioEncoding, bigEndian);

    logger->trace(env, "Created AudioFormat. Pointer: %s", FORMAT_PTR(jAudioFormat));

    return jAudioFormat;
}

/**
 * Converts a org.theko.sound.AudioFormat object to a native WAVEFORMATEX object.
 *
 * The conversion process involves retrieving the sample rate, number of channels, and number of bits per sample from the Java AudioFormat object.
 * The audio encoding is retrieved and checked to be either PCM_FLOAT or PCM_SIGNED/UNSIGNED.
 * If the audio encoding is not supported, an exception is thrown.
 *
 * @param env the JNI environment.
 * @param audioFormat the Java AudioFormat object to be converted.
 * @return a pointer to the native WAVEFORMATEX object, or nullptr if the conversion fails.
 */
static WAVEFORMATEX* AudioFormat_to_WAVEFORMATEX(JNIEnv* env, jobject audioFormat) {
    Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPIBridge.AudioFormat -> WAVEFORMATEX");

    if (!audioFormat) return nullptr;
    
    AudioFormatCache* formatCache = AudioFormatCache::get(env);
    AudioFormatEncodingCache* encodingCache = AudioFormatEncodingCache::get(env);
    ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);

    int sampleRate = JNI_TRY_RETURN(env->CallIntMethod(audioFormat, formatCache->getSampleRate));
    int bits = JNI_TRY_RETURN(env->CallIntMethod(audioFormat, formatCache->getBitsPerSample));
    int channels = JNI_TRY_RETURN(env->CallIntMethod(audioFormat, formatCache->getChannels));
    jobject audioEncoding = JNI_TRY_RETURN(env->CallObjectMethod(audioFormat, formatCache->getEncoding));

    logger->trace(env, "SampleRate=%d, Bits=%d, Channels=%d, AudioEncoding=%p", sampleRate, bits, channels, audioEncoding);

    if (!audioEncoding) {
        logger->error(env, "Unsupported audio format encoding.");
        env->ThrowNew(exceptionsCache->unsupportedAudioEncodingException, "Unsupported audio format encoding.");
        return nullptr;
    }

    bool isFloat = env->IsSameObject(audioEncoding, encodingCache->pcmFloatObj);
    bool isPcm = env->IsSameObject(audioEncoding, encodingCache->pcmUnsignedObj) ||
            env->IsSameObject(audioEncoding, encodingCache->pcmSignedObj);

    logger->trace(env, "Audio Encoding: isFloat=%d, isPcm=%d", isFloat, isPcm);

    if (!isFloat && !isPcm) {
        logger->error(env, "Unsupported audio format encoding.");
        env->ThrowNew(exceptionsCache->unsupportedAudioEncodingException, "Unsupported audio format encoding.");
        return nullptr;
    }

    WAVEFORMATEX* format = (WAVEFORMATEX*)CoTaskMemAlloc(sizeof(WAVEFORMATEX));
    if (!format) {
        logger->error(env, "Memory allocation failed.");
        env->ThrowNew(exceptionsCache->outOfMemoryException, "Memory allocation failed.");
        return nullptr;
    }
    memset(format, 0, sizeof(WAVEFORMATEX));

    logger->trace(env, "Created WAVEFORMATEX. Pointer: %s", FORMAT_PTR(format));

    format->wFormatTag = isFloat ? WAVE_FORMAT_IEEE_FLOAT : WAVE_FORMAT_PCM;
    format->nChannels = channels;
    format->nSamplesPerSec = sampleRate;
    format->wBitsPerSample = bits;
    format->nBlockAlign = (channels * bits) / 8;
    format->nAvgBytesPerSec = sampleRate * format->nBlockAlign;
    format->cbSize = 0;

    env->DeleteLocalRef(audioEncoding);

    return format; // caller must free using 'CoTaskMemFree()'
}

/**
 * Retrieves a property value from an audio device.
 *
 * The function opens the property store of the specified audio device and retrieves the
 * value of the property identified by the specified PROPERTYKEY.
 *
 * If the property store cannot be opened or if the property value cannot be retrieved,
 * nullptr is returned.
 *
 * The returned pointer is owned by the caller and should be released when it is no longer needed
 * using CoTaskMemFree(result).
 *
 * @param env the JNI environment.
 * @param device the IMMDevice object associated with the audio device to retrieve the property
 *         value from.
 * @param key the PROPERTYKEY object identifying the property to retrieve.
 * @return a pointer to the retrieved property value, or nullptr if the call fails.
 */
static wchar_t* getAudioDeviceProperty(JNIEnv* env, IMMDevice* device, const PROPERTYKEY& key) {
    Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPIBridge.getAudioDeviceProperty");

    if (!device) return nullptr;

    IPropertyStore* pProps = nullptr;
    HRESULT hr = device->OpenPropertyStore(STGM_READ, &pProps);
    if (FAILED(hr)) {
        logger->warn(env, "Failed to open audio device property store.");
        return nullptr;
    }

    logger->trace(env, "Opened audio device property store. Pointer: %s", FORMAT_PTR(pProps));
 
    PROPVARIANT var;
    PropVariantInit(&var);

    logger->trace(env, "Trying to get audio device property.");

    hr = pProps->GetValue(key, &var);
    if (SUCCEEDED(hr)) {
        wchar_t* result = nullptr;
        if (var.vt == VT_LPWSTR && var.pwszVal) {
            size_t len = wcslen(var.pwszVal) + 1;
            result = (wchar_t*)CoTaskMemAlloc(sizeof(wchar_t) * len);
            wcscpy_s(result, len, var.pwszVal);

            logger->trace(env, "Obtained audio device property (VT_LPWSTR): %ls", (result ? result : L"N\\A"));
        } else {
            logger->trace(env, "Obtained audio device property: %ls", (result ? result : L"N\\A"));
        }
        PropVariantClear(&var);

        pProps->Release();
        return result;
    }
    PropVariantClear(&var);
    pProps->Release();

    char* hr_msg = format_hr_msg(hr);
    logger->info(env, "Failed to get audio device property. (%s)", hr_msg);
    free(hr_msg);

    return nullptr;
}

/**
 * Converts an IMMDevice to an org.theko.sound.AudioPort.
 * 
 * @param env The JNIEnv which will be used to access the Java objects.
 * @param pDevice The IMMDevice to be converted.
 * @return The converted AudioPort, or nullptr if failed.
 */
static jobject IMMDevice_to_AudioPort(JNIEnv* env, IMMDevice* pDevice) {
    Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPIBridge.IMMDevice -> AudioPort");

    if (!pDevice) return nullptr;

    AudioFlowCache* flowCache = AudioFlowCache::get(env);
    AudioPortCache* portCache = AudioPortCache::get(env);
    ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);
    WASAPIPortHandleCache* handleCache = WASAPIPortHandleCache::get(env);

    // Open property store
    IPropertyStore *pProps = nullptr;
    HRESULT hr = pDevice->OpenPropertyStore(STGM_READ, &pProps);
    if (FAILED(hr)) {
        char* hr_msg = format_hr_msg(hr);
        char* msg = format("Failed to open property store. (%s)", hr_msg);
        free(hr_msg);
        logger->debug(env, msg);
        env->ThrowNew(exceptionsCache->audioBackendException, msg);
        free(msg);
        return nullptr;
    }

    // Obtain device info
    wchar_t* name = getAudioDeviceProperty(env, pDevice, PKEY_Device_FriendlyName);
    wchar_t* manufacturer = getAudioDeviceProperty(env, pDevice, PKEY_Device_Manufacturer);
    // TODO: get accurate version
    wchar_t* version = nullptr;
    wchar_t* description = getAudioDeviceProperty(env, pDevice, PKEY_Device_DeviceDesc);

    logger->trace(env, "Obtained audio device info. Name: %ls, Manufacturer: %ls, Version: %ls, Description: %ls",
            name, manufacturer, version, description);

    if (!name) name = com_memalloc_literal_utf16(L"Unknown");
    if (!manufacturer) manufacturer = com_memalloc_literal_utf16(L"Unknown");
    if (!version) version = com_memalloc_literal_utf16(L"Unknown");
    if (!description) description = com_memalloc_literal_utf16(L"Unknown");

    jstring jName = env->NewString((const jchar*)name, wcslen(name));
    jstring jManufacturer = env->NewString((const jchar*)manufacturer, wcslen(manufacturer));
    jstring jVersion = env->NewString((const jchar*)version, wcslen(version));
    jstring jDescription = env->NewString((const jchar*)description, wcslen(description));

    logger->trace(env, "Created java strings. Name: %s, Manufacturer: %s, Version: %s, Description: %s",
            FORMAT_PTR(jName), FORMAT_PTR(jManufacturer), FORMAT_PTR(jVersion), FORMAT_PTR(jDescription));

    // Obtain WASAPI device ID
    jstring jHandle = nullptr;
    LPWSTR deviceId = nullptr;
    hr = pDevice->GetId(&deviceId);
    if (SUCCEEDED(hr) && deviceId) {
        char* utf8_deviceId = utf16_to_utf8(deviceId);
        jHandle = env->NewStringUTF(utf8_deviceId);
        logger->trace(env, "Obtained WASAPI device ID: %s", utf8_deviceId);
        free(utf8_deviceId);
        CoTaskMemFree(deviceId);
    }

    if (!jHandle) {
        const char* msg = "Failed to obtain WASAPI device ID";
        logger->error(env, msg);
        env->ThrowNew(exceptionsCache->audioBackendException, msg);
        return nullptr;
    }

    // Create WASAPI native handle
    jobject jNativeHandleObj = JNI_TRY_RETURN(env->NewObject(
        handleCache->clazz,
        handleCache->ctor,
        jHandle
    ));

    logger->trace(env, "Created WASAPI native handle: %s", FORMAT_PTR(jNativeHandleObj));

    // Get flow
    jobject jFlowObj = nullptr;

    IMMEndpoint *endpoint = nullptr;
    hr = pDevice->QueryInterface(IID_IMMEndpoint, (void**)&endpoint);
    if (SUCCEEDED(hr) && endpoint) {
        EDataFlow flow = eRender;
        endpoint->GetDataFlow(&flow);
        endpoint->Release();

        if (flow == eRender) {
            jFlowObj = flowCache->outObj;
        } else if (flow == eCapture) {
            jFlowObj = flowCache->inObj;
        }
        logger->trace(env, "Obtained audio flow. Pointer: %s", FORMAT_PTR(jFlowObj));
    } else {
        char* hr_msg = format_hr_msg(hr);
        char* msg = format("Failed to get flow. (%s)", hr_msg);
        free(hr_msg);
        logger->error(env, msg);
        env->ThrowNew(exceptionsCache->audioBackendException, msg);
        free(msg);
        return nullptr;
    }
    
    // Get is active flag
    DWORD state = 0;
    hr = pDevice->GetState(&state);
    if (FAILED(hr)) {
        char* hr_msg = format_hr_msg(hr);
        char* msg = format("Failed to get device state. (%s)", hr_msg);
        free(hr_msg);
        logger->error(env, msg);
        env->ThrowNew(exceptionsCache->audioBackendException, msg);
        free(msg);
        return nullptr;
    }

    bool isActive = (state & DEVICE_STATE_ACTIVE) != 0;
    jboolean jIsActive = isActive ? JNI_TRUE : JNI_FALSE;
    logger->trace(env, "Obtained is active flag: %d", isActive);

    // Get mix format
    WAVEFORMATEX* mixFormat = getMixFormat(pDevice);
    if (!mixFormat && isActive) {
        const char* msg = "Failed to get mix format";
        logger->error(env, msg);
        env->ThrowNew(exceptionsCache->audioBackendException, msg);
        return nullptr;
    } else if (!mixFormat && !isActive) {
        logger->info(env, "Device is not active, and mix format is not available.");
    }
    logger->trace(env, "Obtained WAVEFORMATEX. Pointer: %s", FORMAT_PTR(mixFormat));

    jobject jAudioMixFormat = (mixFormat ?WAVEFORMATEX_to_AudioFormat(env, mixFormat) : nullptr);
    logger->trace(env, "Created AudioFormat. Pointer: %s", FORMAT_PTR(jAudioMixFormat));

    // Create AudioPort
    jobject audioPort = env->NewObject(
        portCache->clazz, 
        portCache->ctor,
        jNativeHandleObj,
        jFlowObj, jIsActive, jAudioMixFormat, 
        jName, jManufacturer, jVersion, jDescription);

    logger->trace(env, "Created AudioPort. Pointer: %s", FORMAT_PTR(audioPort));

    // Cleanup
    CoTaskMemFree(name);
    CoTaskMemFree(manufacturer);
    CoTaskMemFree(version);
    CoTaskMemFree(description);

    CoTaskMemFree(mixFormat);

    pProps->Release();

    return audioPort;
}

/**
 * Converts an org.theko.sound.AudioPort to its corresponding IMMDevice.
 *
 * @param env The JNI environment.
 * @param jAudioPort The AudioPort to convert.
 * @return The corresponding IMMDevice, or nullptr if failed.
 */
static IMMDevice* AudioPort_to_IMMDevice(JNIEnv* env, jobject jAudioPort) {
    Logger* logger = LoggerManager::getManager()->getLogger(env, "NATIVE: WASAPIBridge.AudioPort -> IMMDevice");

    AudioPortCache* portCache = AudioPortCache::get(env);
    ExceptionClassesCache* exceptionsCache = ExceptionClassesCache::get(env);
    WASAPIPortHandleCache* handleCache = WASAPIPortHandleCache::get(env);

    if (!jAudioPort || !env->IsInstanceOf(jAudioPort, portCache->clazz)) {
        logger->warn(env, "Invalid or null AudioPort.");
        return nullptr;
    }

    jobject jNativeHandle = JNI_TRY_RETURN(env->CallObjectMethod(jAudioPort, portCache->getLink));
    if (!jNativeHandle || !env->IsInstanceOf(jNativeHandle, handleCache->clazz)) {
        logger->warn(env, "Invalid or null native handle.");
        if (jNativeHandle) env->DeleteLocalRef(jNativeHandle);
        return nullptr;
    }

    jstring jHandle = (jstring)JNI_TRY_RETURN(env->CallObjectMethod(jNativeHandle, handleCache->getHandle));
    
    if (!jHandle) {
        env->DeleteLocalRef(jNativeHandle);
        logger->warn(env, "Invalid or null handle.");
        return nullptr;
    }

    const char *handle = env->GetStringUTFChars(jHandle, nullptr);
    const wchar_t *wHandle = utf8_to_utf16(handle);

    IMMDevice* pDevice = nullptr;

    if (wHandle) {
        IMMDeviceEnumerator* pEnum = getDeviceEnumerator();
        HRESULT hr = pEnum->GetDevice(wHandle, &pDevice);
        pEnum->Release();

        if (!SUCCEEDED(hr)) {
            char* hr_msg = format_hr_msg(hr);
            char* msg = format("Failed to get audio device. (%s)", hr_msg);
            free(hr_msg);
            logger->error(env, msg);
            env->ThrowNew(exceptionsCache->audioBackendException, msg);
            free(msg);
            pDevice = nullptr;
        } else {
            logger->debug(env, "Obtained IMMDevice. Handle: %ls", wHandle);
        }

        free((void*)wHandle);
    } else {
        logger->warn(env, "Failed to convert handle to UTF-16 string.");
    }

    env->ReleaseStringUTFChars(jHandle, handle);
    env->DeleteLocalRef(jHandle);
    env->DeleteLocalRef(jNativeHandle);
    return pDevice;
}
#endif // _WIN32