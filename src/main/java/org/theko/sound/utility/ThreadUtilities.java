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

package org.theko.sound.utility;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.properties.AudioSystemProperties.ThreadType;

/**
 * Utility class for creating threads.
 * 
 * @author Theko
 * @since 2.0.0
 */
public class ThreadUtilities {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtilities.class);

    private ThreadUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Creates a new thread.
     * 
     * @param name The name of the thread.
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
     * @param name The name of the thread.
     * @param type The type of thread to create.
     * @param priority The priority of the thread.
     * @param task The task to run in the thread.
     * @return The created thread.
     */
    public static Thread startThread(String name, ThreadType type, int priority, Runnable task) {
        return startThread(name, type, priority, true, task);
    }

    /**
     * Creates a new thread.
     * 
     * @param name The name of the thread.
     * @param type The type of thread to create.
     * @param priority The priority of the thread.
     * @param daemon True if the thread is a daemon, false otherwise.
     * @param task The task to run in the thread.
     * @return The created thread.
     */
    public static Thread startThread(String name, ThreadType type, int priority, boolean daemon, Runnable task) {
        if (type == null || task == null) {
            throw new IllegalArgumentException("Thread type or task cannot be null.");
        }
        
        switch (type) {
            case PLATFORM:
                Thread thread = new Thread(task, name);
                thread.setPriority(priority);
                thread.setDaemon(daemon);
                thread.start();
                logger.debug("Created platform thread: {} [{}]", name, FormatUtilities.formatThreadInfo(type, priority));
                return thread;
            case VIRTUAL:
                if (Runtime.version().feature() >= 21) {
                    try {
                        // Use reflection
                        MethodHandle virtualThread = MethodHandles.lookup()
                        .findStatic(
                            Thread.class, "startVirtualThread",
                            MethodType.methodType(Thread.class, Runnable.class));
                        Object virtualThreadObject = virtualThread.invoke(task);
                        if (virtualThreadObject == null) {
                            logger.error("Failed to create virtual thread: {}", name);
                            return null;
                        }
                        if (virtualThreadObject instanceof Thread) {
                            logger.debug("Created virtual thread: {} [{}]", name, FormatUtilities.formatThreadInfo(type, priority));
                            return (Thread) virtualThreadObject;
                        } else {
                            logger.error("Returned thread is not a Thread instance: {}", virtualThreadObject);
                            return null;
                        }
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                        logger.error("Error creating virtual thread", ex);
                    } catch (Throwable ex) {
                        logger.error("Error creating virtual thread", ex);
                    }
                } else {
                    logger.error("Virtual threads are not supported in Java {}", Runtime.version().feature());
                }
            default:
                throw new IllegalArgumentException("Unsupported thread type: " + type);
        }
    }
}
