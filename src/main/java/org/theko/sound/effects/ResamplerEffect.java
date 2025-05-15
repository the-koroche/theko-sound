package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioResampler;
import org.theko.sound.control.FloatControl;

@NonFixedSizeEffect
public class ResamplerEffect extends AudioEffect {
    private FloatControl speed;
    private AudioResampler resampler;

    public ResamplerEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        speed = new FloatControl("Speed", 0.001f,32f, 1f);
        setResampler(new AudioResampler());
    }

    public void setResampler(AudioResampler resampler) {
        if (resampler == null) {
            throw new IllegalArgumentException("New resampler cannot be null.");
        }
        this.resampler = resampler;
    }

    @Override
    public float[][] process(float[][] data) {
        return resampler.resample(data, speed.getValue());
    }

    public FloatControl getSpeedControl() {
        return speed;
    }
}
