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

package org.theko.sound;

/**
 * Represents the direction of audio flow, either input (IN) or output (OUT).
 * 
 * @see AudioPort
 * 
 * @since 1.0.0
 * @author Theko
 */
public enum AudioFlow {
    
    /** Audio input flow (e.g., recording from a microphone). */
    IN,
    /** Audio output flow (e.g., playing sound through speakers). */
    OUT;

    /**
     * Converts a boolean value to an AudioFlow.
     *
     * @param isOut If true, returns {@code OUT}; otherwise, returns {@code IN}.
     * @return The corresponding AudioFlow value.
     */
    public static AudioFlow fromBoolean (boolean isOut) {
        return (isOut ? AudioFlow.OUT : AudioFlow.IN);
    }

    /**
     * Returns a string representation of this AudioFlow.
     *
     * @return "IN" for input flow, "OUT" for output flow.
     */
    @Override
    public String toString () {
        switch (this) {
            case IN: return "IN";
            case OUT: return "OUT";
            default: return "UNKNOWN";
        }
    }
}
