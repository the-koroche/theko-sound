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
#include <string>

/**
 * Reports a Java exception if there is one, and clears it.
 * Returns true if there was an exception to report.
 * @param env The JNIEnv to report the exception on.
 * @return true if there was an exception to report, false otherwise.
 */
static bool reportJavaException(JNIEnv* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

#define JNI_TRY_RETURN(expr) ([&]() { \
    auto _res = (expr); \
    if (reportJavaException(env)) return decltype(_res)(); \
    return _res; \
}())

#define JNI_TRY(expr) do { \
    (expr); \
    reportJavaException(env); \
} while (0)

#define JNI_RELEASE_GLOBAL(obj) do { \
    if (obj) { \
        env->DeleteGlobalRef(obj); \
        obj = nullptr; \
    } \
} while (0)

/**
 * Converts a Java string to a UTF-8 encoded C++ string.
 * The resulting string will be empty if the input Java string is null.
 * @param env The JNI environment.
 * @param str The Java string to convert.
 * @return The UTF-8 encoded C++ string.
 */
static std::string ConvertJStringToUTF8(JNIEnv* env, jstring str) {
    if (!str) return {};
    const char* chars = env->GetStringUTFChars(str, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(str, chars);
    return result;
}