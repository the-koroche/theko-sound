package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioResampler;
import org.theko.sound.control.FloatController;

public class SpeedChangeEffect extends AudioEffect {
    private FloatController speed;

    public SpeedChangeEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        speed = new FloatController("SPEED", 0.001f, 16f, 1f);
    }

    @Override
    public float[][] process(float[][] data) {
        return AudioResampler.resample(data, audioFormat, speed.getValue());
    }

    public FloatController getSpeedController() {
        return speed;
    }
}
