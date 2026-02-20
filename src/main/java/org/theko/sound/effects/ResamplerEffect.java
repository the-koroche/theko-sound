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

import static org.theko.sound.properties.AudioSystemProperties.RESAMPLER_EFFECT;

import org.theko.sound.controls.FloatControl;
import org.theko.sound.resamplers.AudioResampler;
import org.theko.sound.resamplers.ResampleMethod;

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

    protected final FloatControl speedControl = new FloatControl("Speed", 0.0f, 50.0f, 1.0f);
    protected AudioResampler resampler;

    public ResamplerEffect(ResampleMethod method) {
        super(Type.REALTIME);
        resampler = new AudioResampler(method);
        addEffectControl(speedControl);
    }

    public ResamplerEffect() {
        this(RESAMPLER_EFFECT);
    }

    public FloatControl getSpeedControl() {
        return speedControl;
    }

    @Override
    public void effectRender (float[][] samples, int sampleRate) {
        if (speedControl.getValue() == 1.0f) {
            return;
        }
        
        float[][] resampled = resampler.resample(samples, speedControl.getValue());

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
    public int getInputLength(int outputLength) {
        return (int) Math.ceil(outputLength * speedControl.getValue());
    }

    @Override
    public int getTargetLength(int inputLength) {
        return (int) Math.ceil(inputLength / speedControl.getValue());
    }
}
