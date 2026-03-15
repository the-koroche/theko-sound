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

import org.theko.sound.AudioNode;

/**
 * Represents a function that can be used to build an {@link AudioEffect}.
 * <p>
 * Implementations of this interface should define the actual effect processing logic.
 * <p>
 * The {@link #render(float[][], int)} method will be called with the effect buffer and the sample rate.
 *
 * @see AudioEffect
 * @see AudioNode
 * @see #render(float[][], int)
 *
 * @since 0.3.0-beta
 * @author Theko
 */
@FunctionalInterface
public interface AudioEffectFunction extends AudioNode {

    /**
     * Renders the audio effect on the given samples.
     * <p>
     * Subclasses must implement this method to define the actual effect processing logic.
     * <p>
     * The method will be called with the effect buffer and the sample rate.
     * @param samples The audio samples to process
     * @param sampleRate The sample rate of the audio samples
     */
    @Override
    void render(float[][] samples, int sampleRate);

    /**
     * Converts this AudioEffectFunction into an AudioEffect.
     * <p>
     * This method returns an AudioEffect that delegates the rendering of the samples to this AudioEffectFunction.
     * <p>
     * The returned AudioEffect will have the given type and will ignore the enable and mix level controls.
     * <p>
     * The effectRender method of the returned AudioEffect will call the render method of this AudioEffectFunction.
     * @param type The type of the AudioEffect to create
     * @return An AudioEffect that delegates rendering to this AudioEffectFunction
     */
    default AudioEffect asEffect(AudioEffect.Type type) {
        return new AudioEffect(type) {
            @Override
            public void effectRender(float[][] samples, int sampleRate) {
                AudioEffectFunction.this.render(samples, sampleRate);
            }
        };
    }

    /**
     * Convenience method for creating an AudioEffect from this AudioEffectFunction.
     * <p>
     * This method returns an AudioEffect with the type {@link AudioEffect.Type#REALTIME}
     * and delegates the rendering of the samples to this AudioEffectFunction.
     * <p>
     * The effectRender method of the returned AudioEffect will call the render method of this AudioEffectFunction.
     * @return An AudioEffect that delegates rendering to this AudioEffectFunction with the type {@link AudioEffect.Type#REALTIME}
     */
    default AudioEffect asEffect() {
        return asEffect(AudioEffect.Type.REALTIME);
    }
}