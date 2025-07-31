#include "wasapi_backend.h"

#include "org_theko_sound_backend_wasapi_WASAPISharedBackend.h"

static IMMDeviceEnumerator* deviceEnumerator = NULL;

JNIEXPORT void JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_initialize0(JNIEnv* env, jobject obj) {
    HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    if (FAILED(hr)) {
        throwAudioBackendExceptionWithErrno(env, "Initialization \"CoInitializeEx\" failed.", hr);
        return;
    }

    hr = CoCreateInstance(
        &CLSID_MMDeviceEnumerator, NULL,
        CLSCTX_ALL, &IID_IMMDeviceEnumerator,
        (void**)&deviceEnumerator
    );

    if (FAILED(hr)) {
        throwAudioBackendExceptionWithErrno(env, "Initialization \"CoCreateInstance\" failed.", hr);
        CoUninitialize();
        return;
    }
}

JNIEXPORT void JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_shutdown0(JNIEnv* env, jobject obj) {
    if (deviceEnumerator) {
        deviceEnumerator->lpVtbl->Release(deviceEnumerator);
        deviceEnumerator = NULL;
    } 
    CoUninitialize();
}

JNIEXPORT jobjectArray JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getAllPorts0
(JNIEnv *env, jobject obj) {
    IMMDeviceEnumerator *pEnum = getDeviceEnumerator();
    if (!pEnum) return NULL;

    IMMDeviceCollection *pRender = getDevicesList(pEnum, eRender);
    IMMDeviceCollection *pCapture = getDevicesList(pEnum, eCapture);

    if (!pRender && !pCapture) {
        pEnum->lpVtbl->Release(pEnum);
        throwAudioBackendException(env, "Failed to get devices list.");
        return NULL;
    }

    UINT countRender = 0, countCapture = 0;
    if (pRender) pRender->lpVtbl->GetCount(pRender, &countRender);
    if (pCapture) pCapture->lpVtbl->GetCount(pCapture, &countCapture);

    UINT totalCount = countRender + countCapture;

    jclass clsAudioPort = (*env)->FindClass(env, "org/theko/sound/AudioPort");
    jobjectArray array = (*env)->NewObjectArray(env, totalCount, clsAudioPort, NULL);

    jclass clsAudioFlow = (*env)->FindClass(env, "org/theko/sound/AudioFlow");
    jfieldID fidOut = (*env)->GetStaticFieldID(env, clsAudioFlow, "OUT", "Lorg/theko/sound/AudioFlow;");
    jfieldID fidIn = (*env)->GetStaticFieldID(env, clsAudioFlow, "IN", "Lorg/theko/sound/AudioFlow;");
    jobject enumOut = (*env)->GetStaticObjectField(env, clsAudioFlow, fidOut);
    jobject enumIn = (*env)->GetStaticObjectField(env, clsAudioFlow, fidIn);

    UINT index = 0;
    if (pRender) {
        for (UINT i = 0; i < countRender; i++) {
            IMMDevice *pDevice = NULL;
            HRESULT hr = pRender->lpVtbl->Item(pRender, i, &pDevice);
            if (FAILED(hr) || !pDevice) continue;

            jobject port = IMMDevice_to_AudioPort(env, pDevice, enumOut);
            if (port != NULL) {
                (*env)->SetObjectArrayElement(env, array, index++, port);
            }

            pDevice->lpVtbl->Release(pDevice);
        }
        pRender->lpVtbl->Release(pRender);
    }

    if (pCapture) {
        for (UINT i = 0; i < countCapture; i++) {
            IMMDevice *pDevice = NULL;
            HRESULT hr = pCapture->lpVtbl->Item(pCapture, i, &pDevice);
            if (FAILED(hr) || !pDevice) continue;

            jobject port = IMMDevice_to_AudioPort(env, pDevice, enumIn);
            if (port != NULL) {
                (*env)->SetObjectArrayElement(env, array, index++, port);
            }

            pDevice->lpVtbl->Release(pDevice);
        }
        pCapture->lpVtbl->Release(pCapture);
    }

    pEnum->lpVtbl->Release(pEnum);
    return array;
}

