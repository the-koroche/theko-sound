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
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.AudioEffect;
import org.theko.sound.utility.SamplesUtilities;

/**
 * Represents an audio effect that sends audio samples from an {@link AudioMixer} to a target {@link AudioNode}.
 * <p>
 * It provides controls for gain and pan, and allows for applying a chain of effects to the audio samples before sending them to the target node.
 * 
 * @see AudioMixer
 * @see AudioNode
 * @see AudioEffect
 * 
 * @author Theko
 * @since 2.4.1
 */
public class MixerSender extends AudioEffect {

    private static final Logger logger = LoggerFactory.getLogger(MixerSender.class);
    
    protected AudioMixer mixer;
    protected AudioNode senderInputNode = new SenderPlayback();
    protected float[][] effectSamples;

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
     * @param mixer The audio mixer to send audio samples to.
     */
    public MixerSender(AudioMixer mixer) {
        super(Type.REALTIME);
        addControls(mixerControls);

        setMixer(mixer);
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
        effectSamples = samples;
    }

    /**
     * Sets the audio mixer to send audio samples to.
     * If the old mixer is not null, removes the sender node from the old mixer.
     * If the new mixer is not null, adds the sender node to the new mixer.
     * @param mixer The audio mixer to send audio samples to.
     */
    public void setMixer(AudioMixer mixer) {
        if (this.mixer != null) {
            this.mixer.removeInput(senderInputNode);
        }
        this.mixer = mixer;
        if (this.mixer == null) {
            return;
        }
        this.mixer.addInput(senderInputNode);
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
     * Returns the audio mixer that is currently being used by the mixer sender.
     * This mixer is responsible for processing the audio samples sent by the sender node,
     * and is the destination of the audio samples that are processed by the mixer sender.
     * 
     * @return The audio mixer currently being used by the mixer sender.
     */
    public AudioMixer getMixer() {
        return mixer;
    }
}
