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

import org.theko.sound.control.FloatControl;
import org.theko.sound.control.AudioControl;

/**
 * BitcrusherEffect is an audio effect that reduces the bit depth and sample rate of audio samples,
 * creating a lo-fi, distorted sound characteristic of bitcrushing.
 * 
 * This effect can be applied to audio samples in real-time, allowing for creative sound design
 * and manipulation.
 * 
 * @author Theko
 * @since 2.0.0
 */
public class BitcrusherEffect extends AudioEffect {

    protected static final float BASE_SAMPLE_RATE = 22000.0f; // Base sample rate for bitcrusher

    protected final FloatControl bitdepth = new FloatControl("Bit Depth", 1, 16, 4);
    protected final FloatControl sampleRateReduction = new FloatControl("Sample Rate Reduction", 50f, 22000f, 2000f);

    protected final List<AudioControl> bitcrusherControls = List.of(
        bitdepth,
        sampleRateReduction
    );

    public BitcrusherEffect() {
        super(Type.REALTIME);

        addControls(bitcrusherControls);
    }

    public FloatControl getBitdepth() {
        return bitdepth;
    }

    public FloatControl getSampleRateReduction() {
        return sampleRateReduction;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        int channels = samples.length;

        float targetRate = sampleRateReduction.getValue();
        float sampleStep = BASE_SAMPLE_RATE / targetRate;

        int bitDepth = (int) bitdepth.getValue();
        int levels = (1 << bitDepth) - 1;

        for (int ch = 0; ch < channels; ch++) {
            float[] channel = samples[ch];

            float heldSample = 0.0f;
            float sampleCounter = 0.0f;

            for (int i = 0; i < channel.length; i++) {
                if (sampleCounter <= 0.0f) {
                    heldSample = Math.round(channel[i] * levels) / (float) levels;
                    sampleCounter += sampleStep;
                }

                channel[i] = heldSample;
                sampleCounter -= 1.0f;
            }
        }
    }
}
