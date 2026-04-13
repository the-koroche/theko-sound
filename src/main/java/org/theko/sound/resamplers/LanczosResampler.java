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
 * LanczosResampler implements a high-quality resampling algorithm using the Lanczos kernel.
 * It provides superior anti-aliasing and sharpness compared to linear interpolation by 
 * using a sinc-based windowed filter.
 *
 * @since 0.1.4-beta
 * @author Theko
 */
public class LanczosResampler implements Resampler {

    /** The filter size (radius) of the Lanczos kernel. Usually 2 or 3. */
    private final int a;

    /**
     * Constructs a LanczosResampler with a specific filter size.
     * @param a The kernel radius (higher values improve quality but increase CPU load).
     */
    public LanczosResampler(int a) {
        this.a = a;
    }

    /**
     * Constructs a LanczosResampler with a default radius of 3.
     */
    public LanczosResampler() {
        this(3);
    }

    @Override
    public void resample(float[][] input, float[][] output, int targetLength) {
        if (input == null || input.length == 0 || targetLength <= 0) return;

        int numChannels = input.length;
        int sourceLength = input[0].length;

        // Handle single-sample output edge case
        if (targetLength == 1) {
            for (int ch = 0; ch < numChannels; ch++) {
                output[ch][0] = input[ch][0];
            }
            return;
        }

        // Calculate the sampling ratio based on the frame count
        float scale = (float) (sourceLength - 1) / (targetLength - 1);

        for (int i = 0; i < targetLength; i++) {
            // Map the output index to the corresponding position in the source
            float centerIndex = i * scale;
            int i0 = (int) Math.floor(centerIndex);

            for (int ch = 0; ch < numChannels; ch++) {
                float sum = 0f;
                float wsum = 0f;

                /*
                 * Iterate through the neighborhood defined by the kernel radius 'a'.
                 * The window ranges from [center - a + 1] to [center + a].
                 */
                for (int j = i0 - a + 1; j <= i0 + a; j++) {
                    if (j >= 0 && j < sourceLength) {
                        // Calculate weight using the Lanczos sinc function
                        float weight = lanczosKernel(centerIndex - j, a);
                        sum += input[ch][j] * weight;
                        wsum += weight;
                    }
                }
                
                // Normalize the result by the sum of weights to maintain signal amplitude
                output[ch][i] = (wsum != 0f) ? (sum / wsum) : 0f;
            }
        }
    }

    /**
     * Computes the Lanczos kernel value for a given distance.
     * The formula is L(x) = sinc(x) * sinc(x/a).
     */
    private float lanczosKernel(float x, int a) {
        if (x == 0f) return 1f;
        if (Math.abs(x) >= a) return 0f;

        float pix = (float) (Math.PI * x);
        float pixA = pix / a;

        // Implementation of the windowed sinc function
        return (float) ((Math.sin(pix) / pix) * (Math.sin(pixA) / pixA));
    }
}