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
 * LanczosResampleMethod implements a Lanczos resampling algorithm.
 * This method uses the Lanczos kernel for interpolation,
 * known for its high-quality interpolation in audio and image processing.
 * 
 * @since 1.4.1
 * @author Theko
 */
public class LanczosResampleMethod implements ResampleMethod {
    
    @Override
    public void resample(float[] input, float[] output, int targetLength, int quality) {
        for (int i = 0; i < targetLength; i++) {
            // Compute the corresponding index in the original input array
            float index = (float) i * input.length / targetLength;
            int i0 = (int) Math.floor(index);

            output[i] = 0;

            // Perform the Lanczos interpolation with a window around the current index
            for (int j = -quality + 1; j <= quality; j++) {
                int idx = i0 + j;
                if (idx >= 0 && idx < input.length) {
                    output[i] += input[idx] * lanczosKernel(index - idx, quality);
                }
            }
        }
    }

    private float lanczosKernel(float x, int a) {
        if (x == 0) return 1; // The central sample is fully weighted
        if (Math.abs(x) >= a) return 0; // Outside the window, no contribution
        // Apply the Lanczos formula
        return (float) (Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a) / (Math.PI * Math.PI * x * x / a));
    }
}
