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
     * @param input The input audio samples as a float array.
     * @param targetLength The desired length of the output audio samples.
     * @param quality The quality of the resampling process, typically an integer value.
     * @return A float array containing the resampled audio samples.
     */
    float[] resample (float[] input, int targetLength, int quality);
}
