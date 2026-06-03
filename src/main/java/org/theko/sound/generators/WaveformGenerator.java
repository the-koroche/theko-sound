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

package org.theko.sound.generators;

import org.theko.sound.Waveform;
import org.theko.sound.util.MathUtilities;

/**
 * Represents a generator of audio samples based on a specified waveform shape.
 *
 * @since 0.3.1-beta
 * @author Theko
 */
public class WaveformGenerator extends AudioGenerator {

    private final Waveform waveform;
    private float frequency = 440;
    private float volume = 1f;
    private double phase = 0.0f;

    /**
     * Constructs a new WaveformGenerator with the specified waveform shape.
     * @param waveform the waveform shape
     */
    public WaveformGenerator(Waveform waveform) {
        this.waveform = waveform;
    }

    /**
     * @return the waveform shape of this generator.
     */
    public Waveform getWaveform() {
        return waveform;
    }

    /**
     * Sets the frequency of this generator.
     * @param frequency the frequency
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
    }

    /**
     * Sets the volume of this generator.
     * @param volume the volume
     */
    public void setVolume(float volume) {
        this.volume = volume;
    }

    /**
     * Sets the phase of this generator.
     * @param phase the phase
     */
    public void setPhase(double phase) {
        this.phase = MathUtilities.wrap(phase, 0, 2 * Math.PI);
    }

    /**
     * @return the frequency of this generator.
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * @return the volume of this generator.
     */
    public float getVolume() {
        return volume;
    }

    /**
     * @return the phase of this generator.
     */
    public double getPhase() {
        return phase;
    }

    @Override
    public void render(float[][] samples, int sampleRate) {
        double phaseStep = (2 * Math.PI * frequency) / sampleRate;

        for (int i = 0; i < samples[0].length; i++) {
            float sample = (float)(waveform.generate((float)phase) * volume);

            for (int ch = 0; ch < samples.length; ch++) {
                samples[ch][i] += sample;
            }

            phase += phaseStep;
            if (phase >= 2 * Math.PI) {
                phase -= 2 * Math.PI;
            }
        }
    }
}