package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

/**
 * The InvertEffect class is an implementation of the AudioEffect class that 
 * inverts the amplitude of audio samples in real-time. This effect flips the 
 * waveform of the audio signal, effectively multiplying each sample by -1.
 * 
 * <p>This can be useful for phase inversion or creating specific audio effects.</p>
 * 
 * <p>Usage:</p>
 * <pre>
 * AudioFormat format = new AudioFormat(...);
 * InvertEffect invertEffect = new InvertEffect(format);
 * float[][] processedData = invertEffect.process(inputData);
 * </pre>
 * 
 * @see AudioEffect
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
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
