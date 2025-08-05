#pragma once

#include <jni.h>

static jclass audioPortClazz;
static jclass audioFlowClazz;
static jclass audioFormatClazz;
static jclass audioFormatEncodingClazz;
static jclass wasapiNativeHandleClazz;

static jmethodID audioPortCtor;
static jmethodID audioFormatCtor;
static jmethodID wasapiNativeHandleCtor;

static jfieldID audioFlowOut;
static jfieldID audioFlowIn;
static jobject audioFlowOutObj;
static jobject audioFlowInObj;

static jfieldID audioFormatEncodingPcmUnsigned;
static jfieldID audioFormatEncodingPcmSigned;
static jfieldID audioFormatEncodingFloat;
static jobject audioFormatEncodingPcmUnsignedObj;
static jobject audioFormatEncodingPcmSignedObj;
static jobject audioFormatEncodingFloatObj;

static jmethodID audioFormatGetSampleRate;
static jmethodID audioFormatGetSampleSizeInBits;
static jmethodID audioFormatGetChannels;
static jmethodID audioFormatGetByteRate;
static jmethodID audioFormatGetFrameSize;
static jmethodID audioFormatGetEncoding;
static jmethodID audioFormatIsBigEndian;

static jmethodID wasapiNativeHandleGetHandle;

static jmethodID audioPortGetLink;
static jmethodID audioPortGetFlow;
static jmethodID audioPortGetName;
static jmethodID audioPortGetVendor;
static jmethodID audioPortGetVersion;
static jmethodID audioPortGetDescription;

static jclass javaLangOutOfMemoryException;
static jclass javaLangIllegalArgumentException;
static jclass javaLangUnsupportedOperationException;
static jclass javaLangRuntimeException;
static jclass orgThekoSoundBackendAudioBackendException;
static jclass orgThekoSoundUnsupportedAudioFormatException;
static jclass orgThekoSoundUnsupportedAudioEncodingException;

static jclass orgThekoSoundBackendWasapiWASAPISharedBackendClazz;
static jfieldID WASAPISharedBackendHandle;

static bool checkJavaException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe(); 
        env->ExceptionClear();
        return true;
    }
    return false;
}

