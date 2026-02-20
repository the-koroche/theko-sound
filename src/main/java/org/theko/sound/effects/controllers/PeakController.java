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

package org.theko.sound.effects.controllers;

import java.util.List;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.util.AudioBufferUtilities;
import org.theko.sound.util.MathUtilities;

/**
 * Represents an audio effect controller that manages the peak value of an audio signal.
 * This controller provides two controls: peak amount and peak decay speed.
 * The peak amount control determines the amount by which the peak value is scaled.
 * The peak decay speed control determines the speed at which the peak value decays over time.
 * 
 * @since 2.4.1
 * @author Theko
 */
public class PeakController extends EffectController {

    protected final FloatControl peakAmount = new FloatControl("Peak Amount", -2.0f, 2.0f, 1.0f);
    protected final FloatControl peakDecaySpeed = new FloatControl("Peak Decay Speed", 0.0f, 1.0f, 0.7f);

    protected final List<AudioControl> peakControls = List.of(peakAmount, peakDecaySpeed);

    protected float peak = 0.0f;

    public PeakController() {
        super(Type.REALTIME);
        addEffectControls(peakControls);
    }

    @Override
    protected void controllerProcess(float[][] samples, int sampleRate) {
        float value = MathUtilities.clamp(AudioBufferUtilities.getAbsMaxVolume(samples) * peakAmount.getValue(), 0.0f, 1.0f);
        peak = MathUtilities.clamp(
            Math.max(value, peak - peakDecaySpeed.getValue()),
            0.0f, 1.0f);
        getControlGroup().apply(peak);
    }


    /**
     * Returns the peak amount control.
     * This control allows adjusting the overall gain.
     * A value of 1.0 will pass the peak amount through unchanged, while a value of -2.0 will invert the peak amount.
     * 
     * @return The FloatControl representing the peak amount control.
     */
    public FloatControl getPeakAmountControl() {
        return peakAmount;
    }

    /**
     * Returns the peak decay speed control.
     * This control allows adjusting the speed at which the peak value decays.
     * A value of 0.0 will cause the peak value to never decay, while a value of 1.0 will cause the peak value to decay immediately.
     * 
     * @return The FloatControl representing the peak decay speed control.
     */
    public FloatControl getPeakDecayControl() {
        return peakDecaySpeed;
    }

    /**
     * Returns the current peak value with decay applied.
     * The peak value is the maximum volume of the audio signal in the range [0, 1],
     * where 0 represents silence and 1 represents maximum volume.
     * 
     * @return The current peak value of the peak controller.
     */
    public float getPeakValue() {
        return peak;
    }
}
