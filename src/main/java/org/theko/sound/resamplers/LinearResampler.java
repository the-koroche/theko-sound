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
 * LinearResampler implements the Resampler interface using linear interpolation.
 * This method estimates values between existing samples by drawing a straight line 
 * between them, providing a smoother result than nearest-neighbor resampling.
 *
 * @since 0.1.4-beta
 * @author Theko
 */
public class LinearResampler implements Resampler {

    @Override
    public void resample(float[][] input, float[][] output, int targetLength) {
        // Validate input and target dimensions to prevent errors
        if (input == null || input.length == 0 || targetLength <= 0) return;

        int numChannels = input.length;
        int sourceLength = input[0].length;

        // Edge case: For a single target sample, simply take the first source sample
        if (targetLength == 1) {
            for (int ch = 0; ch < numChannels; ch++) {
                output[ch][0] = input[ch][0];
            }
            return;
        }

        // Calculate the ratio between the source and target intervals
        float scale = (float) (sourceLength - 1) / (targetLength - 1);

        for (int i = 0; i < targetLength; i++) {
            // Determine the floating-point position in the source array
            float pos = i * scale;
            
            // Identify the two surrounding sample indices (left and right)
            int i0 = (int) pos;
            int i1 = i0 + 1;
            
            // Calculate the fractional distance between the two samples [0, 1]
            float t = pos - i0;

            for (int ch = 0; ch < numChannels; ch++) {
                if (i0 < 0) {
                    // Handle potential negative index due to float precision
                    output[ch][i] = input[ch][0];
                } else if (i0 >= sourceLength - 1) {
                    // Cap the index at the last available sample
                    output[ch][i] = input[ch][sourceLength - 1];
                } else {
                    // Perform linear interpolation: y = y0 + t * (y1 - y0)
                    // This creates a weighted average based on the distance 't'
                    output[ch][i] = input[ch][i0] * (1 - t) + input[ch][i1] * t;
                }
            }
        } 
    }
}