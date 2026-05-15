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
 * CubicResampler implements a cubic resampling algorithm using Catmull-Rom splines.
 * This method creates a smooth curve through data points by using four local samples,
 * providing higher fidelity than linear interpolation without the complexity of Lanczos.
 *
 * @since 0.2.0-beta
 * @author Theko
 */
public class CubicResampler implements Resampler {

    @Override
    public void resample(float[][] input, float[][] output, int targetLength) {
        // Basic safety check for null or empty input and invalid target length
        if (input == null || input.length == 0 || targetLength <= 0) return;

        int numChannels = input.length;
        int sourceLength = input[0].length;

        // Special case: If target length is 1, return the first sample of the source
        if (targetLength == 1) {
            for (int ch = 0; ch < numChannels; ch++) {
                output[ch][0] = input[ch][0];
            }
            return;
        }

        // Calculate the ratio used to map target indices back to source indices
        float scale = (float) (sourceLength - 1) / (targetLength - 1);

        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i1 = (int) pos; // The base index in the source array
            float t = pos - i1; // Fractional distance between samples [0, 1]

            for (int ch = 0; ch < numChannels; ch++) {
                /*
                 * Extract 4 neighboring samples for the current channel.
                 * p1 and p2 represent the current interval; p0 and p3 are the
                 * "control points" used to determine the curve's tangents.
                 */
                float p0 = getSample(input[ch], i1 - 1);
                float p1 = getSample(input[ch], i1);
                float p2 = getSample(input[ch], i1 + 1);
                float p3 = getSample(input[ch], i1 + 2);

                output[ch][i] = interpolate(p0, p1, p2, p3, t);
            }
        }
    }

    /**
     * Retrieves the sample at the specified index,
     * or a predicted value if the index is out of bounds,
     * using Catmull-Rom spline interpolation.
     * @param data The array of samples.
     * @param index The index of the sample to retrieve.
     * @return The sample value at the specified index.
     */
    protected float getSample(float[] data, int index) {
        int last = data.length - 1;
        if (index <= last) return data[(index > 0 ? index : 0)];

        if (index == last + 1 && last >= 3) {
            float p0 = data[last];
            float p1 = data[last - 1];
            float p2 = data[last - 2];

            return extrapolate(p0, p1, p2, index);
        }
        return data[last];
    }

    /**
     * Standard Catmull-Rom spline interpolation formula.
     * Calculates the point at distance 't' between p1 and p2.
     * @param p0 The sample before the current interval.
     * @param p1 The first sample of the current interval.
     * @param p2 The second sample of the current interval.
     * @param p3 The sample after the current interval.
     * @param t The fractional distance between p1 and p2 (0 <= t <= 1).
     * @return The interpolated sample value at position 't'.
     */
    public static float interpolate(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        return 0.5f * (
            2 * p1 +
            (p2 - p0) * t +
            (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
            (3 * p1 - p0 - 3 * p2 + p3) * t3
        );
    }

    /**
     * Computes the predicted value of the sample at the specified index.
     * @param p0 The sample before the current interval.
     * @param p1 The first sample of the current interval.
     * @param p2 The second sample of the current interval.
     * @param index The index of the sample to extrapolate.
     * @return The predicted sample value at the specified index.
     */
    public static float extrapolate(float p0, float p1, float p2, int index) {
        float delta = p0 - p1;
        float curvature = (p0 - 2 * p1 + p2) * 0.5f;
        return p0 + delta + curvature * (index - 1);
    }
}