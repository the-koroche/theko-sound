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

#include <IJavaClassCache.hpp>
#include <jni.h>
#include <jni_util.hpp>

class AudioFormatEncodingCache : IJavaClassCache {
public:
    jclass clazz;
    jfieldID pcmUnsigned;
    jobject pcmUnsignedObj;
    jfieldID pcmSigned;
    jobject pcmSignedObj;
    jfieldID pcmFloat;
    jobject pcmFloatObj;
    jfieldID ulaw;
    jobject ulawObj;
    jfieldID alaw;
    jobject alawObj;

    AudioFormatEncodingCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env->FindClass("org/theko/sound/AudioFormat$Encoding"));
        pcmUnsigned = JNI_TRY_RETURN(env->GetStaticFieldID(clazz, "PCM_UNSIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmUnsignedObj = JNI_TRY_RETURN(env->GetStaticObjectField(clazz, pcmUnsigned));
        pcmSigned = JNI_TRY_RETURN(env->GetStaticFieldID(clazz, "PCM_SIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmSignedObj = JNI_TRY_RETURN(env->GetStaticObjectField(clazz, pcmSigned));
        pcmFloat = JNI_TRY_RETURN(env->GetStaticFieldID(clazz, "PCM_FLOAT", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmFloatObj = JNI_TRY_RETURN(env->GetStaticObjectField(clazz, pcmFloat));
        ulaw = JNI_TRY_RETURN(env->GetStaticFieldID(clazz, "ULAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        ulawObj = JNI_TRY_RETURN(env->GetStaticObjectField(clazz, ulaw));
        alaw = JNI_TRY_RETURN(env->GetStaticFieldID(clazz, "ALAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        alawObj = JNI_TRY_RETURN(env->GetStaticObjectField(clazz, alaw));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFormat$Encoding failed to initialize");
            return;
        }

        clazz = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(clazz));
        pcmUnsignedObj = (jobject) JNI_TRY_RETURN(env->NewGlobalRef(pcmUnsignedObj));
        pcmSignedObj = (jobject) JNI_TRY_RETURN(env->NewGlobalRef(pcmSignedObj));
        pcmFloatObj = (jobject) JNI_TRY_RETURN(env->NewGlobalRef(pcmFloatObj));
        ulawObj = (jobject) JNI_TRY_RETURN(env->NewGlobalRef(ulawObj));
        alawObj = (jobject) JNI_TRY_RETURN(env->NewGlobalRef(alawObj));
    }

    bool isValid() const override {
        return clazz && pcmUnsigned && pcmUnsignedObj && pcmSigned && pcmSignedObj &&
                pcmFloat && pcmFloatObj && ulaw && ulawObj && alaw && alawObj;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(clazz);
        JNI_RELEASE_GLOBAL(pcmUnsignedObj);
        JNI_RELEASE_GLOBAL(pcmSignedObj);
        JNI_RELEASE_GLOBAL(pcmFloatObj);
        JNI_RELEASE_GLOBAL(ulawObj);
        JNI_RELEASE_GLOBAL(alawObj);
    }

    AUTO_STATIC_CACHE_GET(AudioFormatEncodingCache)
};