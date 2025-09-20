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

#include <vector>
#include "IJavaClassCache.hpp"

class GlobalClassCachesRegistry {
public:
    static std::vector<IJavaClassCache*>& getRegistry() {
        static std::vector<IJavaClassCache*> registry;
        return registry;
    }

    static void add(IJavaClassCache* cache) {
        getRegistry().push_back(cache);
    }

    static void releaseAll(JNIEnv* env) {
        for (auto* cache : getRegistry()) {
            if (cache) {
                cache->release(env);
                delete cache;
            }
        }
        getRegistry().clear();
    }
};