JNIEXPORT jobject JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getDefaultPort0
(JNIEnv* env, jobject obj, jobject flowObj) {
    if (!flowObj || !deviceEnumerator) return NULL;

    jclass clsFlow = (*env)->GetObjectClass(env, flowObj);
    jmethodID midName = (*env)->GetMethodID(env, clsFlow, "name", "()Ljava/lang/String;");
    jstring flowName = (jstring)(*env)->CallObjectMethod(env, flowObj, midName);

    const char* cname = (*env)->GetStringUTFChars(env, flowName, NULL);
    EDataFlow flow = (strcmp(cname, "IN") == 0) ? eCapture : eRender;
    (*env)->ReleaseStringUTFChars(env, flowName, cname);

    IMMDevice* device = NULL;
    HRESULT hr = deviceEnumerator->lpVtbl->GetDefaultAudioEndpoint(deviceEnumerator, flow, eConsole, &device);
    if (FAILED(hr) || !device) {
        throwAudioBackendExceptionWithErrno(env, "Failed to get default device.", hr);
        return NULL;
    }

    jobject result = IMMDevice_to_AudioPort(env, device, flowObj);
    device->lpVtbl->Release(device);
    return result;
}

JNIEXPORT jobject JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getMixFormat0
  (JNIEnv* env, jobject obj, jobject jport) {
    debug(__FUNCTION__, "getting mix format");
    debug(__FUNCTION__, "getting device");
    IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
    if (!device) return NULL;

    IAudioClient* audioClient = NULL;
    debug(__FUNCTION__, "activating audio client");
    HRESULT hr = device->lpVtbl->Activate(device, &IID_IAudioClient, CLSCTX_ALL, NULL, (void**)&audioClient);
    device->lpVtbl->Release(device);
    if (FAILED(hr) || audioClient == NULL) return NULL;

    debug(__FUNCTION__, "getting mix format");
    WAVEFORMATEX* fmt = NULL;
    hr = audioClient->lpVtbl->GetMixFormat(audioClient, &fmt);
    audioClient->lpVtbl->Release(audioClient);
    if (FAILED(hr) || !fmt) return NULL;

    debug(__FUNCTION__, "converting mix format");
    WAVEFORMATEX* basicFmt = NULL;

    if (fmt->wFormatTag == /* WAVEFORMATEXTENSIBLE */ 0xFFFE) {
        const WAVEFORMATEXTENSIBLE* ext = (const WAVEFORMATEXTENSIBLE*)fmt;
        basicFmt = (WAVEFORMATEX*)CoTaskMemAlloc(sizeof(WAVEFORMATEX));
        memcpy(basicFmt, &ext->Format, sizeof(WAVEFORMATEX));
        basicFmt->wFormatTag = WAVE_FORMAT_PCM;
        CoTaskMemFree(fmt);
    }

    debug(__FUNCTION__, "returning mix format");
    jobject result = WAVEFORMATEX_to_AudioFormat(env, basicFmt);
    if (basicFmt) CoTaskMemFree(basicFmt);
    return result;
}

JNIEXPORT jboolean JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_isFormatSupported0
(JNIEnv* env, jobject obj, jobject jport, jobject jformat) {
    IMMDevice* device = AudioPort_to_IMMDevice(env, jport);
    if (!device) return JNI_FALSE;

    IAudioClient* audioClient = NULL;
    HRESULT hr = device->lpVtbl->Activate(device, &IID_IAudioClient, CLSCTX_ALL, NULL, (void**)&audioClient);
    device->lpVtbl->Release(device);
    if (FAILED(hr) || audioClient == NULL) return JNI_FALSE;

    WAVEFORMATEX* fmt = AudioFormat_to_WAVEFORMATEX(env, jformat);
    if (!fmt) {
        audioClient->lpVtbl->Release(audioClient);
        return JNI_FALSE;
    }

    WAVEFORMATEX* closestMatch = NULL;
    hr = audioClient->lpVtbl->IsFormatSupported(audioClient, AUDCLNT_SHAREMODE_EXCLUSIVE, fmt, &closestMatch);
    CoTaskMemFree(fmt);
    if (closestMatch) CoTaskMemFree(closestMatch);
    audioClient->lpVtbl->Release(audioClient);

    return (hr == S_OK || hr == S_FALSE) ? JNI_TRUE : JNI_FALSE;
}