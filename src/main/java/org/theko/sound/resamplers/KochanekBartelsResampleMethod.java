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
 * KochanekBartelsResampleMethod implements cubic interpolation using
 * Tension, Continuity, and Bias (TCB) parameters.
 *
 * T  - controls tightness (0 = normal)
 * C  - controls smoothness
 * B  - controls bias of the curve
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public class KochanekBartelsResampleMethod implements ResampleMethod {

    private final float T; // Tension
    private final float C; // Continuity
    private final float B; // Bias

    /**
     * Creates a Kochanek-Bartels resampler with tension, continuity, and bias parameters.
     *
     * @param tension controls how tight the curve is around points (0 = Catmull-Rom, higher = tighter, lower = looser)
     * @param continuity controls how smooth the transition is between segments (-1 to 1, affects tangent continuity)
     * @param bias controls the direction of the curve (-1 favors previous point, 1 favors next point)
     */
    public KochanekBartelsResampleMethod(float tension, float continuity, float bias) {
        this.T = tension;
        this.C = continuity;
        this.B = bias;
    }

    /**
     * Creates a Kochanek-Bartels resampler with default parameters of T = C = B = 0 (Catmull-Rom).
     */
    public KochanekBartelsResampleMethod() {
        this(0f, 0f, 0f); // Catmull-Rom equivalent
    }

    @Override
    public void resample(float[] input, float[] output, int targetLength) {
        if (targetLength <= 0) return;
        if (targetLength == 1) {
            output[0] = input[0];
            return;
        }
        float scale = (float) (input.length - 1) / (targetLength - 1);

        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i1 = (int) pos;
            float t = pos - i1;

            float p0 = getSample(input, i1 - 1);
            float p1 = getSample(input, i1);
            float p2 = getSample(input, i1 + 1);
            float p3 = getSample(input, i1 + 2);

            output[i] = tcb(p0, p1, p2, p3, t);
        }
    }

    private float getSample(float[] arr, int i) {
        if (i < 0) return arr[0];
        if (i >= arr.length) return arr[arr.length - 1];
        return arr[i];
    }

    private float tcb(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        // Tension, Continuity, Bias influence tangents
        float m1 =
                (1 - T) * (1 + C) * (1 + B) * (p1 - p0) / 2 +
                (1 - T) * (1 - C) * (1 - B) * (p2 - p1) / 2;

        float m2 =
                (1 - T) * (1 - C) * (1 + B) * (p2 - p1) / 2 +
                (1 - T) * (1 + C) * (1 - B) * (p3 - p2) / 2;

        return
                (2 * t3 - 3 * t2 + 1) * p1 +
                (t3 - 2 * t2 + t) * m1 +
                (-2 * t3 + 3 * t2) * p2 +
                (t3 - t2) * m2;
    }
}