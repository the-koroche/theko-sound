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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.theko.sound.AudioNode;
import org.theko.sound.LengthMismatchException;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.utility.SamplesUtilities;

/**
 * AudioEffect is an abstract class representing an audio effect that can be applied to audio samples.
 * It implements the AudioNode interface and provides controls for mixing level and enabling/disabling the effect.
 * 
 * @since v1.3.0
 * @author Theko
 */
public abstract class AudioEffect implements AudioNode, Controllable {

    /**
     * The type of the audio effect, which can be either REALTIME or OFFLINE_PROCESSING.
     */
    protected final Type type;

    /**
     * The default mix level for the effect, ranging from 0.0 (no effect) to 1.0 (full effect).
     * The enable control allows toggling the effect on or off.
     * If this effect is implementing VaryingSizeEffect, then
     * the mix level will be ignored, only 0.0f (no effect) or 1.0f (full effect)
     * will be used, without any in-between values.
     */
    private final FloatControl mixLevel = new FloatControl("Mix Level", 0.0f, 1.0f, 1.0f);

    /**
     * The enable control allows toggling the effect on or off.
     */
    private final BooleanControl enable = new BooleanControl("Enable", true);

    /**
     * A list of controls that are used for mixing the effect.
     * This includes the mix level and enable controls.
     */
    private final List<AudioControl> mixingControls = List.of(mixLevel, enable);

    /**
     * A list of all controls available for this audio effect.
     * This includes the mixing controls and any additional controls specific to the effect.
     */
    private final List<AudioControl> allControls = new ArrayList<>(mixingControls);

    /**
     * The type of audio effect, which can be either REALTIME or OFFLINE_PROCESSING.
     */
    public enum Type {
        REALTIME, OFFLINE_PROCESSING
    }

    /**
     * Constructs an AudioEffect with the specified type.
     * 
     * @param type The type of the audio effect, must not be null.
     */
    public AudioEffect(Type type) {
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Renders the audio effect on the provided samples.
     * This method must be implemented by subclasses to apply the effect to the audio samples.
     * 
     * @param samples The audio samples to process.
     * @param sampleRate The sample rate of the audio.
     * @param length The length of the audio samples.
     */
    @Override
    public final void render(float[][] samples, int sampleRate) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }

        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples must not be null or empty.");
        }

        try {
            SamplesUtilities.checkLength(samples);
        } catch (LengthMismatchException e) {
            throw new RuntimeException("Samples length mismatch.", e);
        }
 
        effectRender(samples, sampleRate);
    }

    protected abstract void effectRender(float[][] samples, int sampleRate);

    /**
     * Returns the type of the audio effect.
     * 
     * @return The type of the audio effect, either REALTIME or OFFLINE_PROCESSING.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the mix level control for this audio effect.
     * The mix level controls the amount of effect applied to the audio samples.
     * 
     * @return The FloatControl representing the mix level.
     */
    public FloatControl getMixLevelControl() {
        return mixLevel;
    }

    /**
     * Returns the enable control for this audio effect.
     * The enable control allows toggling the effect on or off.
     * 
     * @return The BooleanControl representing the enable state of the effect.
     */
    public BooleanControl getEnableControl() {
        return enable;
    }

    /**
     * Returns a list of all controls available for this audio effect.
     * This includes the mix level and enable controls, as well as any additional controls specific to the effect.
     * @return A list of AudioControl objects representing all controls for this effect.
     */
    @Override
    public List<AudioControl> getAllControls() {
        return Collections.unmodifiableList(allControls);
    }

    /**
     * Adds a list of controls to the list of all controls for this audio effect.
     * 
     * @param controls The list of controls to add.
     */
    protected void addControls(List<AudioControl> controls) {
        for (AudioControl control : controls) {
            if (!allControls.contains(control)) {
                allControls.add(control);
            }
        }
    }
}
