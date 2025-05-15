package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

/**
 * The ReverseEffect class is an implementation of an audio effect that reverses
 * the audio samples for each channel in real-time. This effect processes the
 * audio data by reversing the order of samples within each channel, effectively
 * playing the audio backward.
 * 
 * <p>This class extends the {@link AudioEffect} class and overrides the
 * {@code process} method to apply the reverse effect on the provided audio
 * samples.
 * 
 * <p>Usage:
 * <pre>
 * AudioFormat format = new AudioFormat(...);
 * ReverseEffect reverseEffect = new ReverseEffect(format);
 * float[][] processedSamples = reverseEffect.process(samples);
 * </pre>
 * 
 * @see AudioEffect
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public class ReverseEffect extends AudioEffect {
    public ReverseEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
    }

    @Override
    protected float[][] process(float[][] samples) {
        for (int ch = 0; ch < samples.length; ch++) {
            float[] channel = samples[ch];
            int len = channel.length;
            for (int i = 0; i < len / 2; i++) {
                float temp = channel[i];
                channel[i] = channel[len - 1 - i];
                channel[len - 1 - i] = temp;
            }
        }
        return samples;
    }
}
