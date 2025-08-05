#pragma once

#include <jni.h>
#include <string>
#include <unordered_map>
#include <mutex>
#include "Logger.hpp"

class LoggerManager {
private:
    std::unordered_map<std::string, Logger*> loggerCache;
    std::mutex mutex;

public:
    Logger* getLogger(JNIEnv* env, const std::string& name) {
        std::lock_guard<std::mutex> guard(mutex);

        auto it = loggerCache.find(name);
        if (it != loggerCache.end()) {
            return it->second;
        }

        Logger* logger = new Logger(env, name.c_str());
        if (logger) {
            loggerCache[name] = logger;
        }

        return logger;
    }

    void releaseAll(JNIEnv* env) {
        std::lock_guard<std::mutex> guard(mutex);

        for (auto& [_, logger] : loggerCache) {
            if (logger) {
                logger->release(env);
                delete logger;
            }
        }

        loggerCache.clear();
    }

    ~LoggerManager() {
        fprintf(stderr, "LoggerManager::~LoggerManager has no JNIEnv; use releaseAll(env) manually.\n");
    }
};

static LoggerManager* getLoggerManager() {
    static LoggerManager* loggerManager = new LoggerManager();
    return loggerManager;
}