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

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.AudioControlGroup;
import org.theko.sound.effects.AudioEffect;

/**
 * Abstract class representing an audio effect controller.
 * It is used when a control group is managed by the effect.
 * 
 * @since 0.2.4-beta
 * @author Theko
 */
public abstract class EffectController extends AudioEffect {

    protected final AudioControlGroup controlGroup = new AudioControlGroup();

    public EffectController(Type type) {
        super(type);
    }

    /**
     * Adds a control to the group.
     * If the control type is not Float, Boolean or Enum, it is ignored.
     * 
     * @param control The control to add.
     */
    public void addControl(AudioControl control) {
        controlGroup.addControl(control);
    }

    /**
     * Removes a control from the group.
     * 
     * @param control The control to remove.
     */
    public void removeControl(AudioControl control) {
        controlGroup.removeControl(control);
    }

    /**
     * Clears all controls from the group.
     * This method is used to remove all controls managed by the effect controller.
     */
    public void clearControls() {
        controlGroup.clearControls();
    }

    @Override
    protected void effectRender(float[][] samples, int sampleRate) {
        controllerProcess(samples, sampleRate);
    }

    /**
     * This method is called by the effectRender method and should be implemented
     * by subclasses to process the audio samples and apply value to the controls.
     * 
     * @param samples The audio samples to process.
     * @param sampleRate The sample rate of the audio.
     */
    protected abstract void controllerProcess(float[][] samples, int sampleRate);

    public AudioControlGroup getControlGroup() {
        return controlGroup;
    }
}
