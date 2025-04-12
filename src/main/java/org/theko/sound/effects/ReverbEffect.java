package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

import java.util.ArrayList;
import java.util.List;

public class ReverbEffect extends AudioEffect {
    private final int sampleRate;
    private final int numChannels;

    private final List<CircularBuffer> delayBuffers;
    private final int[] delayTimesInSamples;
    private final float[] decayFactors;
    
    // Дополнительные параметры для более сложной реверберации
    private final float earlyReflectionsDecay;
    private final float lateReverbDecay;
    private final int earlyReflectionsDelay;
    private final int lateReverbDelay;

    public ReverbEffect(AudioFormat audioFormat) {
        super(AudioEffect.Type.REALTIME, audioFormat);
        this.sampleRate = (int) audioFormat.getSampleRate();
        this.numChannels = audioFormat.getChannels();
        
        // Параметры реверберации
        double[] delayTimesInSeconds = {0.05, 0.10, 0.15, 0.20}; // задержки для эхо и реверберации
        float[] decays = {0.7f, 0.5f, 0.3f, 0.15f}; // коэффициенты затухания для каждого из эхо
        
        this.delayTimesInSamples = new int[delayTimesInSeconds.length];
        for(int i = 0; i < delayTimesInSeconds.length; i++) {
            delayTimesInSamples[i] = (int)(delayTimesInSeconds[i] * sampleRate);
        }
        this.decayFactors = decays;
        
        // Дополнительные настройки для рефлексий и поздней реверберации
        this.earlyReflectionsDecay = 0.6f;
        this.lateReverbDecay = 0.4f;
        this.earlyReflectionsDelay = (int) (0.03 * sampleRate); // 30ms
        this.lateReverbDelay = (int) (0.15 * sampleRate); // 150ms
        
        int maxDelay = 0;
        for (int delay : delayTimesInSamples) {
            if (delay > maxDelay) maxDelay = delay;
        }

        this.delayBuffers = new ArrayList<>();
        for (int i = 0; i < numChannels; i++) {
            delayBuffers.add(new CircularBuffer(maxDelay));
        }
    }

    @Override
    public float[][] process(float[][] samples) {
        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = applyReverb(samples[ch]);
        }
        return samples;
    }

    private float[] applyReverb(float[] input) {
        float[] output = new float[input.length];
        int samplesPerChannel = input.length / numChannels;

        for (int ch = 0; ch < numChannels; ch++) {
            CircularBuffer buffer = delayBuffers.get(ch);
            
            // Применение реверберации по каналу
            for (int i = 0; i < samplesPerChannel; i++) {
                int idx = i * numChannels + ch;
                float original = input[idx];
                float sum = original;

                // Добавляем ранние отражения (early reflections)
                if (i > earlyReflectionsDelay) {
                    float earlyReflections = buffer.getDelayed(earlyReflectionsDelay);
                    sum += earlyReflections * earlyReflectionsDecay;
                }

                // Добавляем поздние отражения (late reverb)
                if (i > lateReverbDelay) {
                    float lateReverb = buffer.getDelayed(lateReverbDelay);
                    sum += lateReverb * lateReverbDecay;
                }

                // Добавляем дополнительные задержки с учетом коэффициентов затухания
                for (int d = 0; d < delayTimesInSamples.length; d++) {
                    float delayed = buffer.getDelayed(delayTimesInSamples[d]);
                    sum += delayed * decayFactors[d];
                }

                // Ограничение амплитуды
                sum = Math.max(-1.0f, Math.min(1.0f, sum));

                // Обновляем буфер
                buffer.put(original);
                output[idx] = sum;
            }
        }

        return output;
    }

    private static class CircularBuffer {
        private final float[] buffer;
        private int pointer;

        public CircularBuffer(int size) {
            this.buffer = new float[size];
            this.pointer = 0;
        }

        public void put(float sample) {
            buffer[pointer] = sample;
            pointer = (pointer + 1) % buffer.length;
        }

        public float getDelayed(int delay) {
            int index = (pointer - delay + buffer.length) % buffer.length;
            return buffer[index];
        }
    }
}