static bool createCache(JNIEnv *env) {
    audioPortClazz = env->FindClass("org/theko/sound/AudioPort");
    audioFlowClazz = env->FindClass("org/theko/sound/AudioFlow");
    audioFormatClazz = env->FindClass("org/theko/sound/AudioFormat");
    audioFormatEncodingClazz = env->FindClass("org/theko/sound/AudioFormat$Encoding");
    wasapiNativeHandleClazz = env->FindClass("org/theko/sound/backend/wasapi/WASAPINativeAudioPortHandle");
    orgThekoSoundBackendWasapiWASAPISharedBackendClazz = env->FindClass("org/theko/sound/backend/wasapi/WASAPISharedBackend");

    if (
        !audioPortClazz ||
        !audioFlowClazz || 
        !audioFormatClazz || 
        !audioFormatEncodingClazz || 
        !wasapiNativeHandleClazz || 
        !orgThekoSoundBackendWasapiWASAPISharedBackendClazz
    ) return false;

    audioPortCtor = env->GetMethodID(
        audioPortClazz,
        "<init>",
        "(Ljava/lang/Object;Lorg/theko/sound/AudioFlow;ZLorg/theko/sound/AudioFormat;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"
    );
    audioFormatCtor = env->GetMethodID(
        audioFormatClazz,
        "<init>",
        "(IIILorg/theko/sound/AudioFormat$Encoding;Z)V"
    );
    wasapiNativeHandleCtor = env->GetMethodID(
        wasapiNativeHandleClazz,
        "<init>",
        "(Ljava/lang/String;)V"
    );

    if (
        !audioPortCtor ||
        !audioFormatCtor ||
        !wasapiNativeHandleCtor
    ) return false;

    audioFlowOut = env->GetStaticFieldID(audioFlowClazz, "OUT", "Lorg/theko/sound/AudioFlow;");
    audioFlowIn = env->GetStaticFieldID(audioFlowClazz, "IN", "Lorg/theko/sound/AudioFlow;");

    if (!audioFlowOut || !audioFlowIn) return false;

    audioFlowOutObj = env->GetStaticObjectField(audioFlowClazz, audioFlowOut);
    audioFlowInObj = env->GetStaticObjectField(audioFlowClazz, audioFlowIn);

    if (!audioFlowOutObj || !audioFlowInObj) return false;

    audioFormatEncodingPcmUnsigned = env->GetStaticFieldID(
        audioFormatEncodingClazz,
        "PCM_UNSIGNED",
        "Lorg/theko/sound/AudioFormat$Encoding;"
    );
    audioFormatEncodingPcmSigned = env->GetStaticFieldID(
        audioFormatEncodingClazz,
        "PCM_SIGNED",
        "Lorg/theko/sound/AudioFormat$Encoding;"
    );
    audioFormatEncodingFloat = env->GetStaticFieldID(
        audioFormatEncodingClazz,
        "PCM_FLOAT",
        "Lorg/theko/sound/AudioFormat$Encoding;");

    if (!audioFlowOut || !audioFlowIn || !audioFormatEncodingPcmUnsigned || !audioFormatEncodingPcmSigned || !audioFormatEncodingFloat) return false;

    audioFormatEncodingPcmUnsignedObj = env->GetStaticObjectField(audioFormatEncodingClazz, audioFormatEncodingPcmUnsigned);
    audioFormatEncodingPcmSignedObj = env->GetStaticObjectField(audioFormatEncodingClazz, audioFormatEncodingPcmSigned);
    audioFormatEncodingFloatObj = env->GetStaticObjectField(audioFormatEncodingClazz, audioFormatEncodingFloat);

    audioFormatGetSampleRate = env->GetMethodID(audioFormatClazz, "getSampleRate", "()I");
    audioFormatGetSampleSizeInBits = env->GetMethodID(audioFormatClazz, "getSampleSizeInBits", "()I");
    audioFormatGetChannels = env->GetMethodID(audioFormatClazz, "getChannels", "()I");
    audioFormatGetByteRate = env->GetMethodID(audioFormatClazz, "getByteRate", "()I");
    audioFormatGetFrameSize = env->GetMethodID(audioFormatClazz, "getFrameSize", "()I");
    audioFormatGetEncoding = env->GetMethodID(audioFormatClazz, "getEncoding", "()Lorg/theko/sound/AudioFormat$Encoding;");
    audioFormatIsBigEndian = env->GetMethodID(audioFormatClazz, "isBigEndian", "()Z");

    wasapiNativeHandleGetHandle = env->GetMethodID(wasapiNativeHandleClazz, "getHandle", "()Ljava/lang/String;");

    audioPortGetLink = env->GetMethodID(audioPortClazz, "getLink", "()Ljava/lang/Object;");
    audioPortGetFlow = env->GetMethodID(audioPortClazz, "getFlow", "()Lorg/theko/sound/AudioFlow;");
    audioPortGetName = env->GetMethodID(audioPortClazz, "getName", "()Ljava/lang/String;");
    audioPortGetVendor = env->GetMethodID(audioPortClazz, "getVendor", "()Ljava/lang/String;");
    audioPortGetVersion = env->GetMethodID(audioPortClazz, "getVersion", "()Ljava/lang/String;");
    audioPortGetDescription = env->GetMethodID(audioPortClazz, "getDescription", "()Ljava/lang/String;");

    if (
        !audioPortGetLink ||
        !audioPortGetFlow || 
        !audioPortGetName || 
        !audioPortGetVendor || 
        !audioPortGetVersion || 
        !audioPortGetDescription
    ) return false;

    WASAPISharedBackendHandle = env->GetFieldID(
        orgThekoSoundBackendWasapiWASAPISharedBackendClazz,
        "handle", "J"
    );

    if (!WASAPISharedBackendHandle) return false;

    // Exceptions
    javaLangRuntimeException = env->FindClass(
        "java/lang/RuntimeException");

    javaLangOutOfMemoryException = env->FindClass(
        "java/lang/OutOfMemoryError");

    javaLangIllegalArgumentException = env->FindClass(
        "java/lang/IllegalArgumentException");

    javaLangUnsupportedOperationException = env->FindClass(
        "java/lang/UnsupportedOperationException");

    orgThekoSoundBackendAudioBackendException = 
        env->FindClass("org/theko/sound/backend/AudioBackendException");

    orgThekoSoundUnsupportedAudioFormatException = 
        env->FindClass("org/theko/sound/UnsupportedAudioFormatException");

    orgThekoSoundUnsupportedAudioEncodingException = 
        env->FindClass("org/theko/sound/UnsupportedAudioEncodingException");

    if (
        !javaLangRuntimeException ||
        !javaLangOutOfMemoryException ||
        !javaLangIllegalArgumentException ||
        !javaLangUnsupportedOperationException ||
        !orgThekoSoundBackendAudioBackendException ||
        !orgThekoSoundUnsupportedAudioFormatException ||
        !orgThekoSoundUnsupportedAudioEncodingException
    ) return false;

    // Add GlobalRef
    audioPortClazz = (jclass) env->NewGlobalRef(audioPortClazz);
    audioFlowClazz = (jclass) env->NewGlobalRef(audioFlowClazz);
    audioFormatClazz = (jclass) env->NewGlobalRef(audioFormatClazz);
    audioFormatEncodingClazz = (jclass) env->NewGlobalRef(
        audioFormatEncodingClazz);

    wasapiNativeHandleClazz = (jclass) env->NewGlobalRef(
        wasapiNativeHandleClazz);

    audioFlowOutObj = (jobject) env->NewGlobalRef(audioFlowOutObj);
    audioFlowInObj = (jobject) env->NewGlobalRef(audioFlowInObj);

    audioFormatEncodingPcmUnsignedObj = (jobject) env->NewGlobalRef(audioFormatEncodingPcmUnsignedObj);
    audioFormatEncodingPcmSignedObj = (jobject) env->NewGlobalRef(audioFormatEncodingPcmSignedObj);
    audioFormatEncodingFloatObj = (jobject) env->NewGlobalRef(audioFormatEncodingFloatObj);
    
    // Exceptions

    orgThekoSoundBackendWasapiWASAPISharedBackendClazz =
        (jclass) env->NewGlobalRef(
            orgThekoSoundBackendWasapiWASAPISharedBackendClazz
        );

    javaLangRuntimeException = (jclass) env->NewGlobalRef(
        javaLangRuntimeException);

    javaLangOutOfMemoryException = (jclass) env->NewGlobalRef(
        javaLangOutOfMemoryException);

    javaLangIllegalArgumentException = (jclass) env->NewGlobalRef(
        javaLangIllegalArgumentException);
    
    javaLangUnsupportedOperationException = (jclass) env->NewGlobalRef(
        javaLangUnsupportedOperationException);

    orgThekoSoundBackendAudioBackendException = 
        (jclass) env->NewGlobalRef(
            orgThekoSoundBackendAudioBackendException
        );

    orgThekoSoundUnsupportedAudioFormatException = 
        (jclass) env->NewGlobalRef(
            orgThekoSoundUnsupportedAudioFormatException
        );

    orgThekoSoundUnsupportedAudioEncodingException = 
        (jclass) env->NewGlobalRef(
            orgThekoSoundUnsupportedAudioEncodingException
        );

    return true;
}

static bool releaseCache(JNIEnv *env) {
    env->DeleteGlobalRef(audioPortClazz);
    env->DeleteGlobalRef(audioFlowClazz);
    env->DeleteGlobalRef(audioFormatClazz);
    env->DeleteGlobalRef(audioFormatEncodingClazz);
    env->DeleteGlobalRef(wasapiNativeHandleClazz);

    env->DeleteGlobalRef(audioFlowOutObj);
    env->DeleteGlobalRef(audioFlowInObj);

    env->DeleteGlobalRef(audioFormatEncodingPcmUnsignedObj);
    env->DeleteGlobalRef(audioFormatEncodingPcmSignedObj);
    env->DeleteGlobalRef(audioFormatEncodingFloatObj);
    return true;
}