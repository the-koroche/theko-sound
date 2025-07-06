package org.theko.sound.utility;

public class SamplesUtilities {
    
    private SamplesUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static float[][] reversePolarity(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        
        float[][] reversed = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            reversed[i] = new float[samples[i].length];
            for (int j = 0; j < samples[i].length; j++) {
                reversed[i][j] = -samples[i][j];
            }
        }
        return reversed;
    }

    public static float[][] swapChannels(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples cannot be null or empty.");
        }

        float[][] swapped = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            swapped[i] = samples[samples.length - 1 - i].clone();
        }
        return swapped;
    }

    public static float[][] reverse(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        
        float[][] reversed = new float[samples.length][samples[0].length];
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                reversed[i][j] = samples[i][samples[i].length - 1 - j];
            }
        }
        return reversed;
    }

    public static float[][] normalize(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }

        float max = 0.0f;
        for (float[] channel : samples) {
            for (float sample : channel) {
                max = Math.max(max, Math.abs(sample));
            }
        }

        if (max == 0.0f) {
            return samples; // Avoid division by zero
        }

        float[][] normalized = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            normalized[i] = new float[samples[i].length];
            for (int j = 0; j < samples[i].length; j++) {
                normalized[i][j] = samples[i][j] / max;
            }
        }
        return normalized;
    }
}
