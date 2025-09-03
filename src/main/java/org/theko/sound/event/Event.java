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

package org.theko.sound.event;

/**
 * Base class for all events in the system.
 * <p>
 * An {@code Event} represents something that has happened at a specific
 * point in time. Concrete subclasses (e.g. {@link SoundSourceEvent},
 * {@link PlaybackEvent}) provide additional context data related to
 * the action that triggered them.
 * </p>
 *
 * <p>
 * Every event instance automatically records its creation timestamp,
 * which can be used to order events chronologically, measure delays,
 * or debug the event flow.
 * </p>
 *
 * @since 2.4.0
 * @author Theko
 */
public abstract class Event {

    /**
     * The creation time of this event in milliseconds since the UNIX epoch.
     */
    protected final long timestamp = System.currentTimeMillis();

    /**
     * Indicates whether this event has been consumed.
     */
    private boolean isConsumed;

    /**
     * Returns the moment when this event was created.
     *
     * @return the creation timestamp in milliseconds since the UNIX epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Marks this event as consumed.
     */
    public void consume() {
        isConsumed = true;
    }

    /**
     * Checks if this event has been consumed.
     *
     * @return {@code true} if this event has been consumed, {@code false} otherwise
     */
    public boolean isConsumed() {
        return isConsumed;
    }
}