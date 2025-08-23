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

package org.theko.sound.effects;

/**
 * VaryingSizeEffect is an interface for effects that can change their output size
 * based on the required length of the input samples.
 * 
 * Implementations should provide a method to determine the target length
 * based on the required length.
 * 
 * @since 2.0.0
 * @author Theko
 * 
 * @see AudioEffect
 * @see ResamplerEffect
 */
public interface VaryingSizeEffect {

    /**
     * Returns the target length for the effect based on the required length.
     * 
     * @param requiredLength The length of the input samples that the effect will process.
     * @return The target length for the output samples after applying the effect.
     */
    public int getTargetLength (int requiredLength);
}
