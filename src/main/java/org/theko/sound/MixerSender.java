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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.effects.AudioEffect;
import org.theko.sound.util.ArrayUtilities;
import org.theko.sound.util.SamplesUtilities;

/**
 * Sends processed audio samples from its position in an {@link AudioMixer} effect chain
 * to a target {@link AudioMixer}.
 * <p>
 * This effect acts as a bridge between two mixers: it receives the audio samples
 * from the point in the current mixer's effect chain where it is inserted,
 * applies gain and pan adjustments, and then forwards the samples to the target mixer.
 * <p>
 * Example usage:
 * <pre>
 * AudioMixer sourceMixer = new AudioMixer();
 * AudioMixer targetMixer = new AudioMixer();
 * 
 * sourceMixer.addEffect(new AudioEffect()); // This effect's output will be included in MixerSender
 * sourceMixer.addEffect(new MixerSender(targetMixer)); // MixerSender forwards current chain samples to targetMixer
 * sourceMixer.addEffect(new AudioEffect()); // This effect's output will NOT be included in MixerSender
 * </pre>
 * 
 * @see AudioMixer
 * @see AudioEffect
 * 
 * @author Theko
 * @since 2.4.1
 */
public class MixerSender extends AudioEffect {

    private static final Logger logger = LoggerFactory.getLogger(MixerSender.class);
    
    protected AudioMixer targetMixer;
    protected AudioNode senderInputNode = new SenderPlayback();
    protected volatile float[][] effectSamples;

    protected final FloatControl gain = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    protected final FloatControl pan = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);

    private class SenderPlayback implements AudioNode {

        @Override
        public void render(float[][] samples, int sampleRate) {
            if (effectSamples == null) {
                return;
            }
            try {
                SamplesUtilities.adjustGainAndPan(effectSamples, samples, gain.getValue(), pan.getValue());
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected final List<AudioControl> mixerControls = List.of(gain, pan);
    
    /**
     * Constructs a new {@code MixerSender} with the specified {@link AudioMixer}.
     * @param mixer The audio mixer to get audio samples from.
     */
    public MixerSender(AudioMixer mixer) {
        super(Type.REALTIME);
        addEffectControls(mixerControls);

        setTargetMixer(mixer);
    }

    /**
     * Constructs a new {@code MixerSender} with no audio mixer.
     */
    public MixerSender() {
        this(null);
    }
    
    @Override
    protected void effectRender(float[][] samples, int sampleRate) {
        if (effectSamples == null || effectSamples.length != samples.length || effectSamples[0].length != samples[0].length) {
            effectSamples = new float[samples.length][samples[0].length];
        }
        try {
            ArrayUtilities.copyArray(samples, effectSamples);
        } catch (LengthMismatchException | ChannelsCountMismatchException ignored) {
        }
    }

    /**
     * Sets the audio mixer to send audio samples to.
     * If the old mixer is not null, removes the sender node from the old mixer.
     * If the new mixer is not null, adds the sender node to the new mixer.
     * @param mixer The audio mixer to send audio samples to.
     */
    public void setTargetMixer(AudioMixer mixer) {
        if (this.targetMixer != null) {
            this.targetMixer.removeInput(senderInputNode);
        }
        this.targetMixer = mixer;
        if (this.targetMixer == null) {
            return;
        }
        this.targetMixer.addInput(senderInputNode);
    }

    /**
     * Returns the gain control for this mixer sender.
     * The gain control allows adjusting the volume of the audio samples sent to the target node.
     * 
     * @return The FloatControl representing the gain control of the mixer sender.
     */
    public FloatControl getGainControl() {
        return gain;
    }

    /**
     * Returns the pan control for this mixer sender.
     * The pan control allows adjusting the left-right balance of the audio samples sent to the target node.
     * 
     * @return The FloatControl representing the pan control of the mixer sender.
     */
    public FloatControl getPanControl() {
        return pan;
    }


    /**
     * Returns the audio samples that have been processed by the mixer sender
     * and are ready to be sent to the target node.
     * 
     * @return The audio samples that have been processed by the mixer sender.
     */
    public float[][] getEffectSamples() {
        return effectSamples;
    }

    /**
     * Returns the audio node that is responsible for sending audio samples to the mixer.
     * This node is the source of the audio samples that are processed by the mixer sender,
     * and is added as an input to the mixer when the mixer sender is started.
     * 
     * @return The audio node responsible for sending audio samples to the mixer.
     */
    public AudioNode getSenderNode() {
        return senderInputNode;
    }

    /**
     * Returns the audio mixer that receives the audio samples from this mixer sender.
     * 
     * @return The destination audio mixer where processed samples are sent.
     */
    public AudioMixer getTargetMixer() {
        return targetMixer;
    }
}
