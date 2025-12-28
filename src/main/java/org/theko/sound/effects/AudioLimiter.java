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

/**
 * The AudioLimiter class is a real-time audio effect that applies dynamic range compression
 * to audio signals. It limits the amplitude of the audio signal to prevent clipping and 
 * distortion, while also providing soft saturation for smoother transitions.
 * 
 * <p>This class uses an attack-sustain-release (ASR) envelope to control the gain reduction
 * dynamically, ensuring a natural-sounding compression effect. The limiter has configurable
 * parameters for gain, soft saturation threshold, limiter ceiling, and envelope timing.
 * 
 * <p>Usage:
 * <pre>
 * AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
 * AudioLimiter limiter = new AudioLimiter(format);
 * limiter.getGain().setValue(6.0f); // Set gain to +6 dB
 * limiter.getLimiterCeiling().setValue(-1.0f); // Set limiter ceiling to -1 dB
 * float[][] processedSamples = limiter.process(inputSamples);
 * </pre>
 * 
 * @see AudioEffect
 * @see FloatControl
 * 
 * @since 1.4.1
 * @author Theko
 */
public class AudioLimiter extends AudioEffect {
    protected final FloatControl gain = 
            new FloatControl("Gain", -24.0f, 24.0f, 0.0f); // dB
    protected final FloatControl softSaturationThreshold = 
            new FloatControl("Soft Saturation Threshold", -12.0f, 0.0f, -6.0f); // dB
    protected final FloatControl limiterCeiling = 
            new FloatControl("Limiter Ceiling", -20.0f, 0.0f, -0.1f); // dB
    protected final FloatControl envelopeAttack = 
            new FloatControl("Envelope Attack", 0.001f, 1.0f, 0.01f); // seconds
    protected final FloatControl envelopeRelease = 
            new FloatControl("Envelope Release", 0.01f, 3.0f, 0.1f); // seconds
    protected final FloatControl envelopeSustain = 
            new FloatControl("Envelope Sustain", 0.0f, 1.0f, 0.0f); // seconds

    protected final List<AudioControl> limiterControls = List.of(
        gain,
        softSaturationThreshold,
        limiterCeiling,
        envelopeAttack,
        envelopeSustain,
        envelopeRelease
    );

    public AudioLimiter() {
        super(Type.REALTIME);

        addControls(limiterControls);
    }

    public FloatControl getGain() {
        return gain;
    }

    public FloatControl getSoftSaturationThreshold() {
        return softSaturationThreshold;
    }

    public FloatControl getLimiterCeiling() {
        return limiterCeiling;
    }
    
    public FloatControl getAttack() {
        return envelopeAttack;
    }

    public FloatControl getSustain() {
        return envelopeSustain;
    }

    public FloatControl getRelease() {
        return envelopeRelease;
    }

    @Override
    protected void effectRender(float[][] samples, int sampleRate) {
        int channels = samples.length;
        int length = samples[0].length;
    
        float linearGain = (float) Math.pow(10.0, gain.getValue() / 20.0);
        float softThreshold = (float) Math.pow(10.0, softSaturationThreshold.getValue() / 20.0);
        float ceiling = (float) Math.pow(10.0, limiterCeiling.getValue() / 20.0);
    
        float attackCoeff = (float) Math.exp(-1.0 / (sampleRate * getAttack().getValue()));
        float releaseCoeff = (float) Math.exp(-1.0 / (sampleRate * getRelease().getValue()));
        float sustainSamples = getSustain().getValue() * sampleRate;
    
        float envelopeValue = 1.0f;
        float sustainCounter = 0.0f;
    
        for (int i = 0; i < length; i++) {
            // Calculate the maximum absolute value across all channels
            float maxAbs = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                float value = samples[ch][i] * linearGain;
                maxAbs = Math.max(maxAbs, Math.abs(value));
            }
    
            float gainReduction = 1.0f;
    
            if (maxAbs > softThreshold) {
                if (maxAbs > ceiling) {
                    // Hard limiter
                    gainReduction = ceiling / maxAbs;
                    sustainCounter = sustainSamples;
                } else {
                    // Soft saturation
                    float excess = (maxAbs - softThreshold) / (ceiling - softThreshold);
                    gainReduction = (float) (1.0 / (1.0 + excess * excess));
                }
            }
    
            if (gainReduction < envelopeValue) {
                envelopeValue = attackCoeff * (envelopeValue - gainReduction) + gainReduction;
                sustainCounter = sustainSamples; // Reset sustain counter on attack
            } else {
                if (sustainCounter > 0.0f) {
                    sustainCounter--;
                } else {
                    envelopeValue = releaseCoeff * (envelopeValue - 1.0f) + 1.0f;
                }
            }
    
            // Apply gain reduction to all channels
            for (int ch = 0; ch < channels; ch++) {
                samples[ch][i] *= linearGain * envelopeValue;
            }
        }
    }
}
