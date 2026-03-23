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

package org.theko.sound.visualizers;

import org.theko.sound.controls.FloatControl;

/**
 * An abstract base class for audio visualizers that provides a gain control.
 * <p>
 * The gain control allows adjusting the overall volume of the audio visualizer,
 * used to adjust the volume of the input.
 * A value of 1.0 will not change the volume, while a value of 0.0 will mute it.
 *
 * @author Theko
 * @since 0.3.0-beta
 */
public abstract class GainedAudioVisualizer extends AudioVisualizer {

    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 10.0f, 1.0f);
    
    public GainedAudioVisualizer(Type type, float frameRate, int resizeDelayMs) {
        super(type, frameRate, resizeDelayMs);
        addEffectControl(gainControl);
    }

    public GainedAudioVisualizer(Type type, float frameRate) {
        super(type, frameRate);
        addEffectControl(gainControl);
    }

    public GainedAudioVisualizer(Type type) {
        super(type);
        addEffectControl(gainControl);
    }

    /**
     * Returns the gain control for this audio visualizer.
     * The gain control allows adjusting the overall volume of the audio visualizer.
     * A value of 1.0 will not change the volume, while a value of 0.0 will mute it.
     *
     * @return The FloatControl representing the gain control of the audio visualizer
     */
    public FloatControl getGainControl() {
        return gainControl;
    }
}
