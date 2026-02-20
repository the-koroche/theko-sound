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

package org.theko.sound.properties;

import org.theko.sound.util.ThreadUtilities;

/**
 * Configuration for a thread.
 * It includes the thread type and priority.
 * 
 * @see AudioSystemProperties
 * @see ThreadType
 * @see ThreadUtilities
 * 
 * @since 0.2.4-beta
 * @author Theko
 */
public class ThreadConfiguration {
    public final ThreadType threadType;
    public final int priority;

    public ThreadConfiguration(ThreadType threadType, int priority) {
        this.threadType = threadType;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "ThreadConfig{type=%s, priority=%d}".formatted(threadType, priority);
    }
}