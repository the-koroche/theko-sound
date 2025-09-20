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

class AudioFormatCache : public IJavaClassCache {
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

    AudioFormatCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env->FindClass("org/theko/sound/AudioFormat"));
        ctor = JNI_TRY_RETURN(env->GetMethodID(clazz, "<init>", "(IIILorg/theko/sound/AudioFormat$Encoding;Z)V"));
        getSampleRate = JNI_TRY_RETURN(env->GetMethodID(clazz, "getSampleRate", "()I"));
        getBitsPerSample = JNI_TRY_RETURN(env->GetMethodID(clazz, "getBitsPerSample", "()I"));
        getBytesPerSample = JNI_TRY_RETURN(env->GetMethodID(clazz, "getBytesPerSample", "()I"));
        getChannels = JNI_TRY_RETURN(env->GetMethodID(clazz, "getChannels", "()I"));
        getEncoding = JNI_TRY_RETURN(env->GetMethodID(clazz, "getEncoding", "()Lorg/theko/sound/AudioFormat$Encoding;"));
        isBigEndian = JNI_TRY_RETURN(env->GetMethodID(clazz, "isBigEndian", "()Z"));
        getFrameSize = JNI_TRY_RETURN(env->GetMethodID(clazz, "getFrameSize", "()I"));
        getByteRate = JNI_TRY_RETURN(env->GetMethodID(clazz, "getByteRate", "()I"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFormat failed to initialize");
            return;
        }

        clazz = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(clazz));
    }

    bool isValid() const override {
        return clazz && ctor && getSampleRate && getBitsPerSample && getBytesPerSample &&
                getChannels && getEncoding && isBigEndian && getFrameSize && getByteRate;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(clazz);
    }

    AUTO_STATIC_CACHE_GET(AudioFormatCache)
};