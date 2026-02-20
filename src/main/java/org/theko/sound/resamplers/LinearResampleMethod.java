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
 * The LinearResampler class implements the ResamplerMethod interface and provides
 * functionality to resample an input array of floating-point audio samples to a
 * specified target length using linear interpolation.
 * 
 * @since 1.4.1
 * @author Theko
 */
public class LinearResampleMethod implements ResampleMethod {
    
    @Override
    public void resample(float[] input, float[] output, int targetLength) {
        float scale = (float) input.length / targetLength;
        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i0 = (int) pos;
            float t = pos - i0;
            
            if (i0 < 0) {
                output[i] = input[0];
            } else if (i0 >= input.length - 1) {
                output[i] = input[input.length - 1];
            } else {
                output[i] = input[i0] * (1 - t) + input[i0 + 1] * t;
            }
        }
    }
}
