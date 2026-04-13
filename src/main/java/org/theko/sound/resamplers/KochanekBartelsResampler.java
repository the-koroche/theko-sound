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
 * KochanekBartelsResampler implements a cubic spline interpolation using 
 * Tension, Continuity, and Bias (TCB) parameters. 
 * This allows for fine-grained control over the "shape" of the audio waveform 
 * during resampling.
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public class KochanekBartelsResampler implements Resampler {

    private final float T; // Tension: Adjusts how sharply the curve bends
    private final float C; // Continuity: Adjusts the sharpness of changes in velocity
    private final float B; // Bias: Shifts the direction of the curve

    /**
     * Constructs a resampler with specific TCB parameters.
     */
    public KochanekBartelsResampler(float tension, float continuity, float bias) {
        this.T = tension;
        this.C = continuity;
        this.B = bias;
    }

    /**
     * Default constructor. 
     * Setting T, C, and B to 0 is mathematically equivalent to a Catmull-Rom spline.
     */
    public KochanekBartelsResampler() {
        this(0f, 0f, 0f); 
    }

    @Override
    public void resample(float[][] input, float[][] output, int targetLength) {
        if (input == null || input.length == 0 || targetLength <= 0) return;

        int numChannels = input.length;
        int sourceLength = input[0].length;

        // Handle single-sample output case
        if (targetLength == 1) {
            for (int ch = 0; ch < numChannels; ch++) {
                output[ch][0] = input[ch][0];
            }
            return;
        }

        // Scale factor based on total frame count
        float scale = (float) (sourceLength - 1) / (targetLength - 1);

        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i1 = (int) pos; // The primary sample index
            float t = pos - i1; // The fractional offset between samples

            for (int ch = 0; ch < numChannels; ch++) {
                // Cubic interpolation requires 4 points: p0 (previous), p1, p2 (current span), p3 (next)
                float p0 = getSample(input[ch], i1 - 1);
                float p1 = getSample(input[ch], i1);
                float p2 = getSample(input[ch], i1 + 1);
                float p3 = getSample(input[ch], i1 + 2);

                output[ch][i] = tcb(p0, p1, p2, p3, t);
            }
        }
    }

    /**
     * Safely retrieves a sample from the channel data, clamping indices 
     * to the first or last sample to avoid bounds errors.
     */
    private float getSample(float[] channelData, int index) {
        if (index < 0) return channelData[0];
        if (index >= channelData.length) return channelData[channelData.length - 1];
        return channelData[index];
    }

    /**
     * Calculates the interpolated value using the Kochanek-Bartels formula.
     * This involves computing the incoming (m1) and outgoing (m2) tangents.
     */
    private float tcb(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        // Calculate incoming (m1) and outgoing (m2) tangents based on TCB parameters
        float m1 = (1 - T) * (1 + C) * (1 + B) * (p1 - p0) / 2 +
                   (1 - T) * (1 - C) * (1 - B) * (p2 - p1) / 2;

        float m2 = (1 - T) * (1 - C) * (1 + B) * (p2 - p1) / 2 +
                   (1 - T) * (1 + C) * (1 - B) * (p3 - p2) / 2;

        // Standard Hermite basis functions to find the point along the curve
        return (2 * t3 - 3 * t2 + 1) * p1 +
               (t3 - 2 * t2 + t) * m1 +
               (-2 * t3 + 3 * t2) * p2 +
               (t3 - t2) * m2;
    }
}