#pragma once

#include <jni.h>
#include <string>
#include <cstdarg>
#include <cstdlib>
#include <cstdio>

#include "helper_utilities.hpp"

class Logger {
private:
    jobject logger = nullptr;
    jmethodID debugMethod = nullptr;
    jmethodID infoMethod = nullptr;
    jmethodID warnMethod = nullptr;
    jmethodID errorMethod = nullptr;

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
        jclass loggerFactory = env->FindClass("org/slf4j/LoggerFactory");
        if (!loggerFactory) return;

        jmethodID getLogger = env->GetStaticMethodID(
            loggerFactory, "getLogger", "(Ljava/lang/String;)Lorg/slf4j/Logger;");
        if (!getLogger) {
            env->DeleteLocalRef(loggerFactory);
            return;
        }

        jstring jname = env->NewStringUTF(name);
        jobject loggerObj = env->CallStaticObjectMethod(loggerFactory, getLogger, jname);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe(); 
            env->ExceptionClear();
        }
        env->DeleteLocalRef(jname);
        env->DeleteLocalRef(loggerFactory);

        if (!loggerObj) return;
        jclass loggerClass = env->GetObjectClass(loggerObj);

        debugMethod = env->GetMethodID(loggerClass, "debug", "(Ljava/lang/String;)V");
        infoMethod  = env->GetMethodID(loggerClass, "info",  "(Ljava/lang/String;)V");
        warnMethod  = env->GetMethodID(loggerClass, "warn",  "(Ljava/lang/String;)V");
        errorMethod = env->GetMethodID(loggerClass, "error", "(Ljava/lang/String;)V");

        if (!debugMethod || !infoMethod || !warnMethod || !errorMethod) {
            env->DeleteLocalRef(loggerClass);
            env->DeleteLocalRef(loggerObj);
            return;
        }

        logger = env->NewGlobalRef(loggerObj);
        env->DeleteLocalRef(loggerClass);
        env->DeleteLocalRef(loggerObj);
    }

    void debug(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, debugMethod, msg, args);
        va_end(args);
    }

    void info(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, infoMethod, msg, args);
        va_end(args);
    }

    void warn(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, warnMethod, msg, args);
        va_end(args);
    }

    void error(JNIEnv* env, const char* msg, ...) {
        va_list args;
        va_start(args, msg);
        log(env, errorMethod, msg, args);
        va_end(args);
    }

    void release(JNIEnv* env) {
        if (logger) {
            env->DeleteGlobalRef(logger);
            logger = nullptr;
        }
    }

    ~Logger() {
        fprintf(stderr, "Logger::~Logger — has no JNIEnv; use release(env) manually.\n");
    }
};
