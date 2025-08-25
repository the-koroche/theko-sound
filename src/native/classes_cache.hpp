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

#include <jni.h>

static bool checkJavaEnvException(JNIEnv* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

#define JCHECK_RET(expr) ([&]() { \
    auto _res = (expr); \
    if (checkJavaEnvException(env)) return decltype(_res)(); \
    return _res; \
}())

#define JCHECK(expr) do { \
    (expr); \
    checkJavaEnvException(env); \
} while (0)

class AudioFlowCache {
public:
    jclass clazz;
    jfieldID out;
    jfieldID in;
    jobject outObj;
    jobject inObj;

    AudioFlowCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/AudioFlow"));
        out = JCHECK_RET(env->GetStaticFieldID(clazz, "OUT", "Lorg/theko/sound/AudioFlow;"));
        outObj = JCHECK_RET(env->GetStaticObjectField(clazz, out));
        in = JCHECK_RET(env->GetStaticFieldID(clazz, "IN", "Lorg/theko/sound/AudioFlow;"));
        inObj = JCHECK_RET(env->GetStaticObjectField(clazz, in));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFlow failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
        outObj = (jobject) JCHECK_RET(env->NewGlobalRef(outObj));
        inObj = (jobject) JCHECK_RET(env->NewGlobalRef(inObj));
    }

    bool isValid() {
        return clazz && out && outObj && in && inObj;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
        if (outObj) {
            JCHECK(env->DeleteGlobalRef(outObj));
            outObj = nullptr;
        }
        if (inObj) {
            JCHECK(env->DeleteGlobalRef(inObj));
            inObj = nullptr;
        }
    }
};

class AudioFormatEncodingCache {
public:
    jclass clazz;
    jfieldID pcmUnsigned;
    jfieldID pcmSigned;
    jfieldID pcmFloat;
    jfieldID ulaw;
    jfieldID alaw;
    jobject pcmUnsignedObj;
    jobject pcmSignedObj;
    jobject pcmFloatObj;
    jobject ulawObj;
    jobject alawObj;

    AudioFormatEncodingCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/AudioFormat$Encoding"));
        pcmUnsigned = JCHECK_RET(env->GetStaticFieldID(clazz, "PCM_UNSIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmUnsignedObj = JCHECK_RET(env->GetStaticObjectField(clazz, pcmUnsigned));
        pcmSigned = JCHECK_RET(env->GetStaticFieldID(clazz, "PCM_SIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmSignedObj = JCHECK_RET(env->GetStaticObjectField(clazz, pcmSigned));
        pcmFloat = JCHECK_RET(env->GetStaticFieldID(clazz, "PCM_FLOAT", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmFloatObj = JCHECK_RET(env->GetStaticObjectField(clazz, pcmFloat));
        ulaw = JCHECK_RET(env->GetStaticFieldID(clazz, "ULAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        ulawObj = JCHECK_RET(env->GetStaticObjectField(clazz, ulaw));
        alaw = JCHECK_RET(env->GetStaticFieldID(clazz, "ALAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        alawObj = JCHECK_RET(env->GetStaticObjectField(clazz, alaw));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFormat$Encoding failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
        pcmUnsignedObj = (jobject) JCHECK_RET(env->NewGlobalRef(pcmUnsignedObj));
        pcmSignedObj = (jobject) JCHECK_RET(env->NewGlobalRef(pcmSignedObj));
        pcmFloatObj = (jobject) JCHECK_RET(env->NewGlobalRef(pcmFloatObj));
        ulawObj = (jobject) JCHECK_RET(env->NewGlobalRef(ulawObj));
        alawObj = (jobject) JCHECK_RET(env->NewGlobalRef(alawObj));
    }

    bool isValid() {
        return clazz && pcmUnsigned && pcmUnsignedObj && 
        pcmSigned && pcmSignedObj && 
        pcmFloat && pcmFloatObj && ulaw && ulawObj &&
        alaw && alawObj;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
        if (pcmUnsignedObj) {
            JCHECK(env->DeleteGlobalRef(pcmUnsignedObj));
            pcmUnsignedObj = nullptr;
        }
        if (pcmSignedObj) {
            JCHECK(env->DeleteGlobalRef(pcmSignedObj));
            pcmSignedObj = nullptr;
        }
        if (pcmFloatObj) {
            JCHECK(env->DeleteGlobalRef(pcmFloatObj));
            pcmFloatObj = nullptr;
        }
        if (ulawObj) {
            JCHECK(env->DeleteGlobalRef(ulawObj));
            ulawObj = nullptr;
        }
        if (alawObj) {
            JCHECK(env->DeleteGlobalRef(alawObj));
            alawObj = nullptr;
        }
    }
};

class AudioFormatCache {
public:
    jclass clazz;
    jmethodID ctor;
    jmethodID getSampleRate;
    jmethodID getBitsPerSample;
    jmethodID getBytesPerSample;
    jmethodID getChannels;
    jmethodID getEncoding;
    jmethodID isBigEndian;
    jmethodID getFrameSize;
    jmethodID getByteRate;
    
    AudioFormatCache (JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/AudioFormat"));
        ctor = JCHECK_RET(env->GetMethodID(clazz, "<init>", "(IIILorg/theko/sound/AudioFormat$Encoding;Z)V"));
        getSampleRate = JCHECK_RET(env->GetMethodID(clazz, "getSampleRate", "()I"));
        getBitsPerSample = JCHECK_RET(env->GetMethodID(clazz, "getBitsPerSample", "()I"));
        getBytesPerSample = JCHECK_RET(env->GetMethodID(clazz, "getBytesPerSample", "()I"));
        getChannels = JCHECK_RET(env->GetMethodID(clazz, "getChannels", "()I"));
        getEncoding = JCHECK_RET(env->GetMethodID(clazz, "getEncoding", "()Lorg/theko/sound/AudioFormat$Encoding;"));
        isBigEndian = JCHECK_RET(env->GetMethodID(clazz, "isBigEndian", "()Z"));
        getFrameSize = JCHECK_RET(env->GetMethodID(clazz, "getFrameSize", "()I"));
        getByteRate = JCHECK_RET(env->GetMethodID(clazz, "getByteRate", "()I"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFormat failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
    }

    bool isValid() {
        return clazz && ctor && getSampleRate && getBitsPerSample &&
        getBytesPerSample && getChannels && getEncoding && isBigEndian &&
        getFrameSize && getByteRate;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
    }
};

class AudioPortCache {
public:
    jclass clazz;
    jmethodID ctor;
    jmethodID getLink;
    jmethodID getFlow;
    jmethodID isActive;
    jmethodID getMixFormat;
    jmethodID getName;
    jmethodID getVendor;
    jmethodID getVersion;
    jmethodID getDescription;

    AudioPortCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/AudioPort"));
        ctor = JCHECK_RET(env->GetMethodID(clazz, "<init>", "(Ljava/lang/Object;Lorg/theko/sound/AudioFlow;ZLorg/theko/sound/AudioFormat;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
        getLink = JCHECK_RET(env->GetMethodID(clazz, "getLink", "()Ljava/lang/Object;"));
        getFlow = JCHECK_RET(env->GetMethodID(clazz, "getFlow", "()Lorg/theko/sound/AudioFlow;"));
        isActive = JCHECK_RET(env->GetMethodID(clazz, "isActive", "()Z"));
        getMixFormat = JCHECK_RET(env->GetMethodID(clazz, "getMixFormat", "()Lorg/theko/sound/AudioFormat;"));
        getName = JCHECK_RET(env->GetMethodID(clazz, "getName", "()Ljava/lang/String;"));
        getVendor = JCHECK_RET(env->GetMethodID(clazz, "getVendor", "()Ljava/lang/String;"));
        getVersion = JCHECK_RET(env->GetMethodID(clazz, "getVersion", "()Ljava/lang/String;"));
        getDescription = JCHECK_RET(env->GetMethodID(clazz, "getDescription", "()Ljava/lang/String;"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioPort failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
    }

    bool isValid() {
        return clazz && ctor && 
            getLink && getFlow && isActive &&
            getMixFormat && getName && getVendor &&
            getVersion && getDescription;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
    }
};

class WASAPIBackendCache {
public:
    jclass clazz;
    jfieldID backendContextPtr;

    WASAPIBackendCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/backend/wasapi/WASAPISharedBackend"));
        backendContextPtr = JCHECK_RET(env->GetFieldID(clazz, "backendContextPtr", "J"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "WASAPIBackend failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
    }

    bool isValid() {
        return clazz && backendContextPtr;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
    }
};

class WASAPIOutputCache {
public:
    jclass clazz;
    jfieldID outputContextPtr;

    WASAPIOutputCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/backend/wasapi/WASAPISharedOutput"));
        outputContextPtr = JCHECK_RET(env->GetFieldID(clazz, "outputContextPtr", "J"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "WASAPIOutput failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
    }

    bool isValid() {
        return clazz && outputContextPtr;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
    }
};

class WASAPINativeAudioPortHandleCache {
public:
    jclass clazz;
    jmethodID ctor;
    jmethodID getHandle;

    WASAPINativeAudioPortHandleCache(JNIEnv* env) {
        clazz = JCHECK_RET(env->FindClass("org/theko/sound/backend/wasapi/WASAPINativeAudioPortHandle"));
        ctor = JCHECK_RET(env->GetMethodID(clazz, "<init>", "(Ljava/lang/String;)V"));
        getHandle = JCHECK_RET(env->GetMethodID(clazz, "getHandle", "()Ljava/lang/String;"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "WASAPINativeAudioPortHandle failed to initialize");
            return;
        }

        clazz = (jclass) JCHECK_RET(env->NewGlobalRef(clazz));
    }

    bool isValid() {
        return clazz && ctor && getHandle;
    }

    void release(JNIEnv* env) {
        if (clazz) {
            JCHECK(env->DeleteGlobalRef(clazz));
            clazz = nullptr;
        }
    }
};

class AtomicReferenceCache {
public:
    jclass atomicReference;
    jmethodID ctor;
    jmethodID get;
    jmethodID set;

    AtomicReferenceCache(JNIEnv* env) {
        atomicReference = JCHECK_RET(env->FindClass("java/util/concurrent/atomic/AtomicReference"));
        ctor = JCHECK_RET(env->GetMethodID(atomicReference, "<init>", "()V"));
        get = JCHECK_RET(env->GetMethodID(atomicReference, "get", "()Ljava/lang/Object;"));
        set = JCHECK_RET(env->GetMethodID(atomicReference, "set", "(Ljava/lang/Object;)V"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AtomicReference failed to initialize");
            return;
        }

        atomicReference = (jclass) JCHECK_RET(env->NewGlobalRef(atomicReference));
    }

    bool isValid() {
        return atomicReference && ctor && get && set;
    }

    void release(JNIEnv* env) {
        if (atomicReference) {
            JCHECK(env->DeleteGlobalRef(atomicReference));
            atomicReference = nullptr;
        }
    }
};

class JavaExceptions {
public:
    jclass runtimeException;
    jclass outOfMemoryException;
    jclass illegalArgumentException;
    jclass unsupportedOperationException;

    JavaExceptions(JNIEnv* env) {
        runtimeException = JCHECK_RET(env->FindClass("java/lang/RuntimeException"));
        outOfMemoryException = JCHECK_RET(env->FindClass("java/lang/OutOfMemoryError"));
        illegalArgumentException = JCHECK_RET(env->FindClass("java/lang/IllegalArgumentException"));
        unsupportedOperationException = JCHECK_RET(env->FindClass("java/lang/UnsupportedOperationException"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "JavaExceptions failed to initialize");
            return;
        }

        runtimeException = (jclass) JCHECK_RET(env->NewGlobalRef(runtimeException));
        outOfMemoryException = (jclass) JCHECK_RET(env->NewGlobalRef(outOfMemoryException));
        illegalArgumentException = (jclass) JCHECK_RET(env->NewGlobalRef(illegalArgumentException));
        unsupportedOperationException = (jclass) JCHECK_RET(env->NewGlobalRef(unsupportedOperationException));
    }

    bool isValid() {
        return runtimeException && outOfMemoryException && illegalArgumentException && unsupportedOperationException;
    }

    void release(JNIEnv* env) {
        if (runtimeException) {
            JCHECK(env->DeleteGlobalRef(runtimeException));
            runtimeException = nullptr;
        }
        if (outOfMemoryException) {
            JCHECK(env->DeleteGlobalRef(outOfMemoryException));
            outOfMemoryException = nullptr;
        }
        if (illegalArgumentException) {
            JCHECK(env->DeleteGlobalRef(illegalArgumentException));
            illegalArgumentException = nullptr;
        }
        if (unsupportedOperationException) {
            JCHECK(env->DeleteGlobalRef(unsupportedOperationException));
            unsupportedOperationException = nullptr;
        }
    }
};

class ThekoSoundExceptions {
public:
    jclass audioBackendException;
    jclass unsupportedAudioFormatException;
    jclass unsupportedAudioEncodingException;

    ThekoSoundExceptions(JNIEnv* env) {
        audioBackendException = JCHECK_RET(env->FindClass("org/theko/sound/backend/AudioBackendException"));
        unsupportedAudioFormatException = JCHECK_RET(env->FindClass("org/theko/sound/UnsupportedAudioFormatException"));
        unsupportedAudioEncodingException = JCHECK_RET(env->FindClass("org/theko/sound/UnsupportedAudioEncodingException"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "ThekoSoundExceptions failed to initialize");
            return;
        }

        audioBackendException = (jclass) JCHECK_RET(env->NewGlobalRef(audioBackendException));
        unsupportedAudioFormatException = (jclass) JCHECK_RET(env->NewGlobalRef(unsupportedAudioFormatException));
        unsupportedAudioEncodingException = (jclass) JCHECK_RET(env->NewGlobalRef(unsupportedAudioEncodingException));
    }

    bool isValid() {
        return audioBackendException && unsupportedAudioFormatException && unsupportedAudioEncodingException;
    }

    void release(JNIEnv* env) {
        if (audioBackendException) {
            JCHECK(env->DeleteGlobalRef(audioBackendException));
            audioBackendException = nullptr;
        }
        if (unsupportedAudioFormatException) {
            JCHECK(env->DeleteGlobalRef(unsupportedAudioFormatException));
            unsupportedAudioFormatException = nullptr;
        }
        if (unsupportedAudioEncodingException) {
            JCHECK(env->DeleteGlobalRef(unsupportedAudioEncodingException));
            unsupportedAudioEncodingException = nullptr;
        }
    }
};

class ClassesCache {
public:
    AudioFlowCache* audioFlow;
    AudioPortCache* audioPort;
    AudioFormatEncodingCache* audioFormatEncoding;
    AudioFormatCache* audioFormat;
    WASAPIBackendCache* wasapiBackend;
    WASAPIOutputCache* wasapiOutput;
    WASAPINativeAudioPortHandleCache* wasapiNativeAudioPortHandle;
    AtomicReferenceCache* atomicReference;
    JavaExceptions* javaExceptions;
    ThekoSoundExceptions* thekoSoundExceptions;

    ClassesCache(JNIEnv* env) {
        audioFlow = new AudioFlowCache(env);
        audioPort = new AudioPortCache(env);
        audioFormatEncoding = new AudioFormatEncodingCache(env);
        audioFormat = new AudioFormatCache(env);
        wasapiBackend = new WASAPIBackendCache(env);    
        wasapiOutput = new WASAPIOutputCache(env);
        wasapiNativeAudioPortHandle = new WASAPINativeAudioPortHandleCache(env);
        atomicReference = new AtomicReferenceCache(env);
        javaExceptions = new JavaExceptions(env);
        thekoSoundExceptions = new ThekoSoundExceptions(env);
    }

    void release(JNIEnv* env) {
        audioFlow->release(env);
        audioPort->release(env);
        audioFormatEncoding->release(env);
        audioFormat->release(env);
        wasapiBackend->release(env);
        wasapiOutput->release(env);
        wasapiNativeAudioPortHandle->release(env);
        atomicReference->release(env);
        javaExceptions->release(env);
        thekoSoundExceptions->release(env);
    }

    ~ClassesCache() {
        fprintf(stderr, "[ClassesCache] Destructor cannot release resources: no JNIEnv available in native destructor. Call release(env) manually.\n");
    }
};

static ClassesCache* getClassesCache(JNIEnv* env) {
    static ClassesCache* classesCache = new ClassesCache(env);
    return classesCache;
}