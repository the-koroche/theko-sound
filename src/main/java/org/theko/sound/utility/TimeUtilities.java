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
     */
    public static void waitMicrosPrecise(long micros) {
        long nanos = Math.multiplyExact(micros, 1000);
        waitNanosPrecise(nanos);
    }

    /**
     * Waits for the specified amount of time in nanoseconds with high precision.
     * This method is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
     * It uses busy waiting and {@link LockSupport#parkNanos(long)} to wait for the specified amount of time.
     * 
     * @param nanos The amount of time to wait in nanoseconds.
     */
    public static void waitNanosPrecise(long nanos) {
        if (nanos <= 0) return;
        final long deadline = System.nanoTime() + nanos;
        long remaining;

        while ((remaining = deadline - System.nanoTime()) > 0) {
            if (remaining > 100_000) { 
                LockSupport.parkNanos(remaining - 50_000);
            } else if (remaining > 10_000) { 
                Thread.onSpinWait();
            } else if (remaining > 0) {
                long target = System.nanoTime() + remaining;
                while (System.nanoTime() < target) {
                    Thread.onSpinWait();
                }
                break;
            }
        }
    }

    /**
     * Waits for the specified amount of time in nanoseconds using busy waiting.
     * This method is designed to be more precise than {@link Thread#sleep(long)} and {@link Object#wait(long)}.
     * It uses busy waiting and {@link Thread#onSpinWait()} to wait for the specified amount of time.
     * 
     * @param nanos The amount of time to wait in nanoseconds.
     */
    public static void waitNanosBusy(long nanos) {
        if (nanos <= 0) return;
        final long deadline = System.nanoTime() + nanos;
        while (System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
    }
}
