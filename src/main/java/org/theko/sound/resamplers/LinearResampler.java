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
                float y0 = getSample(input[ch], i0);
                float y1 = getSample(input[ch], i1);
                output[ch][i] = interpolate(y0, y1, t);
            }
        }
    }

    /**
     * Retrieves the sample at the specified index,
     * or a predicted value if the index is out of bounds,
     * using linear extrapolation.
     * @param data The array of samples.
     * @param index The index of the sample to retrieve.
     * @return The sample value at the specified index.
     */
    protected float getSample(float[] data, int index) {
        int last = data.length - 1;
        if (index <= last) return data[(index > 0 ? index : 0)];

        if (index == last + 1 && last >= 1) {
            return extrapolate(data[last], data[last - 1], index);
        }
        return data[last];
    }

    /**
     * Calculates the interpolated value using linear interpolation.
     * @param p0 The first sample.
     * @param p1 The second sample.
     * @param t The interpolation factor [0, 1].
     * @return The interpolated value.
     */
    public static float interpolate(float p0, float p1, float t) {
        return p0 + t * (p1 - p0);
    }

    /**
     * Calculates the extrapolated value using linear extrapolation.
     * @param p0 The last sample.
     * @param p1 The second-to-last sample.
     * @param index The index to extrapolate.
     * @return The extrapolated value.
     */
    public static float extrapolate(float p0, float p1, int index) {
        return p0 + (index - 1) * (p0 - p1);
    }
}