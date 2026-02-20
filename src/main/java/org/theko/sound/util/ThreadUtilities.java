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

package org.theko.sound.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.properties.ThreadType;

/**
 * Utility class for creating threads.
 * 
 * @author Theko
 * @since 2.0.0
 */
public final class ThreadUtilities {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtilities.class);

    private ThreadUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Creates a new thread with default name.
     * 
     * @param name The name of the thread. If null, the default name will be used.
     * @param type The type of thread to create.
     * @param task The task to run in the thread.
     * @return The created thread.
     */
    public static Thread startThread(ThreadType type, Runnable task) {
        return startThread(null, type, Thread.NORM_PRIORITY, task);
    }

    /**
     * Creates a new thread.
     * 
     * @param name The name of the thread. If null, the default name will be used.
     * @param type The type of thread to create.
     * @param task The task to run in the thread.
     * @return The created thread.
     */
    public static Thread startThread(String name, ThreadType type, Runnable task) {
        return startThread(name, type, Thread.NORM_PRIORITY, task);
    }


    /**
     * Creates a new thread.
     * 
     * @param name The name of the thread. If null, the default name will be used.
     * @param type The type of thread to create.
     * @param priority The priority of the thread.
     * @param task The task to run in the thread.
     * @return The created thread.
     * @see #startThread(String, ThreadType, int, boolean, Runnable)
     */
    public static Thread startThread(String name, ThreadType type, int priority, Runnable task) {
        return startThread(name, type, priority, true, task);
    }

    
    /**
     * Creates a new thread with the specified name, type, priority, daemon status, and task.
     * <p>
     * Virtual threads does not support daemon and priority, and they will be ignored.
     * If the thread type is not supported, it will fallback to platform thread.
     * 
     * @param name The name of the thread. If null, the default name will be used.
     * @param type The type of thread to create.
     * @param priority The priority of the thread.
     * @param daemon Whether the thread should be a daemon thread.
     * @param task The task to run in the thread.
     * @return The created thread.
     * @throws IllegalArgumentException if the thread type or task is null, or if the thread type is not supported.
     */
    public static Thread startThread(String name, ThreadType type, int priority, boolean daemon, Runnable task) {
        if (type == null || task == null) {
            throw new IllegalArgumentException("Thread type or task cannot be null.");
        }
        logger.trace("Creating thread: {} {}", name, FormatUtilities.formatThreadInfo(type, priority));
        Thread thread = null;

        if (type == ThreadType.VIRTUAL && AudioSystemProperties.IS_SUPPORTING_VIRTUAL_THREADS) {
            try {
                thread = (Thread) Thread.class
                    .getMethod("startVirtualThread", Runnable.class)
                    .invoke(null, task);

                if (thread != null) {
                    if (name != null && !name.isEmpty()) thread.setName(name);
                    else thread.setName("VirtualThread-" + getThreadId(thread));
                }
            } catch (Throwable ex) {
                logger.error("Error creating virtual thread, fallback to platform thread", ex);
            }
        }

        if (thread == null) { // fallback to platform
            thread = new Thread(task);
            if (name != null && !name.isEmpty()) thread.setName(name);
            else thread.setName("PlatformThread-" + getThreadId(thread));

            thread.setPriority(priority);
            thread.setDaemon(daemon);
            thread.start();
        }

        logger.trace("Created thread: {} {}", thread.getName(), FormatUtilities.formatThreadInfo(type, priority));
        return thread;
    }

    /**
     * Returns the ID of the given thread.
     * <p>
     * If the thread is null, returns 0.
     * <p>
     * If the thread is not null, returns the result of calling
     * {@link Thread#getId()} or {@link Thread#threadId()} depending on the
     * Java version. If the reflection call fails, returns 0.
     *
     * @param thread the thread to get the ID of
     * @return the ID of the thread, or 0 if the thread is null or the reflection call fails
     */
    public static long getThreadId(Thread thread) {
        if (thread == null) {
            return 0L;
        }
        String methodName = Runtime.version().feature() >= 19 ? "threadId" : "getId";
        try {
            return (long) Thread.class.getMethod(methodName).invoke(thread);
        } catch (ReflectiveOperationException | SecurityException ex) {
            return 0L;
        }
    }
}
