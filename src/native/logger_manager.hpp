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

        getLoggerCache(env)->release(env);
    }

    ~LoggerManager() {
        fprintf(stderr, "[LoggerManager] Destructor cannot release resources: no JNIEnv available in native destructor. Call releaseAll(env) manually.\n");
    }
};

static LoggerManager* getLoggerManager() {
    static LoggerManager* loggerManager = new LoggerManager();
    return loggerManager;
}