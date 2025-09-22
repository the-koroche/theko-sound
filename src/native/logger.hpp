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
#include <cstdarg>
#include <cstdlib>
#include <cstdio>

#include "IJavaClassCache.hpp"
#include "helper_utilities.hpp"
#include "JNI_Utility.hpp"

class LoggerCache : public IJavaClassCache {
public:
    jclass factoryClass;
    jclass loggerClass;
    jmethodID getLogger;
    jmethodID traceMethod;
    jmethodID debugMethod;
    jmethodID infoMethod;
    jmethodID warnMethod;
    jmethodID errorMethod;

    LoggerCache(JNIEnv* env) : IJavaClassCache(env) {
        factoryClass = env->FindClass("org/slf4j/LoggerFactory");
        loggerClass = env->FindClass("org/slf4j/Logger");

        getLogger = env->GetStaticMethodID(factoryClass, "getLogger", "(Ljava/lang/String;)Lorg/slf4j/Logger;");
        traceMethod = env->GetMethodID(loggerClass, "trace", "(Ljava/lang/String;)V");
        debugMethod = env->GetMethodID(loggerClass, "debug", "(Ljava/lang/String;)V");
        infoMethod  = env->GetMethodID(loggerClass, "info",  "(Ljava/lang/String;)V");
        warnMethod  = env->GetMethodID(loggerClass, "warn",  "(Ljava/lang/String;)V");
        errorMethod = env->GetMethodID(loggerClass, "error", "(Ljava/lang/String;)V");

        if (!isValid()) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "Logger failed to initialize");
        }

        factoryClass = (jclass) JNIUtil_CreateGlobal(env, factoryClass);
        loggerClass = (jclass) JNIUtil_CreateGlobal(env, loggerClass);
    }

    bool isValid() const override {
        return factoryClass && loggerClass && getLogger && traceMethod && debugMethod && infoMethod && warnMethod && errorMethod;
    }

    void release(JNIEnv* env) override {
        JNI_RELEASE_GLOBAL(env, factoryClass);
        JNI_RELEASE_GLOBAL(env, loggerClass);
    }

    AUTO_STATIC_CACHE_GET(LoggerCache)
};

class Logger {
private:
    jobject logger = nullptr;

    /**
     * Logs a message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     *
     * @param env The JNI environment.
     * @param method The method to call on the logger.
     * @param msg The message to log.
     * @param args The arguments to pass to the format function.
     */
    void log(JNIEnv* env, jmethodID method, const char* msg, va_list args) {
        if (!logger || !method || !msg) return;
        const char* formatted = formatv(msg, args).c_str();
        if (!formatted) return;

        jstring jmsg = env->NewStringUTF(formatted);
        env->CallVoidMethod(logger, method, jmsg);
        JNIUtil_ReportException(env);
        env->DeleteLocalRef(jmsg);
    }

public:
    /**
     * Creates a logger with the given name.
     *
     * @param env The JNI environment.
     * @param name The name of the logger.
     *
     * This function creates a logger with the given name and stores it in a global reference.
     * It also checks for any pending exceptions and clears them if necessary.
     * If an exception occurs while creating the logger, it is described and cleared.
     * The created logger is stored in the logger field of the object.
     * The name string is deleted from the local reference after it is used.
     */
    Logger(JNIEnv* env, const char* name) {
        LoggerCache* cache = LoggerCache::get(env);
        jstring jname = env->NewStringUTF(name);    
        logger = env->CallStaticObjectMethod(cache->factoryClass, cache->getLogger, jname);
        JNIUtil_ReportException(env);

        logger = (jobject) JNIUtil_CreateGlobal(env, logger);

        env->DeleteLocalRef(jname);
    }

    /**
     * Logs a trace message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment.
     * @param msg The message to log.
     * @param ... The arguments to pass to the format function.
     */
    void trace(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, LoggerCache::get(env)->traceMethod, msg, args);
        va_end(args);
    }

    /**
     * Logs a debug message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment.
     * @param msg The message to log.
     * @param ... The arguments to pass to the format function.
     */
    void debug(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, LoggerCache::get(env)->debugMethod, msg, args);
        va_end(args);
    }

    /**
     * Logs an info message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment.
     * @param msg The message to log.
     * @param ... The arguments to pass to the format function.
     */
    void info(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, LoggerCache::get(env)->infoMethod, msg, args);
        va_end(args);
    }

    /**
     * Logs a warning message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment.
     * @param msg The message to log.
     * @param ... The arguments to pass to the format function.
     */
    void warn(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, LoggerCache::get(env)->warnMethod, msg, args);
        va_end(args);
    }

    /**
     * Logs an error message to the logger. If the logger is null, the method is null or the message is null, the function does nothing.
     * 
     * @param env The JNI environment.
     * @param msg The message to log.
     * @param ... The arguments to pass to the format function.
     */
    void error(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, LoggerCache::get(env)->errorMethod, msg, args);
        va_end(args);
    }

    /**
     * Releases the logger and its global reference.
     *
     * This function releases the logger and its global reference. If the logger is null, the function does nothing.
     *
     * @param env The JNI environment.
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
