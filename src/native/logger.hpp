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

#include "helper_utilities.hpp"

class LoggerCache {
public:
    jclass factoryClass;
    jclass loggerClass;
    jmethodID getLogger;
    jmethodID traceMethod;
    jmethodID debugMethod;
    jmethodID infoMethod;
    jmethodID warnMethod;
    jmethodID errorMethod;

    LoggerCache(JNIEnv* env) {
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

        factoryClass = (jclass) env->NewGlobalRef(factoryClass);
        loggerClass = (jclass) env->NewGlobalRef(loggerClass);
    }

    bool isValid() {
        return factoryClass && loggerClass && getLogger && traceMethod && debugMethod && infoMethod && warnMethod && errorMethod;
    }

    void release(JNIEnv* env) {
        if (factoryClass) {
            env->DeleteGlobalRef(factoryClass);
            factoryClass = nullptr;
        }
        if (loggerClass) {
            env->DeleteGlobalRef(loggerClass);
            loggerClass = nullptr;
        }
    }
};

static LoggerCache* getLoggerCache(JNIEnv* env) {
    static LoggerCache* loggerCache = new LoggerCache(env);
    return loggerCache;
}

class Logger {
private:
    jobject logger = nullptr;

    void log(JNIEnv* env, jmethodID method, const char* msg, va_list args) {
        if (!logger || !method || !msg) return;
        char* formatted = formatv(msg, args);
        if (!formatted) return;

        jstring jmsg = env->NewStringUTF(formatted);
        env->CallVoidMethod(logger, method, jmsg);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe(); 
            env->ExceptionClear();
        }
        env->DeleteLocalRef(jmsg);
        free(formatted);
    }

public:
    Logger(JNIEnv* env, const char* name) {
        LoggerCache* cache = getLoggerCache(env);
        jstring jname = env->NewStringUTF(name);    
        logger = env->CallStaticObjectMethod(cache->factoryClass, cache->getLogger, jname);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe(); 
            env->ExceptionClear();
        }

        logger = (jobject) env->NewGlobalRef(logger);

        env->DeleteLocalRef(jname);
    }

    void trace(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, getLoggerCache(env)->traceMethod, msg, args);
        va_end(args);
    }

    void debug(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, getLoggerCache(env)->debugMethod, msg, args);
        va_end(args);
    }

    void info(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, getLoggerCache(env)->infoMethod, msg, args);
        va_end(args);
    }

    void warn(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, getLoggerCache(env)->warnMethod, msg, args);
        va_end(args);
    }

    void error(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, getLoggerCache(env)->errorMethod, msg, args);
        va_end(args);
    }

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
