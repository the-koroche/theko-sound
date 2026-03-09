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
#include <string>
#include <cstdarg>
#include <cstdlib>
#include <cstdio>
#include "cache/SLF4J_Logger.hpp"
#include "cache/SLF4J_LoggerFactory.hpp"
#include "helper_utilities.hpp"

class Logger {
private:
    jobject logger = nullptr;

    /**
     * Logs a message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     *
     * @param env The JNI environment
     * @param method The method to call on the logger
     * @param msg The message to log
     * @param args The arguments to pass to the format function
     */
    void log(JNIEnv* env, jmethodID method, const char* msg, va_list args) {
        if (!logger || !method || !msg) return;
        const char* formatted = formatv(msg, args).c_str();
        if (!formatted) return;

        jstring jmsg = env->NewStringUTF(formatted);
        env->CallVoidMethod(logger, method, jmsg);
        env->DeleteLocalRef(jmsg);
    }

public:
    /**
     * Creates a logger with the given name.
     *
     * @param env The JNI environment
     * @param name The name of the logger
     *
     * This function creates a logger with the given name and stores it in a global reference.
     * It also checks for any pending exceptions and clears them if necessary.
     * If an exception occurs while creating the logger, it is described and cleared.
     * The created logger is stored in the logger field of the object.
     * The name string is deleted from the local reference after it is used.
     */
    Logger(JNIEnv* env, const char* name) {
        auto loggerCache = SLF4J_Logger::get(env);
        auto factoryCache = SLF4J_LoggerFactory::get(env);
        jstring jname = env->NewStringUTF(name);
        
        jobject localLogger = factoryCache->getLogger__java_lang_String(env, jname);
        env->DeleteLocalRef(jname);
        
        if (!localLogger) return;
        
        logger = env->NewGlobalRef(localLogger);
        env->DeleteLocalRef(localLogger);
    }

    /**
     * Logs a trace message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment
     * @param msg The message to log
     * @param ... The arguments to pass to the format function
     */
    void trace(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, SLF4J_Logger::getmtd__trace_java_lang_String(env), msg, args);
        va_end(args);
    }

    /**
     * Logs a debug message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment
     * @param msg The message to log
     * @param ... The arguments to pass to the format function
     */
    void debug(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, SLF4J_Logger::getmtd__debug_java_lang_String(env), msg, args);
        va_end(args);
    }

    /**
     * Logs an info message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment
     * @param msg The message to log
     * @param ... The arguments to pass to the format function
     */
    void info(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, SLF4J_Logger::getmtd__info_java_lang_String(env), msg, args);
        va_end(args);
    }

    /**
     * Logs a warning message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment
     * @param msg The message to log
     * @param ... The arguments to pass to the format function
     */
    void warn(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, SLF4J_Logger::getmtd__warn_java_lang_String(env), msg, args);
        va_end(args);
    }

    /**
     * Logs an error message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment
     * @param msg The message to log
     * @param ... The arguments to pass to the format function
     */
    void error(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, SLF4J_Logger::getmtd__error_java_lang_String(env), msg, args);
        va_end(args);
    }

    /**
     * Releases the logger and its global reference.
     *
     * This function releases the logger and its global reference. If the logger is null, the function does nothing.
     *
     * @param env The JNI environment
     */
    void release(JNIEnv* env) {
        if (logger) {
            env->DeleteGlobalRef(logger);
            logger = nullptr;
        }
    }

    ~Logger() {
        fprintf(stderr, "[Logger] Destructor cannot release resources: no JNIEnv available in native destructor. Call release(env) manually.\n");
    }
};
