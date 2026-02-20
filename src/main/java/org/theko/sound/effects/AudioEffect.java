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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.theko.sound.AudioNode;
import org.theko.sound.LengthMismatchException;
import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.BooleanControl;
import org.theko.sound.controls.Controllable;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.samples.SamplesValidation;

/**
 * AudioEffect is an abstract class representing an audio effect that can be applied to audio samples.
 * It implements the AudioNode interface and provides controls for mixing level and enabling/disabling the effect.
 * 
 * @since 0.1.3-beta
 * @author Theko
 */
public abstract class AudioEffect implements AudioNode, Controllable {

    /**
     * The type of the audio effect, which can be either REALTIME or OFFLINE_PROCESSING.
     */
    protected final Type type;

    /**
     * The default mix level for the effect, ranging from 0.0 (no effect) to 1.0 (full effect).
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
     * Renders the audio effect on the given samples, without mixing the effect with the original samples.
     * <p>
     * If the sample rate is not positive, an IllegalArgumentException will be thrown.
     * <p>
     * If the samples have different lengths, a RuntimeException with a LengthMismatchException will be thrown.
     * <p>
     * The effectRender method will be called with the effect buffer and the sample rate.
     * @param samples The audio samples to process
     * @param sampleRate The sample rate of the audio samples
     * @throws IllegalArgumentException If the sample rate is not positive
     * @throws RuntimeException If the samples have different lengths, with a LengthMismatchException as the cause
     */
    @Override
    public final void render(float[][] samples, int sampleRate) throws IllegalArgumentException {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }

        SamplesValidation.validateSamples(samples);

        if (!SamplesValidation.checkLength(samples)) {
            throw new RuntimeException(new LengthMismatchException("Samples length must be the same for all channels."));
        }

        effectRender(samples, sampleRate);
    }

    /**
     * Renders the audio effect on the given samples.
     * <p>
     * If the effect is disabled or the mix level is 0.0, the samples will not be processed.
     * <p>
     * If the sample rate is not positive, an IllegalArgumentException will be thrown.
     * <p>
     * If the samples have different lengths, a RuntimeException with a LengthMismatchException will be thrown.
     * <p>
     * If the effect should be mixed with the original samples (i.e. the mix level is less than 1.0), then
     * a copy of the input samples will be created and the effect will be applied on the copy.
     * The effect buffer will then be mixed back with the original samples.
     * <p>
     * The effectRender method will be called with the effect buffer and the sample rate.
     * @param samples The audio samples to process
     * @param sampleRate The sample rate of the audio samples
     * @throws IllegalArgumentException If the sample rate is not positive
     * @throws RuntimeException If the samples have different lengths, with a LengthMismatchException as the cause
     */
    public final void renderWithMixing(float[][] samples, int sampleRate) {
        if (!enable.isEnabled() || mixLevel.getValue() <= 0.0f) {
            return; // Effect is disabled, do not process samples
        }
        boolean shouldMix = mixLevel.getValue() < 1.0f;

        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }

        SamplesValidation.validateSamples(samples);

        if (!SamplesValidation.checkLength(samples)) {
            throw new RuntimeException(new LengthMismatchException("Samples length must be the same for all channels."));
        }
 
        float[][] effectBuffer = samples;
        if (shouldMix) {
            // Create a copy of the input samples to apply the effect on, so we can mix it back with the original samples later
            effectBuffer = new float[samples.length][];
            for (int ch = 0; ch < samples.length; ch++) {
                effectBuffer[ch] = new float[samples[ch].length];
                System.arraycopy(samples[ch], 0, effectBuffer[ch], 0, samples[ch].length);
            }
        }
        effectRender(effectBuffer, sampleRate);
        if (shouldMix) {
            // Mix the effect buffer back with the original samples
            float mixLevelValue = mixLevel.getValue();
            for (int ch = 0; ch < samples.length; ch++) {
                for (int i = 0; i < samples[ch].length; i++) {
                    samples[ch][i] = (samples[ch][i] * (1.0f - mixLevelValue)) + (effectBuffer[ch][i] * mixLevelValue);
                }
            }
        }
    }
    
    /**
     * Applies the specific audio effect to the provided samples.
     * This method is called by the {@link #render(float[][], int)} or {@link #renderWithMixing(float[][], int)}
     * method after validating the input samples.
     * Subclasses must implement this method to define the actual effect processing logic.
     * 
     * @param samples The audio samples to process.
     * @param sampleRate The sample rate of the audio.
     */
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
     * @throws IllegalArgumentException if the controls list is null or contains null elements.
     */
    protected void addEffectControls(List<AudioControl> controls) {
        if (controls == null) {
            throw new IllegalArgumentException("Controls list cannot be null.");
        }
        for (AudioControl control : controls) {
            addEffectControl(control);
        }
    }

    /**
     * Adds a control to the list of all controls for this audio effect.
     * 
     * @param control The control to add.
     * @return true if the control was added successfully, false if the control was already present in the list.
     * @throws IllegalArgumentException if the control is null.
     */
    protected boolean addEffectControl(AudioControl control) {
        if (control == null) {
            throw new IllegalArgumentException("Control cannot be null.");
        }
        if (!allControls.contains(control)) {
            allControls.add(control);
            return true;
        }
        return false;
    }
}
