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

import java.util.List;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.util.SamplesUtilities;

/**
 * Represents an audio effect that allows for real-time audio positioning
 * through gain and pan controls.
 * 
 * This effect can be applied to audio samples to adjust their perceived
 * position in the stereo field.
 * 
 * @author Theko
 * @since 2.1.0
 */
public class AudioPositioningEffect extends AudioEffect {

    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    protected final FloatControl panControl = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);

    protected final List<AudioControl> positioningControls = List.of(
        gainControl,
        panControl
    );

    public AudioPositioningEffect() {
        super(Type.REALTIME);
        addEffectControls(positioningControls);
    }

    public FloatControl getGain() {
        return gainControl;
    }

    public FloatControl getPan() {
        return panControl;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        SamplesUtilities.adjustGainAndPan(samples, gainControl.getValue(), panControl.getValue());
    }
}