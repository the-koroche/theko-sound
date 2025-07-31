#ifndef WASAPI_BACKEND_H
#define WASAPI_BACKEND_H

#include <windows.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <initguid.h>
#include <stdio.h>
#include <stdlib.h>

#include <jni.h>

#define DEBUG

// org.theko.sound.AudioFlow enum
// AudioFlow.IN, AudioFlow.OUT
// to create from boolean: AudioFlow flow = AudioFlow.fromBoolean(isOutput); // true = AudioFlow.OUT, false = AudioFlow.IN

// org.theko.sound.AudioPort constructor
// AudioPort(Object link, AudioFlow flow, String name, String vendor, String version, String description)
// Object       AudioPort.getLink()
// AudioFlow    AudioPort.getFlow()
// String       AudioPort.getName()
// String       AudioPort.getVendor()
// String       AudioPort.getVersion()
// String       AudioPort.getDescription()

// org.theko.sound.AudioFormat constructor
// public AudioFormat (int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian)
// public AudioFormat (int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian, int frameSize, int byteRate)
// int          AudioFormat.getSampleRate()
// int          AudioFormat.getSampleSizeInBits()
// int          AudioFormat.getSampleSizeInBytes()
// int          AudioFormat.getChannels()
// Encoding     AudioFormat.getEncoding()
// boolean      AudioFormat.isBigEndian()
// int          AudioFormat.getFrameSize()
// int          AudioFormat.getByteRate()

// org.theko.sound.AudioFormat$Encoding enum
// AudioFormat.Encoding.PCM_FLOAT, AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED

// WASAPI BACKEND H DECLARATIONS
// --> EXCEPTIONS
// static void throwOutOfMemoryError(JNIEnv *env)
// static void throwAudioBackendException(JNIEnv *env, const char *msg)
// static void throwAudioBackendExceptionWithErrno(JNIEnv *env, const char *msg, HRESULT hr)
// --> DEBUG
// static void printWAVEFORMATEX(const WAVEFORMATEX* format)
// --> UTILS
// static IMMDeviceEnumerator* getDeviceEnumerator()
// static IMMDeviceCollection* getDevicesList(IMMDeviceEnumerator* pEnum, EDataFlow flow)
// static jstring tryGetProperty(JNIEnv *env, IPropertyStore *pProps, const PROPERTYKEY *key, const char *fallback)
// static jobject IMMDevice_to_AudioPort(JNIEnv *env, IMMDevice *pDevice, jobject flow)
// static IMMDevice* AudioPort_to_IMMDevice(JNIEnv *env, jobject jAudioPort)
// static jobject WAVEFORMATEX_to_AudioFormat(JNIEnv *env, const WAVEFORMATEX* format)
// static WAVEFORMATEX* AudioFormat_to_WAVEFORMATEX(JNIEnv *env, jobject jFormat)

// WASAPI BACKEND C DECLARATIONS
// JNIEXPORT void JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_initialize0(JNIEnv* env, jobject obj)
// JNIEXPORT void JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_shutdown0(JNIEnv* env, jobject obj)
// JNIEXPORT jobjectArray JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getAllPorts0(JNIEnv *env, jobject obj)
// JNIEXPORT jobject JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_getDefaultPort0(JNIEnv* env, jobject obj, jobject flowObj)
// JNIEXPORT jboolean JNICALL Java_org_theko_sound_backend_wasapi_WASAPISharedBackend_isFormatSupported0(JNIEnv* env, jobject obj, jobject jport, jobject jformat)

static void throwOutOfMemoryError(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "org/theko/sound/backend/OutOfMemoryException"), msg); \
}

static void throwAudioBackendException(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "org/theko/sound/backend/AudioBackendException"), msg);
}

static void throwAudioBackendExceptionWithErrno(JNIEnv *env, const char *msg, HRESULT hr) {
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "%s Errno: 0x%lx", msg, hr);
    (*env)->ThrowNew(env, (*env)->FindClass(env, "org/theko/sound/backend/AudioBackendException"), buffer);
}

static void throwUnsupportedAudioFormatException(JNIEnv *env, const char *msg) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "org/theko/sound/backend/UnsupportedAudioFormatException"), msg);
}

static void printWAVEFORMATEX(const WAVEFORMATEX* format) {
    printf("WAVEFORMATEX:\n");
    printf("wFormatTag: %d\n", format->wFormatTag);
    printf("nChannels: %d\n", format->nChannels);
    printf("nSamplesPerSec: %d\n", format->nSamplesPerSec);
    printf("nAvgBytesPerSec: %d\n", format->nAvgBytesPerSec);
    printf("nBlockAlign: %d\n", format->nBlockAlign);
    printf("wBitsPerSample: %d\n", format->wBitsPerSample);
    printf("cbSize: %d\n", format->cbSize);
}

