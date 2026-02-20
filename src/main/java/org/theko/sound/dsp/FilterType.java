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

package org.theko.sound.dsp;

/**
 * Represents common filter types used in audio processing.
 *
 * @since 0.2.3-beta
 * author Theko
 */
public enum FilterType {

    /**
     * Low-pass filter - attenuates (reduces) high frequencies,
     * allowing low frequencies to pass through.
     */
    LOWPASS,

    /**
     * High-pass filter - attenuates low frequencies,
     * allowing high frequencies to pass through.
     */
    HIGHPASS,

    /**
     * Band-pass filter - passes only a specific frequency band
     * and attenuates frequencies outside this range.
     */
    BANDPASS,

    /**
     * Notch filter (band-stop) - attenuates a specific frequency band,
     * passing frequencies outside this range.
     */
    NOTCH,

    /**
     * Peak (resonance) filter - boosts or attenuates a narrow
     * range of frequencies around the center frequency.
     */
    PEAK,

    /**
     * All-pass filter - passes all frequencies but changes
     * the phase response across the spectrum.
     */
    ALLPASS
}