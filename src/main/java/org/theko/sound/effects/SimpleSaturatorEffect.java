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

import java.util.List;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;

/**
 * A simple saturator effect.
 * It uses the hyperbolic tangent function to saturate the audio signal.
 * 
 * @since 2.1.1
 * @author Theko
 */
public class SimpleSaturatorEffect extends AudioEffect {
    protected final FloatControl saturation = new FloatControl("Saturation", 0.0f, 20.0f, 1.5f);
    protected final FloatControl dry = new FloatControl("Dry", 0.0f, 1.25f, 1.0f);
    protected final FloatControl wet = new FloatControl("Wet", 0.0f, 1.25f, 0.5f);

    protected final List<AudioControl> saturatorControls = List.of(saturation, dry, wet);

    public SimpleSaturatorEffect() {
        super(Type.REALTIME);
        addEffectControls(saturatorControls);
    }

    public FloatControl getSaturation() {
        return saturation;
    }

    public FloatControl getDry() {
        return dry;
    }

    public FloatControl getWet() {
        return wet;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                float wet = (float)Math.tanh(samples[ch][i] * saturation.getValue());
                samples[ch][i] = wet * dry.getValue() + samples[ch][i] * dry.getValue();
            }
        }
    }
}
