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

import org.theko.sound.samples.SamplesValidation;

/**
 * Interface for audio filters that process single audio samples.
 * Audio filters can be used to filter audio samples in real-time applications.
 * 
 * <p>This interface provides methods for processing single audio samples and arrays of audio samples.
 * 
 * @see BiquadStage
 * @see CascadeFilter
 * @see ChannelSplittedFilter
 * 
 * @since 2.4.1
 * @author Theko
 */
public interface AudioFilter {
    
    /**
     * Processes a single audio sample.
     * @param sample the input sample
     * @param sampleRate the sample rate in Hz
     * @return the processed sample
     */
    float process(float sample, int sampleRate);

    
    /**
     * Processes an array of audio samples and writes the processed samples to the provided output array.
     * 
     * @param samples the input samples
     * @param output the output samples
     * @param sampleRate the sample rate in Hz
     * @throws IllegalArgumentException if the samples and output arrays do not have the same length
     */
    default void process(float[] samples, float[] output, int sampleRate) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        if (SamplesValidation.checkSamplesDimensions(samples, output) != SamplesValidation.DimensionsResult.EXACT) {
            throw new IllegalArgumentException("Samples and output arrays must have the same length.");
        }
        for (int i = 0; i < samples.length; i++) {
            output[i] = process(samples[i], sampleRate);
        }
    }
    
    /**
     * Creates a copy of the filter.
     * Implementations should return a deep copy, to avoid side effects.
     * @return a copy of the filter
     */
    AudioFilter copyFilter();
}
