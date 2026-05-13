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
     * Retrieves a sample from a specific channel array.
     * Clamps the index to the array bounds to handle edges safely.
     * * @param channelData The sample data for a single audio channel.
     * @param index The requested sample index.
     * @return The sample value, or the nearest boundary sample if out of bounds.
     */
    private float getSample(float[] channelData, int index) {
        if (index < 0) return channelData[0];
        if (index >= channelData.length) return channelData[channelData.length - 1];
        return channelData[index];
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
    protected float interpolate(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        return 0.5f * (
            2 * p1 +
            (p2 - p0) * t +
            (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
            (3 * p1 - p0 - 3 * p2 + p3) * t3
        );
    }
}