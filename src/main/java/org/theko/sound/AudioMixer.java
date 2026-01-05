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

package org.theko.sound;

import static org.theko.sound.properties.AudioSystemProperties.MIXER_DEFAULT_ENABLE_EFFECTS;
import static org.theko.sound.properties.AudioSystemProperties.MIXER_DEFAULT_REVERSE_POLARITY;
import static org.theko.sound.properties.AudioSystemProperties.MIXER_DEFAULT_SWAP_CHANNELS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.AudioEffect;
import org.theko.sound.effects.IncompatibleEffectTypeException;
import org.theko.sound.effects.MultipleVaryingSizeEffectsException;
import org.theko.sound.effects.VaryingSizeEffect;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.utility.ArrayUtilities;
import org.theko.sound.utility.MathUtilities;
import org.theko.sound.utility.SamplesUtilities;


/**
 * The {@code AudioMixer} class implements an audio mixing node that combines multiple {@link AudioNode} inputs,
 * applies a chain of {@link AudioEffect}s, and provides various controls for gain, pan, stereo separation,
 * channel swapping, and polarity reversal. It supports both fixed-size and varying-size effects, and ensures
 * proper handling of input and output buffer lengths and channel counts.
 * <p>
 * Main features:
 * <ul>
 *   <li>Add and remove multiple audio inputs and effects.</li>
 *   <li>Support for pre-gain and post-gain adjustment, pan, and stereo separation.</li>
 *   <li>Enable or disable effects processing, swap channels, and reverse polarity.</li>
 *   <li>Handles effects that require varying input/output buffer sizes.</li>
 *   <li>Ensures thread-safe and robust error handling for input and effect management.</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 *     AudioMixer mixer = new AudioMixer();
 *     mixer.addInput(audioNode1);
 *     mixer.addInput(audioNode2);
 *     mixer.addEffect(reverbEffect);
 *     float[][] buffer = new float[2][1024];
 *     mixer.render(buffer, 44100, 1024);
 * </pre>
 *
 * @see AudioNode
 * @see AudioEffect
 * @see FloatControl
 * @see BooleanControl
 * 
 * @author Theko
 * @since 2.0.0
 */
