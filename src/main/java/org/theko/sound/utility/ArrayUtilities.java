package org.theko.sound.utility;

import java.util.Arrays;

import org.theko.sound.ChannelsCountMismatchException;
import org.theko.sound.LengthMismatchException;

public class ArrayUtilities {

    private ArrayUtilities () {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static float[] cutArray (float[] array, int start, int end) {
        if (start < 0 || end > array.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid start or end indices.");
        }
        return Arrays.copyOfRange(array, start, end);
    }

    public static float[][] cutArray (float[][] array, int startD1, int endD1, int startD2, int endD2) {
        if (startD1 < 0 || endD1 > array.length || startD1 > endD1) {
            throw new IndexOutOfBoundsException("Invalid D1 indices.");
        }

        float[][] result = Arrays.copyOfRange(array, startD1, endD1);

        for (int i = 0; i < result.length; i++) {
            if (result[i].length < endD2 || startD2 > endD2) {
                throw new IndexOutOfBoundsException("Invalid D2 indices in row " + (startD1 + i));
            }
            result[i] = Arrays.copyOfRange(result[i], startD2, endD2);
        }

        return result;
    }

    public static float[][] padArray(float[][] original, int newLengthD1, int newLengthD2) {
        if (original == null) {
            throw new IllegalArgumentException("Original array cannot be null.");
        }
        if (newLengthD1 <= 0 || newLengthD2 <= 0) {
            throw new IllegalArgumentException("New lengths must be greater than zero.");
        }

        float[][] padded = new float[newLengthD1][newLengthD2];
        for (int i = 0; i < Math.min(original.length, newLengthD1); i++) {
            System.arraycopy(original[i], 0, padded[i], 0, Math.min(original[i].length, newLengthD2));
        }
        return padded;
    }

    public static void copyArray (float[][] source, float[][] target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target cannot be null.");
        }
        if (source.length != target.length) {
            throw new RuntimeException(new ChannelsCountMismatchException("Channel count mismatch."));
        }

        for (int ch = 0; ch < source.length; ch++) {
            if (source[ch].length != target[ch].length) {
                throw new RuntimeException(new LengthMismatchException(
                    String.format(
                        "Length mismatch at channel %d: source=%d, target=%d",
                        ch, source[ch].length, target[ch].length
                    )
                ));
            }
            System.arraycopy(source[ch], 0, target[ch], 0, source[ch].length);
        }
    }

    public static void fillZeros(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        
        for (int ch = 0; ch < samples.length; ch++) {
            Arrays.fill(samples[ch], 0.0f);
        }
    }
}
