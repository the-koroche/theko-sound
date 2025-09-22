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

static bool JNIUtil_ReportException(JNIEnv* env);
static jobject JNIUtil_CreateGlobal(JNIEnv* env, jobject obj);
static void JNIUtil_ReleaseGlobal(JNIEnv* env, jobject obj);

#define JNI_TRY_RETURN(env, expr) ([&]() { \
    auto _res = (expr); \
    if (JNIUtil_ReportException(env)) return decltype(_res)(); \
    return _res; \
}())

#define JNI_TRY(env, expr) do { \
    (expr); \
    JNIUtil_ReportException(env); \
} while (0)

#define JNI_RELEASE_GLOBAL(env, obj) do { \
    JNIUtil_ReleaseGlobal(env, obj); \
    obj = nullptr; \
} while (0)


/**
 * Reports whether an exception is pending in the given
 * environment. If true, then an exception is pending and the
 * environment should be checked for exceptions. If false, then no exception is
 * pending.
 * @param env The JNI environment.
 * @return True if an exception is pending, false otherwise.
 */
static bool JNIUtil_ReportException(JNIEnv* env) {
    if (!env) return false;
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

/**
 * Creates a global reference to the given object.
 * If the input object is null, returns null.
 * If an exception occurs while creating the global reference, it is
 * reported and cleared, and nullptr is returned.
 * @param env The JNI environment.
 * @param obj The object to create a global reference to.
 * @return The created global reference, or nullptr if an exception
 * occurs.
 */
static jobject JNIUtil_CreateGlobal(JNIEnv* env, jobject obj) { 
    if (!env || !obj) return nullptr;
    jobject res = env->NewGlobalRef(obj);
    if (!res) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Failed to create global reference");
        return nullptr;
    }
    if (JNIUtil_ReportException(env)) return nullptr;
    return res;
}

/**
 * Releases a global reference to the given object.
 * If the input object is null, this function does nothing.
 * If an exception occurs while releasing the global reference, it is
 * reported and cleared.
 * @param env The JNI environment.
 * @param obj The object to release the global reference to.
 */
static void JNIUtil_ReleaseGlobal(JNIEnv* env, jobject obj) { 
    if (!env || !obj) return;
    env->DeleteGlobalRef(obj);
    JNIUtil_ReportException(env);
}

/**
 * Converts a Java string to a UTF-8 encoded C++ string.
 * The resulting string will be empty if the input Java string is null.
 * @param env The JNI environment.
 * @param str The Java string to convert.
 * @return The UTF-8 encoded C++ string.
 */
static std::string JNIUtil_StringToUTF8(JNIEnv* env, jstring str) {
    if (!env || !str) return {};
    const char* chars = env->GetStringUTFChars(str, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(str, chars);
    return result;
}