package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioResampler;
import org.theko.sound.control.FloatControl;

public class ResamplerEffect extends AudioEffect {
    private FloatControl speed;

    public ResamplerEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        speed = new FloatControl("Speed", 0.001f,32f, 1f);
    }

    @Override
    public float[][] process(float[][] data) {
        return AudioResampler.resample(data, audioFormat, speed.getValue());
    }

    public FloatControl getSpeedControl() {
        return speed;
    }
}