public class AudioMixer implements AudioNode {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioMixer.class);

    private final List<AudioNode> inputs = new ArrayList<>();
    private final List<AudioEffect> effects = new ArrayList<>();

    private final FloatControl preGainControl = new FloatControl("Pre-Gain", 0.0f, 2.0f, 1.0f);
    private final FloatControl postGainControl = new FloatControl("Post-Gain", 0.0f, 2.0f, 1.0f);
    private final FloatControl panControl = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);
    private final FloatControl stereoSeparationControl = new FloatControl("Stereo Separation", -1.0f, 1.0f, 0.0f);

    private final BooleanControl enableEffectsControl = new BooleanControl("Enable Effects", MIXER_DEFAULT_ENABLE_EFFECTS);
    private final BooleanControl swapChannelsControl = new BooleanControl("Swap Channels", MIXER_DEFAULT_SWAP_CHANNELS);
    private final BooleanControl reversePolarityControl = new BooleanControl("Reverse Polarity", MIXER_DEFAULT_REVERSE_POLARITY);

    private static final float STEREO_SEP_EPSILON = 0.000001f;

    private CollectedInputs collectedInputs = null;
    private float[][][] inputBuffers = null;
    private boolean[] validInputs = null;
    
    private float[][] mixedBuffer = null;
    private float[][] effectBuffer = null;

    /**
     * Adds an audio input to the mixer.
     * 
     * @param input The audio input to add.
     * @throws IllegalArgumentException If the input is null, an effect, this mixer itself, or a circular reference.
     */
    public void addInput(AudioNode input) {
        if (input == null) {
            logger.error("Attempted to add null input to AudioMixer");
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input instanceof AudioEffect) {
            logger.error("Attempted to add an effect ({}) as an input to AudioMixer", input.getClass().getSimpleName());
            throw new IllegalArgumentException("Input cannot be an effect");
        }
        if (input.equals(this)) {
            logger.error("Attempted to add self as an input to AudioMixer");
            throw new IllegalArgumentException("Input cannot be self");
        }

        // Check for circular reference
        if (input instanceof AudioMixer) {
            AudioMixer mixerInput = (AudioMixer) input;
            if (mixerInput.hasMixer(this)) {
                logger.error("Detected circular reference when trying to add mixer {} to {}", 
                            mixerInput.getClass().getSimpleName(), this.getClass().getSimpleName());
                throw new IllegalArgumentException("Circular mixer reference detected");
            }
        }

        inputs.add(input);
    }

    private boolean hasMixer(AudioMixer mixer) {
        for (AudioNode input : inputs) {
            if (input.equals(mixer)) {
                return true;
            }
            if (input instanceof AudioMixer) {
                AudioMixer inputMixer = (AudioMixer) input;
                if (inputMixer.hasMixer(mixer)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds an audio effect to the mixer.
     * 
     * @param effect The audio effect to add.
     * @throws IncompatibleEffectTypeException if the effect is an offline processing effect.
     * @throws MultipleVaryingSizeEffectsException if the effect is a varying size effect and the mixer already has a varying size effect.
     */
    public void addEffect(AudioEffect effect) throws IncompatibleEffectTypeException, MultipleVaryingSizeEffectsException {
        if (effect == null) {
            logger.error("Attempted to add null effect to AudioMixer");
            throw new IllegalArgumentException("Effect cannot be null");
        }
        if (effect.getType() == AudioEffect.Type.OFFLINE_PROCESSING) {
            logger.error("Attempted to add an offline processing effect ({}) to AudioMixer", effect.getClass().getSimpleName());
            throw new IncompatibleEffectTypeException("Offline processing effect cannot be added to AudioMixer");
        }
        if (effect instanceof VaryingSizeEffect && hasVaryingSizeEffect()) {
            logger.error("Attempted to add multiple varying size effects to AudioMixer");
            throw new MultipleVaryingSizeEffectsException();
        }
        effects.add(effect);
    }

    /**
     * Checks if the mixer has a varying size effect.
     * 
     * @return True if the mixer has a varying size effect, false otherwise.
     */
    protected boolean hasVaryingSizeEffect() {
        return getVaryingSizeEffectIndex() != -1;
    }

    /**
     * Gets the index of the varying size effect in the effects list.
     * 
     * @return The index of the varying size effect, or -1 if not found.
     */
    protected int getVaryingSizeEffectIndex() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i) instanceof VaryingSizeEffect) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes an audio input from the mixer.
     * 
     * @param input The audio input to remove.
     * @throws IllegalArgumentException If the input is null.
     */
    public boolean removeInput(AudioNode input) {
        if (input == null) {
            logger.error("Attempted to remove null input from AudioMixer");
            throw new IllegalArgumentException("Input cannot be null");
        }
        return inputs.remove(input);
    }

    /**
     * Removes an audio effect from the mixer.
     * 
     * @param effect The audio effect to remove.
     * @throws IllegalArgumentException If the effect is null.
     */
    public boolean removeEffect(AudioEffect effect) {
        if (effect == null) {
            logger.error("Attempted to remove null effect from AudioMixer");
            throw new IllegalArgumentException("Effect cannot be null");
        }
        return effects.remove(effect);
    }

    /**
     * Gets the audio inputs of the mixer.
     * 
     * @return the unmodifiable list of audio inputs.
     */
    public List<AudioNode> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /**
     * Gets the audio effects of the mixer.
     * 
     * @return the unmodifiable list of audio effects.
     */
    public List<AudioEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /**
     * Gets the pre-gain control of the mixer.
     * Pre-gain control adjusts the gain of the input audio before it is mixed.
     * 
     * @return the pre-gain control.
     */
    public FloatControl getPreGainControl() {
        return preGainControl;
    }

    /**
     * Gets the post-gain control of the mixer.
     * Post-gain control adjusts the gain of the mixed audio after it is processed by effects.
     * 
     * @return the post-gain control.
     */
    public FloatControl getPostGainControl() {
        return postGainControl;
    }

    /**
     * Gets the pan control of the mixer.
     * Pan control adjusts the pan of the mixed audio.
     * 
     * @return the pan control.
     */
    public FloatControl getPanControl() {
        return panControl;
    }

    /**
     * Gets the stereo separation control of the mixer.
     * Stereo separation control adjusts the stereo separation of the mixed audio.
     * 
     * @return the stereo separation control.
     */
    public FloatControl getStereoSeparationControl() {
        return stereoSeparationControl;
    }

    /**
     * Gets the enable effects control of the mixer.
     * Enable effects control enables or disables effects processing.
     * 
     * @return the enable effects control.
     */
    public BooleanControl getEnableEffectsControl() {
        return enableEffectsControl;
    }

    /**
     * Gets the swap channels control of the mixer.
     * Swap channels control swaps the channels of the mixed audio.
     * 
     * @return the swap channels control.
     */
    public BooleanControl getSwapChannelsControl() {
        return swapChannelsControl;
    }

    /**
     * Gets the reverse polarity control of the mixer.
     * Reverse polarity control reverses the polarity of the mixed audio.
     * 
     * @return the reverse polarity control.
     */
    public BooleanControl getReversePolarityControl() {
        return reversePolarityControl;
    }

    /**
     * A class representing collected inputs.
     */
    private static class CollectedInputs {
        public float[][][] inputs;
        public boolean[] validInputs;
    }

    /**
     * Renders the mixed audio into the provided sample buffer.
     * 
     * @param samples The sample buffer to render into.
     * @param sampleRate The sample rate of the audio.
     * @throws MixingException If an error occurs while mixing the audio.
     * @throws IllegalArgumentException If the sample rate is not positive.
     */
    @Override
    public void render(float[][] samples, int sampleRate) throws MixingException, IllegalArgumentException {
        if (sampleRate <= 0) {
            logger.error("Sample rate must be positive.");
            throw new IllegalArgumentException("Sample rate must be positive.");
        }

        SamplesValidation.validateSamples(samples);

        if (!SamplesValidation.checkLength(samples)) {
            logger.error("Samples length must be the same for all channels.");
            throw new MixingException(new LengthMismatchException("Samples length must be the same for all channels."));
        }

        int outputLength = samples[0].length;

        boolean enableEffects = enableEffectsControl.isEnabled();
        int inputLength = outputLength;
        try {
            inputLength = getTargetInputLength(outputLength);
        } catch (LengthMismatchException ex) {
            logger.error("Input length is not valid.", ex);
            throw new MixingException(ex);
        }
        int varyingSizeEffectIndex = getVaryingSizeEffectIndex();

        int channels = samples.length;
        float[][] mixed = null;

        // Skip input collection and mixing if the varying-size effect requires zero-length input.
        if (inputLength > 0) {
            CollectedInputs collectedInputs = collectInputs(sampleRate, inputLength, channels);
            try {
                checkInputs(collectedInputs, inputLength, channels);
            } catch (ChannelsCountMismatchException | LengthMismatchException ex) {
                logger.error("Input channels or length mismatch.", ex);
                throw new MixingException(ex);
            }

            mixed = mixInputs(collectedInputs, inputLength, channels);
            SamplesUtilities.adjustGainAndPan(mixed, mixed, preGainControl.getValue(), 0.0f);

            if (enableEffects) {
                int firstEffectsEnd = varyingSizeEffectIndex == -1 ? effects.size() : varyingSizeEffectIndex;
                processEffectChain(mixed, sampleRate, 0, firstEffectsEnd);

                if (varyingSizeEffectIndex != -1) {
                    if (inputLength < outputLength) {
                        mixed = ArrayUtilities.padArrayWithLast(mixed, channels, outputLength);
                    }
                    effects.get(varyingSizeEffectIndex).render(mixed, sampleRate);
                    if (inputLength > outputLength) {
                        mixed = ArrayUtilities.cutArray(mixed, 0, channels, 0, outputLength);
                    }
                    processEffectChain(mixed, sampleRate, varyingSizeEffectIndex + 1, effects.size());
                }
            }
        } else {
            mixed = new float[channels][outputLength];
            if (enableEffects) {
                processEffectChain(mixed, sampleRate, varyingSizeEffectIndex + 1, effects.size());
            }
        }

        float separation = stereoSeparationControl.getValue();
        if (separation * separation > STEREO_SEP_EPSILON) {
            mixed = SamplesUtilities.stereoSeparation(mixed, separation);
        }

        if (swapChannelsControl.isEnabled()) {
            mixed = SamplesUtilities.swapChannels(mixed);
        }
        if (reversePolarityControl.isEnabled()) {
            mixed = SamplesUtilities.reversePolarity(mixed);
        }

        SamplesUtilities.adjustGainAndPan(mixed, mixed, postGainControl.getValue(), panControl.getValue());
        try {
            ArrayUtilities.copyArray(mixed, samples);
        } catch (ChannelsCountMismatchException | LengthMismatchException ex) {
            logger.error("Failed to copy mixed samples to output.", ex);
            throw new MixingException(ex);
        }
    }

    private int getTargetInputLength(int length) throws LengthMismatchException {
        if (!enableEffectsControl.isEnabled()) {
            return length;
        }
        int varyingIndex = getVaryingSizeEffectIndex();
        if (varyingIndex != -1) {
            AudioEffect effect = effects.get(varyingIndex);
            if (effect instanceof VaryingSizeEffect) {
                if (effect.getEnableControl().isDisabled() || effect.getMixLevelControl().getValue() <= 0.0f) {
                    return length;
                }
                int required = ((VaryingSizeEffect) effect).getTargetLength(length);
                if (required < 0) {
                    throw new LengthMismatchException("Effect requested negative input length: " + required);
                }
                length = required;
            } else {
                logger.warn("Effect at index {} is not a VaryingSizeEffect", varyingIndex);
            }
        }
        return length;
    }

    private CollectedInputs collectInputs(int sampleRate, int frameCount, int channels) {
        if (inputBuffers == null 
            || inputBuffers.length != inputs.size()
            || (inputs.size() > 0 && 
                (inputBuffers[0].length != channels || inputBuffers[0][0].length != frameCount))) {
            
            inputBuffers = new float[inputs.size()][channels][frameCount];
        } else {
            for (int i = 0; i < inputs.size(); i++) {
                ArrayUtilities.fillZeros(inputBuffers[i]);
            }
        }
        if (validInputs == null || validInputs.length != inputs.size()) {
            validInputs = new boolean[inputs.size()];
        } else {
            Arrays.fill(validInputs, false);
        }

        for (int i = 0; i < inputs.size(); i++) {
            try {
                if (inputs.get(i) == null) {
                    logger.warn("Null input at index {}", i);
                    continue;
                }
                inputs.get(i).render(inputBuffers[i], sampleRate);
                validInputs[i] = true;
            } catch (Exception ex) {
                logger.warn("Render failed for input[{}]", i, ex);
            }
        }

        if (collectedInputs == null) {
            collectedInputs = new CollectedInputs();
        }
        
        collectedInputs.inputs = inputBuffers;
        collectedInputs.validInputs = validInputs;
        return collectedInputs;
    }

    private void checkInputs(CollectedInputs collectedInputs, int frameCount, int channels) throws ChannelsCountMismatchException, LengthMismatchException {
        if (collectedInputs == null) {
            throw new NullPointerException("Collected inputs cannot be null.");
        }
        if (frameCount <= 0 || channels <= 0) {
            throw new IllegalArgumentException("Frame count or channels must be greater than zero.");
        }

        for (int i = 0; i < collectedInputs.inputs.length; i++) {
            if (!collectedInputs.validInputs[i]) continue; // Not an error
            float[][] input = collectedInputs.inputs[i];

            if (channels != input.length) {
                throw new ChannelsCountMismatchException(
                    String.format("Expected %d channels, but got %d at index %d", channels, input.length, i)
                );
            }

            if (!SamplesValidation.checkLength(input, frameCount)) {
                throw new LengthMismatchException(
                    String.format("Expected %d frames, but got %d at index %d", frameCount, input[0].length, i)
                );
            }
        }
    }
    
    private float[][] mixInputs(CollectedInputs collectedInputs, int frameCount, int channels) {
        if (mixedBuffer == null || mixedBuffer.length != channels || mixedBuffer[0].length != frameCount) {
            mixedBuffer = new float[channels][frameCount];
        } else {
            ArrayUtilities.fillZeros(mixedBuffer);
        }

        for (int i = 0; i < collectedInputs.inputs.length; i++) {
            if (!collectedInputs.validInputs[i]) continue;
            for (int ch = 0; ch < channels; ch++) {
                for (int frame = 0; frame < collectedInputs.inputs[i][ch].length; frame++) {
                    // Allow out of range (-1, +1) values
                    mixedBuffer[ch][frame] += collectedInputs.inputs[i][ch][frame];
                }
            }
        }

        return mixedBuffer;
    }

    private void processEffectChain(float[][] samples, int sampleRate, int start, int end) {
        int numChannels = samples.length;
        int numFrames = samples[0].length;

        for (int i = start; i < end; i++) {
            AudioEffect effect = effects.get(i);

            if (effect == null) {
                logger.warn("Effect at index {} is null.", i);
                continue;
            }

            float mixLevel = effect.getMixLevelControl().getValue();
            if (mixLevel <= 0.0f || !effect.getEnableControl().isEnabled()) {
                continue;
            }

            if (mixLevel >= 1.0f) {
                effect.render(samples, sampleRate);
                continue;
            }

            // Clone the input buffer
            if (effectBuffer == null || effectBuffer.length != numChannels || effectBuffer[0].length != numFrames) {
                effectBuffer = new float[numChannels][numFrames];
            }
            for (int ch = 0; ch < numChannels; ch++) {
                System.arraycopy(samples[ch], 0, effectBuffer[ch], 0, numFrames);
            }

            effect.render(effectBuffer, sampleRate);

            // Mix
            for (int ch = 0; ch < numChannels; ch++) {
                float[] out = samples[ch];
                float[] fx = effectBuffer[ch];
                for (int f = 0; f < numFrames; f++) {
                    out[f] = MathUtilities.lerp(out[f], fx[f], mixLevel);
                }
            }
        }
    }
}
