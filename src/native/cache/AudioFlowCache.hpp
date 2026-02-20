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

class AudioFlowCache : public IJavaClassCache {
public:
    jclass clazz;
    jfieldID out;
    jfieldID in;
    jobject outObj;
    jobject inObj;

    AudioFlowCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/AudioFlow"));
        out = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "OUT", "Lorg/theko/sound/AudioFlow;"));
        outObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, out));
        in = JNI_TRY_RETURN(env, env->GetStaticFieldID(clazz, "IN", "Lorg/theko/sound/AudioFlow;"));
        inObj = JNI_TRY_RETURN(env, env->GetStaticObjectField(clazz, in));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AudioFlow failed to initialize");
            return;
        }

        clazz = (jclass) JNIUtil_CreateGlobal(env, clazz);
        outObj = JNIUtil_CreateGlobal(env, outObj);
        inObj = JNIUtil_CreateGlobal(env, inObj);
    }

    bool isValid() const override {
        return clazz && out && outObj && in && inObj;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(env, clazz);
        JNI_RELEASE_GLOBAL(env, outObj);
        JNI_RELEASE_GLOBAL(env, inObj);
    }

    AUTO_STATIC_CACHE_GET(AudioFlowCache)
};