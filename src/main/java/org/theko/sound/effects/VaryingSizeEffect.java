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

package org.theko.sound.effects;

/**
 * VaryingSizeEffect is an interface for effects that can change their output size
 * based on the required length of the input frames.
 * 
 * Implementations should provide a method to determine the target length
 * based on the required length.
 * 
 * @since 0.2.0-beta
 * @author Theko
 * 
 * @see AudioEffect
 * @see ResamplerEffect
 */
public interface VaryingSizeEffect {

    /**
     * Returns the target length of the input frames based on the required
     * length of the output frames.
     * @param outputLength The required length of the output frames
     * @return The target length of the input frames
     */
    public int getInputLength(int outputLength);

    /**
     * Returns the processed output length based on the input length.
     * @param inputLength The input length
     * @return The processed output length
     */
    public int getTargetLength(int inputLength);
}
