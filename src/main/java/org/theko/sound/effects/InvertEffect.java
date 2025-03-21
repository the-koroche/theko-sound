package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public class InvertEffect extends AudioEffect {

    public InvertEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
    }

    @Override
    public float[][] process(float[][] data) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = data[i][j] * -1;
            }
        }
        return data;
    }
}
