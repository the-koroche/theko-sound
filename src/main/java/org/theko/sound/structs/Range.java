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

package org.theko.sound.structs;

import java.io.Serializable;

/**
 * A utility class representing a range of values.
 * <p>
 * This class is used to represent a range of values of a type that implements {@link Comparable}.
 * It provides methods to check if a value is within the range, and to clamp a value to the range.
 * <p>
 * Example usage:
 * <pre>{@code}
 * Range<Integer> range = new Range<>(0, 10);
 * boolean withinRange = range.contains(5);
 * Integer clampedValue = range.clamp(15);
 * </pre>
 *
 * @param <T> the type of the values in the range, must implement {@link Comparable}
 * @author Theko
 * @since 0.3.0-beta
 */
public class Range<T extends Comparable<T>> implements Serializable {

    private final T min;
    private final T max;

    /**
     * Creates a new range with the specified minimum and maximum values.
     *
     * @param min the minimum value of the range
     * @param max the maximum value of the range
     */
    public Range(T min, T max) {
        this.min = min;
        this.max = max;
    }

    /** @return the minimum value of the range */
    public T getMin() { return min; }

    /** @return the maximum value of the range */
    public T getMax() { return max; }

    /**
     * Checks if the given value is within the range.
     * <p>
     * A value is considered to be within the range if it is greater than or equal to the minimum value,
     * and less than or equal to the maximum value.
     *
     * @param value the value to check
     * @return true if the value is within the range, false otherwise
     */
    public boolean contains(T value) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    /**
     * Clamps the given value to be within the range.
     *
     * @param value the value to clamp
     * @return the clamped value
     */
    public T clamp(T value) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }
}
