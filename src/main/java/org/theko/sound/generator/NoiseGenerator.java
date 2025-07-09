package org.theko.sound.generator;

public class NoiseGenerator extends AudioGenerator {
    @Override
    public void render(float[][] samples, int sampleRate, int length) {
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < length; i++) {
                samples[ch][i] = (float) Math.random() * 2 - 1;
            }
        }
    }
}
