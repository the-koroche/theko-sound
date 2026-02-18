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
 * NearestResampleMethod implements a nearest neighbor resampling algorithm.
 * It simply copies the nearest sample from the input array to the output array.
 *
 * @since 2.3.2
 * @author Theko
 */
public class NearestResampleMethod implements ResampleMethod {

    @Override
    public void resample(float[] input, float[] output, int targetLength, int quality) {
        int inputLength = input.length;

        for (int i = 0; i < targetLength; i++) {
            int nearestIndex = Math.round((i * (inputLength - 1f)) / (targetLength - 1f));
            nearestIndex = Math.min(nearestIndex, inputLength - 1);
            output[i] = input[nearestIndex];
        }
    }
}