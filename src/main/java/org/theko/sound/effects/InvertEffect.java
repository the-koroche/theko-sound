package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public class InvertEffect extends AudioEffect {

    public InvertEffect(AudioFormat audioFormat) {
        super(audioFormat);
    }

    @Override
    public float[][] process(float[][] data) {
        //byte[] output = new byte[data.length];
        //int bytesPerSample = audioFormat.getBytesPerSample();
        //Encoding encoding = audioFormat.getEncoding();

        //for (int i = 0; i < data.length; i += bytesPerSample) {
        //    switch (bytesPerSample) {
        //        case 1 -> process8Bit(data, output, i, encoding);
        //        case 2 -> process16Bit(data, output, i, encoding);
        //        case 3 -> process24Bit(data, output, i, encoding);
        //        case 4 -> process32Bit(data, output, i, encoding);
        //        default -> throw new IllegalArgumentException("Unsupported sample size: " + bytesPerSample);
        //    }
        //}
        //return output;

        float[][] output = new float[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                output[i][j] = 1.0f - data[i][j];
            }
        }
        return output;
    }
}
