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

class ExceptionClassesCache : public IJavaClassCache {
public:
    jclass runtimeException;
    jclass illegalArgumentException;
    jclass unsupportedOperationException;
    jclass outOfMemoryException;
    
    jclass audioBackendException;
    jclass deviceException;
    jclass deviceInactiveException;
    jclass deviceInvalidatedException;
    jclass unsupportedAudioFormatException;
    jclass unsupportedAudioEncodingException;

    ExceptionClassesCache(JNIEnv* env) : IJavaClassCache(env) {
        runtimeException = JNI_TRY_RETURN(env, env->FindClass("java/lang/RuntimeException"));
        outOfMemoryException = JNI_TRY_RETURN(env, env->FindClass("java/lang/OutOfMemoryError"));
        illegalArgumentException = JNI_TRY_RETURN(env, env->FindClass("java/lang/IllegalArgumentException"));
        unsupportedOperationException = JNI_TRY_RETURN(env, env->FindClass("java/lang/UnsupportedOperationException"));

        audioBackendException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/backend/AudioBackendException"));
        deviceException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/backend/DeviceException"));
        deviceInvalidatedException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/backend/DeviceInvalidatedException"));
        deviceInactiveException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/backend/DeviceInactiveException"));
        unsupportedAudioFormatException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/UnsupportedAudioFormatException"));
        unsupportedAudioEncodingException = JNI_TRY_RETURN(env, env->FindClass("org/theko/sound/UnsupportedAudioEncodingException"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Exception classes failed to initialize");
            return;
        }

        runtimeException = (jclass) JNIUtil_CreateGlobal(env, runtimeException);
        outOfMemoryException = (jclass) JNIUtil_CreateGlobal(env, outOfMemoryException);
        illegalArgumentException = (jclass) JNIUtil_CreateGlobal(env, illegalArgumentException);
        unsupportedOperationException = (jclass) JNIUtil_CreateGlobal(env, unsupportedOperationException);

        audioBackendException = (jclass) JNIUtil_CreateGlobal(env, audioBackendException);
        deviceException = (jclass) JNIUtil_CreateGlobal(env, deviceException);
        deviceInvalidatedException = (jclass) JNIUtil_CreateGlobal(env, deviceInvalidatedException);
        deviceInactiveException = (jclass) JNIUtil_CreateGlobal(env, deviceInactiveException);
        unsupportedAudioFormatException = (jclass) JNIUtil_CreateGlobal(env, unsupportedAudioFormatException);
        unsupportedAudioEncodingException = (jclass) JNIUtil_CreateGlobal(env, unsupportedAudioEncodingException);
    }

    bool isValid() const override {
        return runtimeException && outOfMemoryException && illegalArgumentException && unsupportedOperationException &&
               audioBackendException && deviceException && deviceInvalidatedException && deviceInactiveException &&
               unsupportedAudioFormatException && unsupportedAudioEncodingException;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(env, runtimeException);
        JNI_RELEASE_GLOBAL(env, outOfMemoryException);
        JNI_RELEASE_GLOBAL(env, illegalArgumentException);
        JNI_RELEASE_GLOBAL(env, unsupportedOperationException);

        JNI_RELEASE_GLOBAL(env, audioBackendException);
        JNI_RELEASE_GLOBAL(env, deviceException);
        JNI_RELEASE_GLOBAL(env, deviceInvalidatedException);
        JNI_RELEASE_GLOBAL(env, deviceInactiveException);
        JNI_RELEASE_GLOBAL(env, unsupportedAudioFormatException);
        JNI_RELEASE_GLOBAL(env, unsupportedAudioEncodingException);
    }

    AUTO_STATIC_CACHE_GET(ExceptionClassesCache)
};