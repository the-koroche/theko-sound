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
 * Utility class for generating waveforms.
 * 
 * @since 2.3.2
 * @author Theko
 */
public class WaveformGenerator {
    
    private WaveformGenerator() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Generates a waveform based on the specified type and value.
     * 
     * @param type The waveform type.
     * @param value The value to generate the waveform for.
     * @return The generated waveform value.
     */
    public static float generate(WaveformType type, float value) {
        switch (type) {
            case SINE:
                return (float)Math.sin(value * Math.PI * 2);
            case SQUARE:
                return (float)Math.signum(Math.sin(value * Math.PI * 2));
            case TRIANGLE:
                return 1f - 4f * Math.abs(value % 1f - 0.5f);
            case SAWTOOTH:
                return 2f * (value % 1f) - 1f;
        }
        return 0.0f;
    }
}
