
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
 * Represents different types of waveforms.
 * 
 * @see WaveformGenerator
 * 
 * @since 2.3.2
 * @author Theko
 */
public enum WaveformType {
    /**
     * Sine waveform.
     */
    SINE,

    /**
     * Square waveform, with a 50% duty cycle.
     */
    SQUARE,

    /**
     * Sawtooth waveform.
     */
    SAWTOOTH,
    
    /**
     * Triangle waveform.
     */
    TRIANGLE
}
