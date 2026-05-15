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
public class KochanekBartelsResampler extends CubicResampler {

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

    /**
     * Retrieves the sample at the specified index,
     * or a predicted value if the index is out of bounds,
     * using Kochanek-Bartels spline interpolation.
     * @param data The array of samples.
     * @param index The index of the sample to retrieve.
     * @return The sample value at the specified index.
     */
    @Override
    protected float getSample(float[] data, int index) {
        int last = data.length - 1;
        if (index <= last) return data[(index > 0 ? index : 0)];

        if (index == last + 1 && last >= 3) {
            float p0 = data[last];
            float p1 = data[last - 1];
            float p2 = data[last - 2];

            return extrapolate(T, C, B, p0, p1, p2, index);
        }
        return data[last];
    }

    /**
     * Calculates the interpolated value using the Kochanek-Bartels formula.
     * This involves computing the incoming (m1) and outgoing (m2) tangents.
     * @param T The Tension parameter.
     * @param C The Continuity parameter.
     * @param B The Bias parameter.
     * @param p0 The sample before the current interval.
     * @param p1 The first sample of the current interval.
     * @param p2 The second sample of the current interval.
     * @param p3 The sample after the current interval.
     * @param t The fractional distance between p1 and p2 (0 <= t <= 1).
     * @return The interpolated sample value at position 't'.
     */
    public static float interpolate(float T, float C, float B, float p0, float p1, float p2, float p3, float t) {
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

    /**
     * Computes the predicted value of the sample at the specified index.
     * @param T The Tension parameter.
     * @param C The Continuity parameter.
     * @param B The Bias parameter.
     * @param p0 The sample before the current interval.
     * @param p1 The first sample of the current interval.
     * @param p2 The second sample of the current interval.
     * @param index The index of the sample to retrieve.
     * @return The predicted sample value at the specified index.
     */
    public static float extrapolate(float T, float C, float B, float p0, float p1, float p2, int index) {
        float d1 = p0 - p1;
        float d2 = p1 - p2;

        float mOut =
                (1 - T) *
                ((1 + C) * (1 + B) * d1 +
                (1 - C) * (1 - B) * d2) * 0.5f;

        return p0 + mOut;
    }
}