/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.utility;

import java.util.Arrays;

import org.theko.sound.ChannelsCountMismatchException;
import org.theko.sound.LengthMismatchException;

/**
 * Utility class for array operations related to audio samples.
 * <p>This class provides methods to cut, pad, copy, fill, and create 2D float arrays (matrices).
 * It also includes methods for mutating samples in place.
 * 
 * @since 2.0.0
 * @author Theko
 * 
 * @see SamplesUtilities
 */
public final class ArrayUtilities {

    private ArrayUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Cuts a 1D float array to a specified range.
     *
     * @param array The original float array to cut.
     * @param start The starting index (inclusive).
     * @param end The ending index (exclusive).
     * @return A new float array containing the specified range.
     * @throws IndexOutOfBoundsException if the indices are out of bounds.
     */
    public static float[] cutArray(float[] array, int start, int end) {
        if (start < 0 || end > array.length || start > end) {
            throw new IndexOutOfBoundsException("Invalid indices for cutting the array.");
        }
        return Arrays.copyOfRange(array, start, end);
    }

    /**
     * Cuts a 2D float array (matrix) to a specified range in both dimensions.
     *
     * @param array The original 2D float array to cut.
     * @param startD1 The starting index for the first dimension (rows).
     * @param endD1 The ending index for the first dimension (rows).
     * @param startD2 The starting index for the second dimension (columns).
     * @param endD2 The ending index for the second dimension (columns).
     * @return A new 2D float array containing the specified range.
     * @throws IndexOutOfBoundsException if the indices are out of bounds.
     */
    public static float[][] cutArray(float[][] array, int startD1, int endD1, int startD2, int endD2) {
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

    /**
     * Pads a 2D float array to a new specified length in both dimensions.
     * If the original array is smaller than the new dimensions, it will be padded with zeros.
     *
     * @param original The original 2D float array to pad.
     * @param newLengthD1 The new length for the first dimension (rows).
     * @param newLengthD2 The new length for the second dimension (columns).
     * @return A new 2D float array padded to the specified dimensions.
     * @throws IllegalArgumentException if the original array is null or if the new lengths are less than or equal to zero.
     */
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

    /**
     * Pads a 2D float array to a new specified length in both dimensions.
     * If the original array is smaller than the new dimensions, it will be padded
     * by repeating the last available element in each row instead of zeros.
     *
     * @param original The original 2D float array to pad.
     * @param newLengthD1 The new length for the first dimension (rows).
     * @param newLengthD2 The new length for the second dimension (columns).
     * @return A new 2D float array padded to the specified dimensions.
     * @throws IllegalArgumentException if the original array is null or if the new lengths are less than or equal to zero.
     */
    public static float[][] padArrayWithLast(float[][] original, int newLengthD1, int newLengthD2) {
        if (original == null) {
            throw new IllegalArgumentException("Original array cannot be null.");
        }
        if (newLengthD1 <= 0 || newLengthD2 <= 0) {
            throw new IllegalArgumentException("New lengths must be greater than zero.");
        }

        float[][] padded = new float[newLengthD1][newLengthD2];

        for (int i = 0; i < Math.min(original.length, newLengthD1); i++) {
            int copyLength = Math.min(original[i].length, newLengthD2);
            System.arraycopy(original[i], 0, padded[i], 0, copyLength);

            if (copyLength < newLengthD2) {
                float lastValue = original[i].length > 0 ? original[i][original[i].length - 1] : 0.0f;
                for (int j = copyLength; j < newLengthD2; j++) {
                    padded[i][j] = lastValue;
                }
            }
        }

        for (int i = original.length; i < newLengthD1; i++) {
            float[] newRow = new float[newLengthD2];
            float lastValue = original.length > 0 && original[original.length - 1].length > 0
                    ? original[original.length - 1][original[original.length - 1].length - 1]
                    : 0.0f;
            for (int j = 0; j < newLengthD2; j++) {
                newRow[j] = lastValue;
            }
            padded[i] = newRow;
        }

        return padded;
    }

    /**
     * Copies a 2D float array (matrix) from source to target.
     * The source and target arrays must have the same number of channels (rows).
     * Each channel can have a different length, the target length will be minimum of the two.
     *
     * @param source The source 2D float array to copy from.
     * @param target The target 2D float array to copy to.
     * @throws LengthMismatchException if the lengths of the channels do not match.
     * @throws ChannelsCountMismatchException if the number of channels does not match.
     */
    public static void copyArray(float[][] source, float[][] target) throws LengthMismatchException, ChannelsCountMismatchException {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target cannot be null.");
        }
        if (source.length != target.length) {
            throw new ChannelsCountMismatchException("Channel count mismatch.");
        }

        for (int ch = 0; ch < source.length; ch++) {
            if (source[ch] == null || target[ch] == null) {
                throw new IllegalArgumentException("Source and target channels cannot be null.");
            }
            int min = Math.min(source[ch].length, target[ch].length);
            System.arraycopy(source[ch], 0, target[ch], 0, min);
        }
    }

    /**
     * Fills a 2D float array (matrix) with zeros.
     * This method sets all elements in each channel to zero.
     *
     * @param samples The 2D float array to fill with zeros.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static void fillZeros(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        
        for (int ch = 0; ch < samples.length; ch++) {
            Arrays.fill(samples[ch], 0.0f);
        }
    }
}
