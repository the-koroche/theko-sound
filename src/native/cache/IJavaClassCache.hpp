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

class IJavaClassCache {
public:
    IJavaClassCache(JNIEnv* env) {};
    virtual ~IJavaClassCache() = default; // Use release(JNIEnv* env)
    
    virtual bool isValid() const = 0;
    virtual void release(JNIEnv* env) = 0;
};

#include "GlobalClassCachesRegistry.hpp"

#define AUTO_STATIC_CACHE_GET(_typename) \
static _typename* get(JNIEnv* env) { \
    static _typename* cache = nullptr; \
    if (!cache) { \
        if (!env) return nullptr; \
        cache = new _typename(env); \
        GlobalClassCachesRegistry::add(cache); \
    } \
    return cache; \
}
