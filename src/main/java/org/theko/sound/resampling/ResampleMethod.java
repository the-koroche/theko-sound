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

package org.theko.sound.resampling;

/**
 * Interface representing a resampling method for audio data.
 * Implementations of this interface define how to resample an input array
 * of audio samples to a specified target length.
 * 
 * @since 1.4.1
 * @author Theko
 */
public interface ResampleMethod {
    
    /**
     * Resamples the input audio samples to a new length.
     * 
     * @param input The input audio samples to resample.
     * @param output The output array to store the resampled audio samples, with target length.
     * @param targetLength The target length of the resampled audio samples.
     * @param quality The quality of the resampling process.
     */
    void resample(float[] input, float[] output, int targetLength, int quality);
}