static void debug(const char *func, const char *msg) {
    #ifdef DEBUG
    printf("[native] DEBUG %s - %s\n", func, msg);
    #endif
}

static void debugf(const char *func, const char *fmt, ...) {
    #ifdef DEBUG
    char buffer[256];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);
    printf("[native] DEBUG %s - %s\n", func, buffer);
    #endif
}

static BOOL compareFormats(const WAVEFORMATEX* a, const WAVEFORMATEX* b) {
    return a->wFormatTag == b->wFormatTag &&
           a->nChannels == b->nChannels &&
           a->nSamplesPerSec == b->nSamplesPerSec &&
           a->wBitsPerSample == b->wBitsPerSample &&
           a->nBlockAlign == b->nBlockAlign;
}

static WAVEFORMATEX* cloneWaveFormat(const WAVEFORMATEX* src) {
    debug(__FUNCTION__, "cloning WAVEFORMATEX");
    WAVEFORMATEX* dst = (WAVEFORMATEX*)CoTaskMemAlloc(sizeof(WAVEFORMATEX));
    if (dst) memcpy(dst, src, sizeof(WAVEFORMATEX));
    return dst;
}

static IMMDeviceEnumerator* getDeviceEnumerator() {
    IMMDeviceEnumerator *pEnum = NULL;
    debug(__FUNCTION__, "creating IMMDeviceEnumerator");
    HRESULT hr = CoCreateInstance(&CLSID_MMDeviceEnumerator, NULL, CLSCTX_ALL,
        &IID_IMMDeviceEnumerator, (void**)&pEnum);
        return SUCCEEDED(hr) ? pEnum : NULL;
}

static IMMDeviceCollection* getDevicesList(IMMDeviceEnumerator* pEnum, EDataFlow flow) {
    IMMDeviceCollection *pCollection = NULL;
    HRESULT hr = pEnum->lpVtbl->EnumAudioEndpoints(pEnum, flow, DEVICE_STATE_ACTIVE, &pCollection);
    debug(__FUNCTION__, "enumerating devices");
    return SUCCEEDED(hr) ? pCollection : NULL;
}

static jstring tryGetProperty(JNIEnv *env, IPropertyStore *pProps, const PROPERTYKEY *key, const char *fallback) {
    PROPVARIANT var;
    PropVariantInit(&var);
    HRESULT hr = pProps->lpVtbl->GetValue(pProps, key, &var);
    jstring result = NULL;
    if (SUCCEEDED(hr) && var.vt == VT_LPWSTR && var.pwszVal) {
        int len = WideCharToMultiByte(CP_UTF8, 0, var.pwszVal, -1, NULL, 0, NULL, NULL);
        char *utf8 = malloc(len);
        WideCharToMultiByte(CP_UTF8, 0, var.pwszVal, -1, utf8, len, NULL, NULL);
        result = (*env)->NewStringUTF(env, utf8);
        free(utf8);
    }
    PropVariantClear(&var);
    if (!result && fallback)
        result = (*env)->NewStringUTF(env, fallback);
    return result;
}

