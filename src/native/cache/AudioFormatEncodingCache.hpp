/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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
#include "IJavaClassCache.hpp"
#include "JNI_Utility.hpp"

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
        clazz = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/AudioFormat$Encoding"));

        pcmUnsigned = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "PCM_UNSIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmUnsignedObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, pcmUnsigned));

        pcmSigned = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "PCM_SIGNED", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmSignedObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, pcmSigned));

        pcmFloat = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "PCM_FLOAT", "Lorg/theko/sound/AudioFormat$Encoding;"));
        pcmFloatObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, pcmFloat));

        ulaw = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "ULAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        ulawObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, ulaw));
        
        alaw = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "ALAW", "Lorg/theko/sound/AudioFormat$Encoding;"));
        alawObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, alaw));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFormat$Encoding failed to initialize");
            return;
        }

        clazz = (jclass) JNIUtil_CreateGlobal(env, clazz);
        pcmUnsignedObj = JNIUtil_CreateGlobal(env, pcmUnsignedObj);
        pcmSignedObj = JNIUtil_CreateGlobal(env, pcmSignedObj);
        pcmFloatObj = JNIUtil_CreateGlobal(env, pcmFloatObj);
        ulawObj = JNIUtil_CreateGlobal(env, ulawObj);
        alawObj = JNIUtil_CreateGlobal(env, alawObj);
    }

    bool isValid() const override {
        return clazz && pcmUnsigned && pcmUnsignedObj && pcmSigned && pcmSignedObj &&
                pcmFloat && pcmFloatObj && ulaw && ulawObj && alaw && alawObj;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(env, clazz);
        JNI_RELEASE_GLOBAL(env, pcmUnsignedObj);
        JNI_RELEASE_GLOBAL(env, pcmSignedObj);
        JNI_RELEASE_GLOBAL(env, pcmFloatObj);
        JNI_RELEASE_GLOBAL(env, ulawObj);
        JNI_RELEASE_GLOBAL(env, alawObj);
    }

    AUTO_STATIC_CACHE_GET(AudioFormatEncodingCache)
};