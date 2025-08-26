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
 * Utility class for converting audio units such as microseconds, frames, and samples.
 * Provides methods for converting between different time and sample-based representations
 * of audio data.
 *
 * @since 1.1.0
 * author Theko
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
}