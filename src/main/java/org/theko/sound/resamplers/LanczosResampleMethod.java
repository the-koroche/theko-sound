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

    private final int a;

    public LanczosResampleMethod(int a) {
        this.a = a;
    }

    public LanczosResampleMethod() {
        this(3);
    }
    
    @Override
    public void resample(float[] input, float[] output, int targetLength) {
        int inLen = input.length;

        for (int i = 0; i < targetLength; i++) {
            // Compute the corresponding index in the original input array
            float index = (i + 0.5f) * inLen / targetLength - 0.5f;
            int i0 = (int) Math.floor(index);

            float sum = 0f;

            // Perform the Lanczos interpolation with a window around the current index
            for (int j = -a; j <= a; j++) {
                int idx = i0 + j;
                if (idx >= 0 && idx < inLen) {
                    sum += input[idx] * lanczosKernel(index - idx, a);
                }
            }
            output[i] = sum;
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
