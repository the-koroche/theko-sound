package org.theko.sound;

import java.util.ArrayList;
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
import static org.theko.sound.properties.AudioSystemProperties.*;
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
 * @since v2.0.0
 */
public class AudioMixer implements AudioNode {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioMixer.class);

    private final List<AudioNode> inputs = new ArrayList<>();
    private final List<AudioEffect> effects = new ArrayList<>();

    private final FloatControl preGainControl = new FloatControl("Pre-Gain", 0.0f, 2.0f, 1.0f);
    private final FloatControl postGainControl = new FloatControl("Post-Gain", 0.0f, 2.0f, 1.0f);
    private final FloatControl panControl = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);
    private final FloatControl stereoSeparationControl = new FloatControl("Stereo Separation", -1.0f, 1.0f, 0.0f);

    private final BooleanControl enableEffectsControl = new BooleanControl("Enable Effects", ENABLE_EFFECTS_IN_MIXER);
    private final BooleanControl swapChannelsControl = new BooleanControl("Swap Channels", SWAP_CHANNELS_IN_MIXER);
    private final BooleanControl reversePolarityControl = new BooleanControl("Reverse Polarity", REVERSE_POLARITY_IN_MIXER);

    private static final float STEREO_SEP_EPSILON = 0.000001f;

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

    private boolean hasMixer (AudioMixer mixer) {
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

    public void addEffect (AudioEffect effect) throws IncompatibleEffectTypeException, MultipleVaryingSizeEffectsException {
        if (effect == null) {
            logger.error("Attempted to add null effect to AudioMixer");
            throw new IllegalArgumentException("Effect cannot be null");
        }
        if (effect.getType() == AudioEffect.Type.OFFLINE_PROCESSING) {
            logger.error("Attempted to add an offline processing effect ({}) to AudioMixer", effect.getClass().getSimpleName());
            throw new IncompatibleEffectTypeException();
        }
        if (effect instanceof VaryingSizeEffect && hasVaryingSizeEffect()) {
            logger.error("Attempted to add multiple varying size effects to AudioMixer");
            throw new MultipleVaryingSizeEffectsException();
        }
        effects.add(effect);
    }

    protected boolean hasVaryingSizeEffect () {
        return getVaryingSizeEffectIndex() != -1;
    }

    protected int getVaryingSizeEffectIndex () {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i) instanceof VaryingSizeEffect) {
                return i;
            }
        }
        return -1;
    }

    public void removeInput (AudioNode input) {
        if (input == null) {
            logger.error("Attempted to remove null input from AudioMixer");
            throw new IllegalArgumentException("Input cannot be null");
        }
        inputs.remove(input);
    }

    public void removeEffect (AudioEffect effect) {
        if (effect == null) {
            logger.error("Attempted to remove null effect from AudioMixer");
            throw new IllegalArgumentException("Effect cannot be null");
        }
        effects.remove(effect);
    }

    public List<AudioNode> getInputs () {
        return Collections.unmodifiableList(inputs);
    }

    public List<AudioEffect> getEffects () {
        return Collections.unmodifiableList(effects);
    }

    public FloatControl getPreGainControl () {
        return preGainControl;
    }

    public FloatControl getPostGainControl () {
        return postGainControl;
    }

    public FloatControl getPanControl () {
        return panControl;
    }

    public FloatControl getStereoSeparationControl () {
        return stereoSeparationControl;
    }

    public BooleanControl getEnableEffectsControl () {
        return enableEffectsControl;
    }

    public BooleanControl getSwapChannelsControl () {
        return swapChannelsControl;
    }

    public BooleanControl getReversePolarityControl () {
        return reversePolarityControl;
    }

    @Override
    public void render (float[][] samples, int sampleRate, int length) {
        if (samples == null || samples.length == 0 || length <= 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty, and length must be greater than zero.");
        }

        boolean enableEffects = enableEffectsControl.isEnabled();
        int inputLength = length;
        try {
            inputLength = getTargetInputLength(length);
        } catch (LengthMismatchException ex) {
            logger.error("Input length is not valid.", ex);
            throw new MixingException(ex);
        }
        int varyingSizeEffectIndex = getVaryingSizeEffectIndex();

        int channels = samples.length;
        CollectedInputs collectedInputs = collectInputs(sampleRate, inputLength, channels);
        try {
            checkInputs(collectedInputs, inputLength, channels);
        } catch (ChannelsCountMismatchException | LengthMismatchException ex) {
            logger.error("Input channels or length mismatch.", ex);
            throw new MixingException(ex);
        }

        float[][] mixed = mixInputs(collectedInputs, inputLength, channels);
        SamplesUtilities.adjustGainAndPan(mixed, preGainControl.getValue(), 0.0f);

        if (enableEffects) {
            int preVaryingEffectEnd = varyingSizeEffectIndex == -1 ? effects.size() : varyingSizeEffectIndex;
            processEffectChain(mixed, sampleRate, 0, preVaryingEffectEnd);

            if (varyingSizeEffectIndex != -1) {
                if (inputLength < length) {
                    mixed = ArrayUtilities.padArray(mixed, channels, length);
                }
                effects.get(varyingSizeEffectIndex).render(mixed, sampleRate, inputLength);
                if (inputLength > length) {
                    mixed = ArrayUtilities.cutArray(mixed, 0, channels, 0, length);
                } else {
                    processEffectChain(mixed, sampleRate, varyingSizeEffectIndex + 1, effects.size());
                }
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

        SamplesUtilities.adjustGainAndPan(mixed, postGainControl.getValue(), panControl.getValue());
        try {
            ArrayUtilities.copyArray(mixed, samples);
        } catch (ChannelsCountMismatchException | LengthMismatchException ex) {
            logger.error("Failed to copy mixed samples to output.", ex);
            throw new MixingException(ex);
        }
        
    }

    private int getTargetInputLength (int length) throws LengthMismatchException {
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
                if (required <= 0) {
                    throw new LengthMismatchException("Effect requested invalid input length: " + required);
                }
                length = required;
            } else {
                logger.warn("Effect at index {} is not a VaryingSizeEffect", varyingIndex);
            }
        }
        return length;
    }

    private CollectedInputs collectInputs (int sampleRate, int frameCount, int channels) {
        float[][][] inputBuffers = new float[inputs.size()][channels][frameCount];
        boolean[] valid = new boolean[inputs.size()];

        for (int i = 0; i < inputs.size(); i++) {
            try {
                inputs.get(i).render(inputBuffers[i], sampleRate, frameCount);
                valid[i] = true;
            } catch (Exception ex) {
                logger.warn("Render failed for input[{}]: {}", i, ex.toString());
            }
        }

        return new CollectedInputs(inputBuffers, valid);
    }

    private void checkInputs (CollectedInputs collectedInputs, int frameCount, int channels) throws ChannelsCountMismatchException, LengthMismatchException {
        if (collectedInputs == null) {
            throw new NullPointerException("Collected inputs cannot be null.");
        }
        if (frameCount <= 0 || channels <= 0) {
            throw new IllegalArgumentException("Frame count or channels must be greater than zero.");
        }

        for (int i = 0; i < collectedInputs.inputs.length; i++) {
            if (!collectedInputs.valid[i]) continue; // Not an error
            float[][] input = collectedInputs.inputs[i];

            if (channels != input.length) {
                throw new ChannelsCountMismatchException(
                    String.format("Expected %d channels, but got %d at index %d", channels, input.length, i)
                );
            }

            if (CHECK_LENGTH_MISMATCH_IN_MIXER) {
                SamplesUtilities.checkLength(input, frameCount);
            }
        }
    }
    
    private float[][] mixInputs (CollectedInputs collectedInputs, int frameCount, int channels) {
        float[][] mixed = new float[channels][frameCount];

        for (int i = 0; i < collectedInputs.inputs.length; i++) {
            if (!collectedInputs.valid[i]) continue;
            for (int ch = 0; ch < channels; ch++) {
                for (int frame = 0; frame < collectedInputs.inputs[i][ch].length; frame++) {
                    // Allow out of range (-1, +1) values
                    mixed[ch][frame] += collectedInputs.inputs[i][ch][frame];
                }
            }
        }

        return mixed;
    }

    private void processEffectChain (float[][] samples, int sampleRate, int start, int end) {
        for (int i = start; i < end; i++) {
            AudioEffect effect = effects.get(i);
            if (effect.getMixLevelControl().getValue() > 0.0f && effect.getEnableControl().isEnabled()) {
                float[][] effectBuffer = ArrayUtilities.cutArray(samples, 0, samples.length, 0, samples[0].length);
                effect.render(effectBuffer, sampleRate, effectBuffer[0].length);
                float mixLevel = effect.getMixLevelControl().getValue();
                for (int ch = 0; ch < samples.length; ch++) {
                    float[] channelSamples = samples[ch];
                    float[] effectChannelSamples = effectBuffer[ch];
                    for (int frame = 0; frame < channelSamples.length; frame++) {
                        channelSamples[frame] = MathUtilities.lerp(samples[ch][frame], effectChannelSamples[frame], mixLevel);
                    }
                }
            }
            // Else do nothing, as the effect is disabled or has zero mix level
        }
    }

    private static class CollectedInputs {
        float[][][] inputs;
        boolean[] valid;

        CollectedInputs (float[][][] inputs, boolean[] valid) {
            this.inputs = inputs;
            this.valid = valid;
        }
    }
}
