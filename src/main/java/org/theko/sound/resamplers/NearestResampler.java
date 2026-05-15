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

/**
 * NearestResampler implements a nearest-neighbor resampling algorithm.
 * This method maps the output sample index to the closest corresponding
 * index in the source array, providing a fast but "blocky" interpolation.
 *
 * @since 0.2.3-beta
 * @author Theko
 */
public class NearestResampler implements Resampler {

    @Override
    public void resample(float[][] input, float[][] output, int targetLength) {

        // Early exit if the target length is invalid
        if (targetLength <= 0) return;

        int inputLength = input[0].length;

        // Edge case: If only one sample is requested, take the first sample from the input
        if (targetLength == 1) {
            for (int ch = 0; ch < input.length; ch++) {
                output[ch][0] = input[ch][0];
            }
            return;
        }

        // Main resampling loop
        for (int i = 0; i < targetLength; i++) {
            // Calculate the nearest source index using floating-point math
            // to maintain ratio precision, then rounding to the closest integer.
            int nearestIndex = Math.round((i * (inputLength - 1f)) / (targetLength - 1f));

            // Ensure the index doesn't exceed the bounds of the input array
            nearestIndex = Math.min(nearestIndex, inputLength - 1);

            // Copy the sample from the nearest index for all available channels
            for (int ch = 0; ch < input.length; ch++) {
                output[ch][i] = input[ch][nearestIndex];
            }
        }
    }
}