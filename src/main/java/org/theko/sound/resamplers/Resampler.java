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

package org.theko.sound.resamplers;

import org.theko.sound.samples.SamplesValidation;

/**
 * Interface representing a resampling algorithm for audio data.
 * Implementations of this interface define how to resample an input array
 * of audio samples to a specified target length.
 *
 * @since 0.1.4-beta
 * @author Theko
 */
public interface Resampler {

    /**
     * Resamples multi-channel audio samples into the provided output array.
     *
     * @param input the input audio samples (2D array, [channel][sample])
     * @param output the output array for the resampled samples; must have size = [channels][targetLength]
     * @param targetLength the desired length of each channel in the output
     * @throws IllegalArgumentException if the target length is less than or equal to zero, or if samples are null or empty
     */
    void resample(float[][] input, float[][] output, int targetLength);

    /**
     * Resamples multi-channel audio samples and returns a new 2D array.
     *
     * @param input the input audio samples (2D array, [channel][sample])
     * @param targetLength the desired length of each channel in the output
     * @return a new 2D array containing the resampled audio samples
     * @throws IllegalArgumentException if the target length is less than or equal to zero, or if samples are null or empty
     */
    default float[][] resample(float[][] input, int targetLength) {
        SamplesValidation.validateSamples(input);
        if (targetLength <= 0) {
            throw new IllegalArgumentException("Target length must be greater than zero.");
        }
        float[][] output = new float[input.length][targetLength];
        resample(input, output, targetLength);
        return output;
    }
}