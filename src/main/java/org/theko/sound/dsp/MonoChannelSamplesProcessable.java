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

package org.theko.sound.dsp;

/**
 * An interface for processing mono-channel audio samples.
 * 
 * @since 2.3.2
 * @author Theko
 */
public interface MonoChannelSamplesProcessable {
    
    /**
     * Processes a single audio sample.
     * 
     * @param sample the audio sample
     * @param sampleRate the sample rate
     * @return the processed audio sample
     */
    float process(float sample, int sampleRate);

    /**
     * Processes the given audio samples, by repeating {@link #process(float, int)} for each sample.
     * <p>
     * This method returns the processed audio samples in the provided output array.
     * Output array must have the same length as the input array.
     * </p>
     * 
     * @param samples the audio samples
     * @param output the processed audio samples
     * @param sampleRate the sample rate
     */
    default void process(float[] samples, float[] output, int sampleRate) {
        if (samples == null || samples.length == 0 || sampleRate <= 0 || output == null || output.length != samples.length) {
            return;
        }
        for (int i = 0; i < samples.length; i++) {
            output[i] = process(samples[i], sampleRate);
        }
    }

    /**
     * Processes the given audio samples, by repeating {@link #process(float, int)} for each sample.
     * 
     * @param samples the audio samples
     * @param sampleRate the sample rate
     * @return the processed audio samples
     */
    default float[] process(float[] samples, int sampleRate) {
        float[] output = new float[samples.length];
        process(samples, output, sampleRate);
        return output;
    }
}
