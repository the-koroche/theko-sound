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

package org.theko.sound.resamplers;

/**
 * Interface representing a resampling algorithm for audio data.
 * Implementations of this interface define how to resample an input array
 * of audio samples to a specified target length.
 * 
 * @since 1.4.1
 * @author Theko
 */
public interface ResampleMethod {

    /**
     * Resamples the input audio samples into the provided output array.
     *
     * @param input the input audio samples (1D array)
     * @param output the output array where resampled samples will be stored; must have length = {@code targetLength}
     * @param targetLength the desired length of the output array (number of samples)
     */
    void resample(float[] input, float[] output, int targetLength);

    /**
     * Resamples input audio samples to a new length and returns a new array.
     *
     * @param input the input audio samples (1D array)
     * @param targetLength the desired length of the output array
     * @return a new array containing the resampled audio samples
     */
    default float[] resample(float[] input, int targetLength) {
        float[] output = new float[targetLength];
        resample(input, output, targetLength);
        return output;
    }

    /**
     * Resamples multi-channel audio samples into the provided output array.
     *
     * @param input the input audio samples (2D array, [channel][sample])
     * @param output the output array for the resampled samples; must have size = [channels][targetLength]
     * @param targetLength the desired length of each channel in the output
     */
    default void resample(float[][] input, float[][] output, int targetLength) {
        for (int ch = 0; ch < input.length; ch++) {
            resample(input[ch], output[ch], targetLength);
        }
    }

    /**
     * Resamples multi-channel audio samples and returns a new 2D array.
     *
     * @param input the input audio samples (2D array, [channel][sample])
     * @param targetLength the desired length of each channel in the output
     * @return a new 2D array containing the resampled audio samples
     */
    default float[][] resample(float[][] input, int targetLength) {
        float[][] output = new float[input.length][targetLength];
        resample(input, output, targetLength);
        return output;
    }
}