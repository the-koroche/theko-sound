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

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.resampling.ResampleMethod;
import org.theko.sound.utility.ArrayUtilities;

/**
 * ResamplerEffect is an audio effect that allows for real-time resampling of audio samples.
 * It uses a specified resampling method to adjust the speed of the audio playback.
 * 
 * This effect can be applied to audio samples to change their playback speed without altering
 * the pitch, making it useful for various audio processing tasks.
 * 
 * @since 2.0.0
 * @author Theko
 */
public class ResamplerEffect extends AudioEffect implements VaryingSizeEffect{

    protected final FloatControl speedControl = new FloatControl("Speed", 0.001f, 50.0f, 1.0f);
    protected AudioResampler resampler;

    protected final List<AudioControl> resamplerControls = List.of(speedControl);

    public ResamplerEffect(ResampleMethod method) {
        super(Type.REALTIME);
        resampler = new AudioResampler(method, AudioSystemProperties.RESAMPLER_EFFECT_QUALITY);

        addControls(resamplerControls);
    }

    public ResamplerEffect() {
        this(AudioSystemProperties.RESAMPLER_EFFECT_METHOD);
    }

    public FloatControl getSpeedControl() {
        return speedControl;
    }

    @Override
    public void effectRender (float[][] samples, int sampleRate) {
        int length = samples[0].length;

        float[][] toResample = ArrayUtilities.cutArray(samples, 0, samples.length, 0, length);
        float[][] resampled = resampler.resample(toResample, speedControl.getValue());

        int channels = Math.min(samples.length, resampled.length);
        for (int ch = 0; ch < channels; ch++) {
            int minCopy = Math.min(samples[ch].length, resampled[ch].length);
            for (int i = 0; i < minCopy; i++) {
                samples[ch][i] = resampled[ch][i];
            }
            // Zero out the rest if output is shorter than buffer
            for (int i = minCopy; i < samples[ch].length; i++) {
                samples[ch][i] = 0.0f;
            }
        }
    }

    @Override
    public int getTargetLength(int length) {
        return (int) Math.ceil(length * speedControl.getValue());
    }
}
