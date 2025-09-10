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
 * Provides methods for converting between time durations in microseconds and audio frames,
 * and also between linear amplitude multipliers and decibel (dB) values.
 *
 * @since 1.1.0
 * @author Theko
 */
public final class AudioUnitsConverter {

    private AudioUnitsConverter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Converts a time duration in microseconds to the equivalent number of audio frames.
     *
     * @param microseconds The time duration in microseconds.
     * @param sampleRate   The sample rate of the audio data (samples per second).
     * @return The equivalent number of audio frames.
     */
    public static long microsecondsToFrames(long microseconds, int sampleRate) {
        return (microseconds * sampleRate) / 1_000_000L;
    }

    /**
     * Converts a number of audio frames to the equivalent time duration in microseconds.
     *
     * @param frames      The number of audio frames.
     * @param sampleRate  The sample rate of the audio data (samples per second).
     * @return The equivalent time duration in microseconds.
     */
    public static long framesToMicroseconds(long frames, int sampleRate) {
        return (frames * 1_000_000L) / sampleRate;
    }

    /**
     * Converts a total number of audio samples across all channels to the equivalent time duration in microseconds.
     * <p>
     * Note: Here a "sample" refers to a single value in one channel (not a frame).
     *
     * @param samples     The total number of audio samples across all channels.
     * @param sampleRate  The sample rate of the audio data (samples per second).
     * @param channels    The number of channels in the audio data.
     * @return The equivalent time duration in microseconds.
     */
    public static long samplesToMicroseconds(long samples, int sampleRate, int channels) {
        return (long) (samples / channels * 1_000_000.0 / sampleRate);
    }

    /**
     * Converts a time duration in microseconds to the equivalent number of audio samples across all channels.
     * <p>
     * Note: Here a "sample" refers to a single value in one channel (not a frame).
     *
     * @param microseconds The time duration in microseconds.
     * @param sampleRate   The sample rate of the audio data (samples per second).
     * @param channels     The number of channels in the audio data.
     * @return The equivalent number of total audio samples.
     */
    public static long microsecondsToSamples(long microseconds, int sampleRate, int channels) {
        return (long) (microseconds * sampleRate / 1_000_000.0) * channels;
    }

    /**
     * Converts a decibel (dB) value to a linear amplitude multiplier.
     * <p>
     * Decibel values are relative to a reference level of 1.0 (0 dB), and are typically used to measure
     * the amplitude of audio signals. A decibel value of -3 dB represents a relative amplitude of 0.7071,
     * which is half the power of 0 dB. A decibel value of -20 dB represents a relative amplitude of 0.1,
     * which is one-tenth the power of 0 dB.
     * <p>
     * The conversion formula is: linear = 10^(decibels/20)
     * <p>
     * Returns 0.0 if decibels is Double.NEGATIVE_INFINITY.
     * 
     * @param decibels The decibel value to convert.
     * @return The linear amplitude multiplier.
     */
    public static double decibelToLinear(double decibels) {
        if (decibels == Double.NEGATIVE_INFINITY) return 0.0;
        return Math.pow(10.0, decibels / 20.0);
    }

    /**
     * Converts a linear amplitude multiplier to a decibel (dB) value.
     * <p>
     * The conversion formula is: decibels = 20 * log10(linear)
     * <p>
     * Returns Double.NEGATIVE_INFINITY if linear is 0.0 or negative.
     * 
     * @param linear The linear amplitude multiplier to convert.
     * @return The decibel value.
     */
    public static double linearToDecibel(double linear) {
        if (linear <= 0.0) return Double.NEGATIVE_INFINITY;
        return 20.0 * Math.log10(linear);
    }
}