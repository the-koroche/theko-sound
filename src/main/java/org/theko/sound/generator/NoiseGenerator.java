package org.theko.sound.generator;

public class NoiseGenerator extends AudioGenerator {
    @Override
    public void render(float[][] samples, int sampleRate) {
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                samples[ch][i] = (float) Math.random() * 2 - 1;
            }
        }
    }
}
