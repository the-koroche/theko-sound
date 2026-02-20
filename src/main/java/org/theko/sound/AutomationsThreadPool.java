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

package org.theko.sound;

import static org.theko.sound.properties.AudioSystemProperties.AUTOMATIONS_THREADS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the thread pool used to execute
 * the automation event loop. It is responsible for starting and shutting
 * down the thread pool.
 * 
 * @see Automation
 *
 * @since 0.2.4-beta
 * @author Theko
 */
class AutomationsThreadPool {

    private static final Logger logger = LoggerFactory.getLogger(AutomationsThreadPool.class);

    private static final AtomicReference<ExecutorService> executorRef = new AtomicReference<>();
    private static final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    private static final Runnable poolShutdownHook = () -> {
        if (!isShuttingDown.get()) {
            isShuttingDown.set(true);
            shutdown();
        }
    };

    static {
        startPool();
        Runtime.getRuntime().addShutdownHook(new Thread(poolShutdownHook, "Automations-Pool Shutdown Hook"));
    }

    /**
     * Starts the thread pool used to execute the automation event loop.
     * This method is thread-safe and idempotent, meaning it can be called
     * multiple times without side effects. If the pool is already running,
     * this method does nothing.
     */
    private static synchronized void startPool() {
        if (executorRef.get() == null || executorRef.get().isShutdown() || executorRef.get().isTerminated()) {
            ExecutorService newExecutor = Executors.newFixedThreadPool(AUTOMATIONS_THREADS);
            executorRef.set(newExecutor);
            logger.debug("Automations pool started with {} threads.", AUTOMATIONS_THREADS);
        }
    }

    /**
     * Submits a task to the thread pool used to execute the automation event loop.
     * If the task is null, this method does nothing. If the pool is shutting down by the shutdown hook, the task is ignored.
     * If the pool is shutdown, it is restarted before submitting the task.
     * 
     * @param task The task to submit to the thread pool.
     */
    public static void submit(Runnable task) {
        if (task == null) return;

        if (isShuttingDown.get()) {
            logger.warn("Automations pool is shutting down. Ignoring task.");
            return;
        }

        ExecutorService executor = executorRef.get();
        if (executor == null || executor.isShutdown()) {
            logger.warn("Automations pool is shutdown. Restarting...");
            startPool();
            executor = executorRef.get();
        }

        executor.submit(task);
    }


    /**
     * Shuts down the thread pool used to execute the automation event loop.
     * This method is thread-safe and idempotent, meaning it can be called
     * multiple times without side effects. If the pool is already shutting down,
     * this method does nothing.
     * 
     * <p>When shutting down the pool, the following steps are taken:
     * <ul>
     *   <li>The pool is marked as shutting down.</li>
     *   <li>The pool is shut down using {@link ExecutorService#shutdown()}.</li>
     *   <li>If the pool does not terminate within 500 milliseconds, the pool is
     *       forced to shut down using {@link ExecutorService#shutdownNow()}.</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for the pool to
     *             shut down. In this case, the thread is interrupted, and the pool is
     *             immediately shut down using {@link ExecutorService#shutdownNow()}.
     */
    public static synchronized void shutdown() {
        ExecutorService executor = executorRef.getAndSet(null);
        if (executor == null) return;

        logger.debug("Shutting down automations pool.");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                logger.warn("Automations pool shutdown timed out, forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warn("Automations pool shutdown interrupted.");
        }
    }
}
