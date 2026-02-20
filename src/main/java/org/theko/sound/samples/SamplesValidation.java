/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.samples;

import org.theko.sound.util.AudioBufferUtilities;

/**
 * Utility class for validating audio samples.
 * Samples validation checks if the provided 2D float array represents valid audio samples.
 * 
 * @see AudioBufferUtilities
 * 
 * @since 0.2.4-beta
 * @author Theko
 */
public final class SamplesValidation {
    
    /**
     * Enumeration of possible validation results for audio samples.
     */
    public enum ValidationResult {
        VALID, NULL_ARRAY, NULL_CHANNEL, EMPTY_ARRAY, EMPTY_CHANNEL
    }

    /**
     * Enumeration of possible results for audio samples dimensions validation.
     */
    public enum DimensionsResult {
        EXACT, INVALID_CHANNELS, INVALID_SAMPLES, NOT_MATCH_CHANNELS, NOT_MATCH_SAMPLES
    }

    /**
     * Checks if two 2D float arrays (matrices) represent valid audio samples.
     * This method checks if both arrays have the same number of channels, and if each channel has the same number of samples.
     * 
     * @param a The first 2D float array to check.
     * @param b The second 2D float array to check.
     * @return An enumeration value indicating the result of the validation check.
     * @see DimensionsResult
     */
    public static DimensionsResult checkSamplesDimensions(float[][] a, float[][] b) {
        if (a == null || b == null) return DimensionsResult.INVALID_SAMPLES;
        if (a.length != b.length) return DimensionsResult.NOT_MATCH_CHANNELS;
        
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null || b[i] == null) {
                return DimensionsResult.INVALID_CHANNELS;
            } else if (a[i].length != b[i].length) {
                return DimensionsResult.NOT_MATCH_SAMPLES;
            }
        }
        
        return DimensionsResult.EXACT;
    }

    /**
     * Checks if two 1D float arrays (vectors) represent valid audio samples.
     * This method checks if both arrays have the same number of samples.
     * 
     * @param a The first 1D float array to check.
     * @param b The second 1D float array to check.
     * @return An enumeration value indicating the result of the validation check.
     * @see DimensionsResult
     */
    public static DimensionsResult checkSamplesDimensions(float[] a, float[] b) {
        if (a == null || b == null) return DimensionsResult.INVALID_SAMPLES;
        if (a.length != b.length) return DimensionsResult.NOT_MATCH_SAMPLES;
        return DimensionsResult.EXACT;
    }

    /**
     * Validates the dimensions of two 2D float arrays, ensuring they have the same number of channels and samples.
     * If the validation check fails, an {@link IllegalArgumentException} is thrown.
     *
     * @param a The first 2D float array to validate.
     * @param b The second 2D float array to validate.
     * @throws IllegalArgumentException if the dimensions of the arrays do not match.
     */
    public static void validateSamplesDimensions(float[][] a, float[][] b) {
        if (checkSamplesDimensions(a, b) != DimensionsResult.EXACT) {
            throw new IllegalArgumentException("Input and output arrays must have the same number of channels and samples.");
        }
    }

    /**
     * Validates the dimensions of two 1D float arrays, ensuring they have the same number of samples.
     * If the validation check fails, an {@link IllegalArgumentException} is thrown.
     *
     * @param a The first 1D float array to validate.
     * @param b The second 1D float array to validate.
     * @throws IllegalArgumentException if the dimensions of the arrays do not match.
     */
    public static void validateSamplesDimensions(float[] a, float[] b) {
        if (checkSamplesDimensions(a, b) != DimensionsResult.EXACT) {
            throw new IllegalArgumentException("Input and output arrays must have the same number of samples.");
        }
    }

    /**
     * Checks if the provided 1D float array represents valid audio samples.
     * <p>If the array is null or empty, the method returns corresponding ValidationResult.
     *
     * @param samples The 1D float array representing audio samples, in the {@code float[samples]} format.
     * @return ValidationResult indicating the validity of the samples.
     */
    public static ValidationResult isValidSamples(float[] samples) {
        if (samples == null) return ValidationResult.NULL_ARRAY;
        if (samples.length == 0) return ValidationResult.EMPTY_ARRAY;
        return ValidationResult.VALID;
    }

    /**
     * Checks if the provided 2D float array represents valid audio samples.
     * <p>If the array is null, empty, or contains null or empty channels, the method returns corresponding ValidationResult.
     *
     * @param samples The 2D float array representing audio samples, in the {@code float[channels][samples]} format.
     * @return ValidationResult indicating the validity of the samples.
     */
    public static ValidationResult isValidSamples(float[][] samples) {
        if (samples == null) return ValidationResult.NULL_ARRAY;
        if (samples.length == 0) return ValidationResult.EMPTY_ARRAY;
        for (float[] ch : samples) {
            if (ch == null) return ValidationResult.NULL_CHANNEL;
            if (ch.length == 0) return ValidationResult.EMPTY_CHANNEL;
        }
        return ValidationResult.VALID;
    }

    /**
     * Validates the provided 1D float array representing audio samples.
     * <p>This method throws an IllegalArgumentException if the array is invalid, determined by {@link #isValidSamples(float[])} method.
     * <p>Throws IllegalArgumentException if the array is null or empty.
     *
     * @param samples The 1D float array representing audio samples, in the {@code float[samples]} format.
     * @throws IllegalArgumentException if the array is null or empty.
     */
    public static void validateSamples(float[] samples) throws IllegalArgumentException {
        switch (isValidSamples(samples)) {
            case VALID:
                return;
            case NULL_ARRAY:
                throw new IllegalArgumentException("Samples array cannot be null.");
            case EMPTY_ARRAY:
                throw new IllegalArgumentException("Samples array cannot be empty.");
            default:
                throw new IllegalStateException("Unexpected validation result.");
        }
    }

    /**
     * Validates the provided 2D float array representing audio samples.
     * <p>This method throws an IllegalArgumentException if the array is invalid, determined by {@link #isValidSamples(float[][])} method.
     * <p>Throws IllegalArgumentException if the array is null, empty, or contains null or empty channels.
     *
     * @param samples The 2D float array representing audio samples, in the {@code float[channels][samples]} format.
     * @throws IllegalArgumentException if the array is null, empty, or contains null or empty channels.
     */
    public static void validateSamples(float[][] samples) throws IllegalArgumentException {
        switch (isValidSamples(samples)) {
            case VALID:
                return;
            case NULL_ARRAY:
                throw new IllegalArgumentException("Samples array cannot be null.");
            case EMPTY_ARRAY:
                throw new IllegalArgumentException("Samples array cannot be empty.");
            case NULL_CHANNEL:
                throw new IllegalArgumentException("Samples array cannot contain null channels.");
            case EMPTY_CHANNEL:
                throw new IllegalArgumentException("Samples array cannot contain empty channels.");
            default:
                throw new IllegalStateException("Unexpected validation result.");
        }
    }

    /**
     * Checks if all channels in the samples array have the same length.
     * It uses the first channel length as the expected length.
     *
     * @param samples The 2D float array representing audio samples, in the {@code float[channels][samples]} format.
     * @return true if all channels have the same length, false otherwise.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static boolean checkLength(float[][] samples) {
        return checkLength(samples, samples[0].length);
    }

    /**
     * Checks if all channels in the samples array have the specified length.
     *
     * @param samples The 2D float array representing audio samples, in the {@code float[channels][samples]} format.
     * @param length The expected length of each channel.
     * @return true if all channels have the specified length, false otherwise.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static boolean checkLength(float[][] samples, int length) {
        validateSamples(samples);
        for (int ch = 0; ch < samples.length; ch++) {
            if (samples[ch].length != length) {
                return false;
            }
        }
        return true;
    }
}