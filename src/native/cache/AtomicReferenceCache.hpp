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
#include "jni_util.hpp"

class AtomicReferenceCache : public IJavaClassCache {
public:
    jclass clazz;
    jmethodID ctor;
    jmethodID getMethod;
    jmethodID setMethod;

    AtomicReferenceCache(JNIEnv* env) : IJavaClassCache(env) {
        clazz = JNI_TRY_RETURN(env->FindClass("java/util/concurrent/atomic/AtomicReference"));
        ctor = JNI_TRY_RETURN(env->GetMethodID(clazz, "<init>", "()V"));
        getMethod = JNI_TRY_RETURN(env->GetMethodID(clazz, "get", "()Ljava/lang/Object;"));
        setMethod = JNI_TRY_RETURN(env->GetMethodID(clazz, "set", "(Ljava/lang/Object;)V"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "AtomicReference failed to initialize");
            return;
        }

        clazz = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(clazz));
    }

    bool isValid() const override {
        return clazz && ctor && getMethod && setMethod;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(clazz);
    }

    AUTO_STATIC_CACHE_GET(AtomicReferenceCache)
};