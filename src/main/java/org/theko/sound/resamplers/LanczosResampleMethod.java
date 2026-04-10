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
 * LanczosResampleMethod implements a Lanczos resampling algorithm.
 * This method uses the Lanczos kernel for interpolation,
 * known for its high-quality interpolation in audio and image processing.
 *
 * @since 0.1.4-beta
 * @author Theko
 */
public class LanczosResampleMethod implements ResampleMethod {

    private final int a;

    public LanczosResampleMethod(int a) {
        this.a = a;
    }

    public LanczosResampleMethod() {
        this(3);
    }

    @Override
    public void resample(float[] input, float[] output, int targetLength) {
        if (targetLength <= 0) return;
        if (targetLength == 1) {
            output[0] = input[0];
            return;
        }
        int inLen = input.length;

        for (int i = 0; i < targetLength; i++) {
            // Compute the corresponding index in the original input array
            float index = (float) i * (inLen - 1) / (targetLength - 1);
            int i0 = (int) Math.floor(index);

            // Perform the Lanczos interpolation with a window around the current index
            float sum = 0f;
            float wsum = 0f;

            for (int j = -a; j <= a; j++) {
                int idx = i0 + j;
                if (idx >= 0 && idx < inLen) {
                    float w = lanczosKernel(index - idx, a);
                    sum += input[idx] * w;
                    wsum += w;
                }
            }
            output[i] = wsum != 0f ? (sum / wsum) : 0f;
        }
    }

    private float lanczosKernel(float x, int a) {
        if (x == 0f) return 1f; // The central sample is fully weighted
        if (Math.abs(x) >= a) return 0f; // Outside the window

        float pix = (float) (Math.PI * x);
        float pixA = pix / a;

        return (float) ((Math.sin(pix) / pix) * (Math.sin(pixA) / pixA));
    }
}
