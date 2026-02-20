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

import java.util.concurrent.locks.LockSupport;

/**
 * Utility class for waiting with high precision.
 * <p>
 * This class provides methods to wait for a certain amount of time with high precision.
 * It is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
 * <p>
 * The methods in this class use busy waiting and {@link LockSupport#parkNanos(long)} to wait for the specified amount of time.
 * <p>
 * This class is final and cannot be instantiated.
 *
 * @since 2.4.1
 * @author Theko
 */
public final class TimeUtilities {

    private TimeUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }
    
    /**
     * Waits for the specified amount of time in microseconds with high precision.
     * This method is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
     * It uses busy waiting and {@link LockSupport#parkNanos(long)} to wait for the specified amount of time.
     * 
     * @param micros The amount of time to wait in microseconds.
     * @throws InterruptedException when the thread is interrupted while waiting.
     */
    public static void waitMicrosPrecise(long micros) throws InterruptedException {
        long nanos = Math.multiplyExact(micros, 1000);
        waitNanosPrecise(nanos);
    }

    /**
     * Waits for the specified amount of time in nanoseconds with high precision.
     * This method is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
     * It uses busy waiting and {@link LockSupport#parkNanos(long)} to wait for the specified amount of time.
     * 
     * @param nanos The amount of time to wait in nanoseconds.
     * @throws InterruptedException when the thread is interrupted while waiting.
     */
    public static void waitNanosPrecise(long nanos) throws InterruptedException {
        if (nanos <= 0) return;

        final long deadline = System.nanoTime() + nanos;
        long remaining;

        while ((remaining = deadline - System.nanoTime()) > 0) {
            // Immediate interrupt check
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            if (remaining > 100_000) {
                LockSupport.parkNanos(remaining - 50_000);

                // if parkNanos was interrupted, check
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            } else {
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Waits for the specified amount of time in nanoseconds using busy waiting.
     * This method is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
     * It uses busy waiting and {@link Thread#onSpinWait()} to wait for the specified amount of time.
     * 
     * @param nanos The amount of time to wait in nanoseconds.
     * @throws InterruptedException when the thread is interrupted while waiting.
     */
    public static void waitNanosBusy(long nanos) throws InterruptedException {
        if (nanos <= 0) return;
        final long deadline = System.nanoTime() + nanos;
        while (System.nanoTime() < deadline) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            Thread.onSpinWait();
        }
    }
}
