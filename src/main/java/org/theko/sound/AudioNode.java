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

package org.theko.sound;

/**
 * Represents a node in the audio processing graph.
 * <p>Each node can process audio samples and is responsible for rendering
 * its output into the provided sample buffer.
 * 
 * @since 0.2.0-beta
 * @author Theko
 * 
 * @see org.theko.sound.AudioMixer
 * @see org.theko.sound.effects.AudioEffect
 */
public interface AudioNode {
    
    /**
     * Renders the audio node's output into the provided sample buffer.
     * 
     * @param samples The sample buffer to render into.
     * @param sampleRate The sample rate of the audio.
     */
    public void render(float[][] samples, int sampleRate);
}
