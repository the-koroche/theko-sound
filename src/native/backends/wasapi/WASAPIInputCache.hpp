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
#include "IJavaClassCache.hpp"
#include "JNI_Utility.hpp"

class WASAPIInputCache : public IJavaClassCache {
public:
    jclass clazz;
    jfieldID inputContextPtr;

    WASAPIInputCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/backend/wasapi/WASAPISharedInput"));
        inputContextPtr = JNI_TRY_RETURN(env, env->GetFieldID(clazz, "inputContextPtr", "J"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "WASAPIInputCache failed to initialize");
            return;
        }

        clazz = (jclass) JNIUtil_CreateGlobal(env, clazz);
    }

    bool isValid() const override {
        return clazz && inputContextPtr;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(env, clazz);
    }

    AUTO_STATIC_CACHE_GET(WASAPIInputCache)
};