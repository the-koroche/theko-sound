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

import java.util.concurrent.TimeUnit;

import org.theko.sound.util.FormatUtilities;

/**
 * Represents a measurement of time.
 * It can be created with a specific time and time unit,
 * and can be converted to other time units.
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public class TimeMeasure {

    private static final int DEFAULT_PRECISION = 3;

    private final long time;
    private final TimeUnit unit;

    /**
     * Constructs a new time measure with the given time and unit.
     * @param time the time
     * @param unit the time unit
     * @throws IllegalArgumentException if the time is negative
     */
    public TimeMeasure(long time, TimeUnit unit) {
        if (time < 0) throw new IllegalArgumentException("Time cannot be negative");
        this.time = time;
        this.unit = unit;
    }

    /**
     * @return The time value of this time measure
     */
    public long getTime() {
        return time;
    }

    /**
     * @return The time unit of this time measure
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * @return The time value of this time measure in milliseconds
     */
    public long asMillis() {
        return unit.toMillis(time);
    }

    @Override
    public String toString() {
        return FormatUtilities.formatTime(unit.toNanos(time), DEFAULT_PRECISION);
    }
}
