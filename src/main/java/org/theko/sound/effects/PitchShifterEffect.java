package org.theko.sound.effects;

import java.util.Arrays;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public class PitchShifterEffect extends AudioEffect {
    public PitchShifterEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
    }

    public static float[] timeStretch(float[] input, float stretchFactor, int windowSize, int overlap) {
        int stepSize = windowSize - overlap;
        int newLength = (int) ((input.length - windowSize) / stretchFactor) + windowSize;
        float[] output = new float[newLength];
        int[] counts = new int[newLength]; // Для нормализации
        float[] window = hannWindow(windowSize);
        
        for (int i = 0; i <= input.length - windowSize; i += stepSize) {
            int j = (int) (i / stretchFactor);
            float[] segment = Arrays.copyOfRange(input, i, i + windowSize);
            applyWindow(segment, window);
            addWithOverlap(output, counts, segment, j, windowSize);
        }
        
        // Нормализация
        for (int k = 0; k < output.length; k++) {
            if (counts[k] > 0) {
                output[k] /= counts[k];
            }
        }
        
        return output;
    }

    public static float[] pitchShift(float[] input, float pitchFactor, int windowSize, int overlap) {
        float[] stretched = timeStretch(input, 1 / pitchFactor, windowSize, overlap);
        return linearResample(stretched, input.length);
    }

    // Линейная интерполяция вместо Lanczos
    public static float[] linearResample(float[] input, int targetLength) {
        float[] output = new float[targetLength];
        float scale = (float) input.length / targetLength;
        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i0 = (int) pos;
            float t = pos - i0;
            
            if (i0 < 0) {
                output[i] = input[0];
            } else if (i0 >= input.length - 1) {
                output[i] = input[input.length - 1];
            } else {
                output[i] = input[i0] * (1 - t) + input[i0 + 1] * t;
            }
        }
        return output;
    }

    public static float[] hannWindow(int size) {
        float[] window = new float[size];
        for (int i = 0; i < size; i++) {
            window[i] = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (size - 1)));
        }
        return window;
    }

    private static void applyWindow(float[] segment, float[] window) {
        for (int i = 0; i < segment.length; i++) {
            segment[i] *= window[i];
        }
    }

    private static void addWithOverlap(float[] output, int[] counts, float[] segment, int position, int windowSize) {
        for (int i = 0; i < windowSize; i++) {
            int idx = position + i;
            if (idx < output.length && idx >= 0) {
                output[idx] += segment[i];
                counts[idx]++;
            }
        }
    }

    @Override
    public float[][] process(float[][] samples) {
        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = pitchShift(samples[ch], 2f, 1024, 512);
        }
        return samples;
    }
}