static jobject IMMDevice_to_AudioPort(JNIEnv *env, IMMDevice *pDevice, jobject flow) {
    if (!pDevice || !flow) return NULL;

    IPropertyStore *pProps = NULL;
    HRESULT hr = pDevice->lpVtbl->OpenPropertyStore(pDevice, STGM_READ, &pProps);
    if (FAILED(hr)) {
        throwAudioBackendExceptionWithErrno(env, "Failed to open property store.", hr);
        return NULL;
    }

    jclass clsAudioPort = (*env)->FindClass(env, "org/theko/sound/AudioPort");
    jmethodID ctor = (*env)->GetMethodID(env, clsAudioPort, "<init>",
        "(Ljava/lang/Object;Lorg/theko/sound/AudioFlow;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

    // Device ID
    jstring jLink = NULL;
    LPWSTR deviceId = NULL;
    hr = pDevice->lpVtbl->GetId(pDevice, &deviceId);
    if (SUCCEEDED(hr) && deviceId) {
        int l = WideCharToMultiByte(CP_UTF8, 0, deviceId, -1, NULL, 0, NULL, NULL);
        char *buf = malloc(l);
        WideCharToMultiByte(CP_UTF8, 0, deviceId, -1, buf, l, NULL, NULL);
        jLink = (*env)->NewStringUTF(env, buf);
        free(buf);
        CoTaskMemFree(deviceId);
    }

    // Friendly name
    jstring jName = tryGetProperty(env, pProps, &PKEY_Device_FriendlyName, "Unknown");

    // Optional fields
    jstring jVendor = tryGetProperty(env, pProps, &PKEY_PNPX_Manufacturer, "Unknown Vendor");
    jstring jVersion = tryGetProperty(env, pProps, &PKEY_DeviceInterface_ClassGuid, "Unknown Version");
    jstring jDesc = tryGetProperty(env, pProps, &PKEY_Device_DeviceDesc, "Unknown Description");

    pProps->lpVtbl->Release(pProps);

    return (*env)->NewObject(env, clsAudioPort, ctor,
        jLink, flow, jName, jVendor, jVersion, jDesc);
}

static IMMDevice* AudioPort_to_IMMDevice(JNIEnv *env, jobject jAudioPort) {
    if (!jAudioPort) return NULL;

    jclass audioPortClass = (*env)->GetObjectClass(env, jAudioPort);
    jmethodID getLink = (*env)->GetMethodID(env, audioPortClass, "getLink", "()Ljava/lang/Object;");
    jstring jLinkStr = (jstring)(*env)->CallObjectMethod(env, jAudioPort, getLink);
    if (!jLinkStr) return NULL;

    // Convert Java string to wchar*
    const char *linkUtf = (*env)->GetStringUTFChars(env, jLinkStr, NULL);
    int wlen = MultiByteToWideChar(CP_UTF8, 0, linkUtf, -1, NULL, 0);
    wchar_t *wstr = (wchar_t*)malloc(wlen * sizeof(wchar_t));
    MultiByteToWideChar(CP_UTF8, 0, linkUtf, -1, wstr, wlen);
    (*env)->ReleaseStringUTFChars(env, jLinkStr, linkUtf);

    IMMDeviceEnumerator *pEnum = getDeviceEnumerator();
    if (!pEnum) {
        free(wstr);
        throwAudioBackendException(env, "Failed to get device enumerator.");
        return NULL;
    }

    IMMDevice *pDevice = NULL;
    HRESULT hr = pEnum->lpVtbl->GetDevice(pEnum, wstr, &pDevice);
    free(wstr);
    pEnum->lpVtbl->Release(pEnum);

    if (FAILED(hr)) {
        throwAudioBackendExceptionWithErrno(env, "Failed to get device.", hr);
        return NULL;
    }

    return pDevice;
}

static jobject WAVEFORMATEX_to_AudioFormat(JNIEnv *env, const WAVEFORMATEX* format) {
    if (!format) return NULL;

    debug(__FUNCTION__, "converting WAVEFORMATEX to AudioFormat");
    debug(__FUNCTION__, "getting classes");
    jclass audioFormatClass = (*env)->FindClass(env, "org/theko/sound/AudioFormat");
    jclass encodingClass    = (*env)->FindClass(env, "org/theko/sound/AudioFormat$Encoding");

    // Detect encoding
    jobject encoding = NULL;
    switch (format->wFormatTag) {
        case WAVE_FORMAT_IEEE_FLOAT:
            debug(__FUNCTION__, "IEEE_FLOAT");
            encoding = (*env)->GetStaticObjectField(env, encodingClass, (*env)->GetStaticFieldID(env, encodingClass, "PCM_FLOAT", "Lorg/theko/sound/AudioFormat$Encoding;"));
            break;
        case WAVE_FORMAT_PCM:
            debug(__FUNCTION__, "PCM");
            if (format->wBitsPerSample == 8)
                encoding = (*env)->GetStaticObjectField(env, encodingClass, (*env)->GetStaticFieldID(env, encodingClass, "PCM_UNSIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
            else
                encoding = (*env)->GetStaticObjectField(env, encodingClass, (*env)->GetStaticFieldID(env, encodingClass, "PCM_SIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
            break;
        default:
            debug(__FUNCTION__, "unsupported encoding");
            char msg[128];
            snprintf(msg, sizeof(msg), "Unsupported WAVEFORMATEX encoding. Encoding: 0x%04X", format->wFormatTag);
            throwAudioBackendException(env, msg);
            return NULL;
    }

    debug(__FUNCTION__, "creating AudioFormat");
    // Constructor AudioFormat(int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian, int frameSize, int byteRate)
    jmethodID ctor = (*env)->GetMethodID(env, audioFormatClass, "<init>",
        "(IIILorg/theko/sound/AudioFormat$Encoding;ZII)V");

    // Little-endian
    jboolean bigEndian = JNI_FALSE;

    debug(__FUNCTION__, "creating AudioFormat object");
    return (*env)->NewObject(env, audioFormatClass, ctor,
        (jint)format->nSamplesPerSec,
        (jint)format->wBitsPerSample,
        (jint)format->nChannels,
        encoding,
        bigEndian,
        (jint)format->nBlockAlign,
        (jint)format->nAvgBytesPerSec
    );
}

static jobject WAVEFORMATEXTENSIBLE_to_AudioFormat(JNIEnv *env, const WAVEFORMATEXTENSIBLE* format) {
    if (!format) return NULL;
    debug(__FUNCTION__, "converting WAVEFORMATEXTENSIBLE to AudioFormat");
    return WAVEFORMATEX_to_AudioFormat(env, &format->Format);
}

static WAVEFORMATEX* AudioFormat_to_WAVEFORMATEX(JNIEnv *env, jobject jFormat) {
    if (!jFormat) return NULL;

    jclass cls = (*env)->GetObjectClass(env, jFormat);
    jclass encodingClass = (*env)->FindClass(env, "org/theko/sound/AudioFormat$Encoding");
    if (!cls || !encodingClass) return NULL;

    jmethodID mid_getSampleRate = (*env)->GetMethodID(env, cls, "getSampleRate", "()I");
    jmethodID mid_getBits = (*env)->GetMethodID(env, cls, "getSampleSizeInBits", "()I");
    jmethodID mid_getChannels = (*env)->GetMethodID(env, cls, "getChannels", "()I");
    jmethodID mid_getByteRate = (*env)->GetMethodID(env, cls, "getByteRate", "()I");
    jmethodID mid_getFrameSize = (*env)->GetMethodID(env, cls, "getFrameSize", "()I");
    jmethodID mid_getEncoding = (*env)->GetMethodID(env, cls, "getEncoding", "()Lorg/theko/sound/AudioFormat$Encoding;");
    jmethodID mid_isBigEndian = (*env)->GetMethodID(env, cls, "isBigEndian", "()Z");

    if (!mid_getSampleRate || !mid_getBits || !mid_getChannels || !mid_getByteRate || !mid_getFrameSize || !mid_getEncoding || !mid_isBigEndian)
        return NULL;

    jint sampleRate    = (*env)->CallIntMethod(env, jFormat, mid_getSampleRate);
    jint bitsPerSample = (*env)->CallIntMethod(env, jFormat, mid_getBits);
    jint channels      = (*env)->CallIntMethod(env, jFormat, mid_getChannels);
    jint byteRate      = (*env)->CallIntMethod(env, jFormat, mid_getByteRate);
    jint frameSize     = (*env)->CallIntMethod(env, jFormat, mid_getFrameSize);
    jboolean bigEndian = (*env)->CallBooleanMethod(env, jFormat, mid_isBigEndian);
    jobject encoding   = (*env)->CallObjectMethod(env, jFormat, mid_getEncoding);
    if (!encoding) return NULL;

    if (bigEndian) {
        throwAudioBackendException(env, "Big-endian WAVEFORMATEX is not supported.");
        return NULL;
    }

    // Detect format tag
    jfieldID fid_float = (*env)->GetStaticFieldID(env, encodingClass, "PCM_FLOAT", "Lorg/theko/sound/AudioFormat$Encoding;");
    jfieldID fid_signed = (*env)->GetStaticFieldID(env, encodingClass, "PCM_SIGNED", "Lorg/theko/sound/AudioFormat$Encoding;");
    jfieldID fid_unsigned = (*env)->GetStaticFieldID(env, encodingClass, "PCM_UNSIGNED", "Lorg/theko/sound/AudioFormat$Encoding;");
    if (!fid_float || !fid_signed || !fid_unsigned) return NULL;

    WORD formatTag;
    if ((*env)->IsSameObject(env, encoding, (*env)->GetStaticObjectField(env, encodingClass, fid_float))) {
        formatTag = WAVE_FORMAT_IEEE_FLOAT;
    } else if ((*env)->IsSameObject(env, encoding, (*env)->GetStaticObjectField(env, encodingClass, fid_signed)) ||
               (*env)->IsSameObject(env, encoding, (*env)->GetStaticObjectField(env, encodingClass, fid_unsigned))) {
        formatTag = WAVE_FORMAT_PCM;
    } else {
        throwAudioBackendException(env, "Unsupported WAVEFORMATEX encoding.");
        return NULL;
    }

    if (frameSize <= 0)
        frameSize = channels * ((bitsPerSample + 7) / 8);
    if (byteRate <= 0)
        byteRate = sampleRate * frameSize;

    WAVEFORMATEX* wf = (WAVEFORMATEX*)calloc(1, sizeof(WAVEFORMATEX));
    if (!wf) return NULL;

    wf->wFormatTag      = formatTag;
    wf->nSamplesPerSec  = sampleRate;
    wf->wBitsPerSample  = (WORD)bitsPerSample;
    wf->nChannels       = (WORD)channels;
    wf->nBlockAlign     = (WORD)frameSize;
    wf->nAvgBytesPerSec = byteRate;
    wf->cbSize          = 0;

    return wf;
}

#endif // WASAPI_BACKEND_H