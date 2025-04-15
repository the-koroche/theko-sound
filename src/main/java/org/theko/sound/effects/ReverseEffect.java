package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

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
