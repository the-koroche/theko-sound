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

class ExceptionClassesCache : public IJavaClassCache {
public:
    jclass runtimeException;
    jclass illegalArgumentException;
    jclass unsupportedOperationException;
    jclass outOfMemoryException;
    
    jclass audioBackendException;
    jclass unsupportedAudioFormatException;
    jclass unsupportedAudioEncodingException;

    ExceptionClassesCache(JNIEnv* env) : IJavaClassCache(env) {
        runtimeException = JNI_TRY_RETURN(env->FindClass("java/lang/RuntimeException"));
        outOfMemoryException = JNI_TRY_RETURN(env->FindClass("java/lang/OutOfMemoryError"));
        illegalArgumentException = JNI_TRY_RETURN(env->FindClass("java/lang/IllegalArgumentException"));
        unsupportedOperationException = JNI_TRY_RETURN(env->FindClass("java/lang/UnsupportedOperationException"));

        audioBackendException = JNI_TRY_RETURN(env->FindClass("org/theko/sound/backend/AudioBackendException"));
        unsupportedAudioFormatException = JNI_TRY_RETURN(env->FindClass("org/theko/sound/UnsupportedAudioFormatException"));
        unsupportedAudioEncodingException = JNI_TRY_RETURN(env->FindClass("org/theko/sound/UnsupportedAudioEncodingException"));

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Exception classes failed to initialize");
            return;
        }

        runtimeException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(runtimeException));
        outOfMemoryException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(outOfMemoryException));
        illegalArgumentException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(illegalArgumentException));
        unsupportedOperationException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(unsupportedOperationException));

        audioBackendException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(audioBackendException));
        unsupportedAudioFormatException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(unsupportedAudioFormatException));
        unsupportedAudioEncodingException = (jclass) JNI_TRY_RETURN(env->NewGlobalRef(unsupportedAudioEncodingException));
    }

    bool isValid() const override {
        return runtimeException && outOfMemoryException && illegalArgumentException && unsupportedOperationException &&
               audioBackendException && unsupportedAudioFormatException && unsupportedAudioEncodingException;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(runtimeException);
        JNI_RELEASE_GLOBAL(outOfMemoryException);
        JNI_RELEASE_GLOBAL(illegalArgumentException);
        JNI_RELEASE_GLOBAL(unsupportedOperationException);

        JNI_RELEASE_GLOBAL(audioBackendException);
        JNI_RELEASE_GLOBAL(unsupportedAudioFormatException);
        JNI_RELEASE_GLOBAL(unsupportedAudioEncodingException);
    }

    AUTO_STATIC_CACHE_GET(ExceptionClassesCache)
};