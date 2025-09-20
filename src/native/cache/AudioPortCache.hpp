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

class AudioPortCache : public IJavaClassCache {
public:
    jclass clazz;
    jmethodID ctor;
    jmethodID getName;
    jmethodID getVendor;
    jmethodID getVersion;
    jmethodID getDescription;
    jmethodID getFlow;
    jmethodID getMixFormat;
    jmethodID isActive;
    jmethodID getLink;

    AudioPortCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env->FindClass("org/theko/sound/AudioPort"));
        ctor = JNI_TRY_RETURN(env->GetMethodID(clazz, "<init>", "(Ljava/lang/Object;Lorg/theko/sound/AudioFlow;ZLorg/theko/sound/AudioFormat;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
        getName = JNI_TRY_RETURN(env->GetMethodID(clazz, "getName", "()Ljava/lang/String;"));
        getVendor = JNI_TRY_RETURN(env->GetMethodID(clazz, "getVendor", "()Ljava/lang/String;"));
        getVersion = JNI_TRY_RETURN(env->GetMethodID(clazz, "getVersion", "()Ljava/lang/String;"));
        getDescription = JNI_TRY_RETURN(env->GetMethodID(clazz, "getDescription", "()Ljava/lang/String;"));
        getFlow = JNI_TRY_RETURN(env->GetMethodID(clazz, "getFlow", "()Lorg/theko/sound/AudioFlow;"));
        getMixFormat = JNI_TRY_RETURN(env->GetMethodID(clazz, "getMixFormat", "()Lorg/theko/sound/AudioFormat;"));
        isActive = JNI_TRY_RETURN(env->GetMethodID(clazz, "isActive", "()Z"));
        getLink = JNI_TRY_RETURN(env->GetMethodID(clazz, "getLink", "()Ljava/lang/Object;"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioPort failed to initialize");
            return;
        }

        clazz = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(clazz));
    }

    bool isValid() const override {
        return clazz && ctor && getName && getVendor && getVersion && getDescription && getFlow && getMixFormat && isActive && getLink;
    }
    
    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(clazz);
    }

    AUTO_STATIC_CACHE_GET(AudioPortCache)
};