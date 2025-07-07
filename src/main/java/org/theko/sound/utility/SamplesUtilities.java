package org.theko.sound.utility;

import org.theko.sound.LengthMismatchException;

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

    public static float[][] stereoSeparation(float[][] samples, float separation) {
        if (samples == null || samples.length < 2) {
            throw new IllegalArgumentException("Samples must contain at least two channels.");
        }
        try {
            checkLength(samples);
        } catch (LengthMismatchException ex) {
            throw new IllegalArgumentException("All channels must have the same length.", ex);
        }
        if (samples.length > 2) {
            throw new IllegalArgumentException("This method only supports stereo (2-channel) samples.");
        }
        float normalizedSeparation = Math.max(-1.0f, Math.min(separation, 1.0f));
        float amount = (normalizedSeparation + 1.0f) / 2.0f; // Remap from [-1, 1] to [0, 1]
        float[][] separated = new float[samples.length][];

        int frameCount = samples[0].length;
        for (int i = 0; i < samples.length; i++) {
            separated[i] = new float[frameCount];
        }
        for (int i = 0; i < samples[0].length; i++) {
            float left = samples[0][i];
            float right = samples[1][i];

            float mid = (left + right) / 2.0f;
            float side = (left - right) / 2.0f;

            separated[0][i] = mid + side * amount;
            separated[1][i] = mid - side * amount;
        }
        return separated;
    }

    public static void checkLength(float[][] samples) throws LengthMismatchException {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        checkLength(samples, samples[0].length);
    }

    public static void checkLength(float[][] samples, int length) throws LengthMismatchException {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        for (int ch = 0; ch < samples.length; ch++) {
            if (samples[ch].length != length) {
                throw new LengthMismatchException("Channel " + ch + " has " + samples[ch].length + " samples, expected " + length);
            }
        }
    }
}
