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
#include "logger.hpp"

class LoggerManager {
private:
    std::unordered_map<std::string, Logger*> loggerCache;
    std::mutex mutex;

public:
    /**
     * @brief Retrieves a SLF4J logger instance by name.
     *
     * @param env The JNIEnv pointer for the current thread.
     * @param name The name of the logger to retrieve.
     *
     * @return A pointer to the logger or nullptr if the logger could not be
     *          created.
     *
     * This function is thread-safe and will return a cached logger if one
     * already exists for the given name. If the logger does not exist, a
     * new one will be created and stored in the cache for future requests.
     * If the logger could not be created for any reason, nullptr will be
     * returned.
     */
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

    /**
     * @brief Releases all cached loggers and the logger cache.
     *
     * This function is thread-safe and will release all loggers and the logger
     * cache if they exist. The loggers will be released in the order they
     * were retrieved from the cache, and the logger cache will be released last.
     *
     * @param env The JNIEnv pointer for the current thread.
     */
    void releaseAll(JNIEnv* env) {
        std::lock_guard<std::mutex> guard(mutex);

        for (auto& [_, logger] : loggerCache) {
            if (logger) {
                logger->release(env);
                delete logger;
            }
        }

        loggerCache.clear();

        LoggerCache::get(env)->release(env);
    }

    ~LoggerManager() {
        fprintf(stderr, "[LoggerManager] Destructor cannot release resources: no JNIEnv available in native destructor. Call releaseAll(env) manually.\n");
    }

    /**
     * @brief Retrieves the global LoggerManager instance.
     *
     * This function returns the global LoggerManager instance, which is
     * lazily initialized when the function is first called. The
     * instance is stored in a static variable and is guaranteed
     * to be thread-safe.
     *
     * @return The global LoggerManager instance.
     */
    static LoggerManager* getManager() {
        static LoggerManager* manager = new LoggerManager();
        return manager;
    }
};