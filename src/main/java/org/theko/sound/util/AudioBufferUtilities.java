/*
 * Copyright 2025-2026 Alex Soloviov (aka Theko)
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

package org.theko.sound.util;

import java.util.Arrays;

import org.theko.sound.samples.SamplesValidation;

/**
 * Utility class for performing operations on audio sample buffers.
 * <p>This class provides methods to manipulate 1D and 2D float arrays representing audio samples.
 * It includes operations such as padding, copying, cloning, filling, reversing, polarity inversion,
 * channel swapping, gain adjustment, panning, and normalization. Methods are provided both for
 * mutating existing arrays and creating new arrays with the modified data.
 * 
 * @since 0.3.0-beta
 * @author Theko
 */
public final class AudioBufferUtilities {
    
    private AudioBufferUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Pads a 2D float array to a new specified length in both dimensions.
     * If the original array is smaller than the new dimensions, it will be padded
     * with either the last available element in each row or with a default value (0.0f).
     * 
     * @param original The original 2D float array to pad.
     * @param newLengthD1 The new length for the first dimension (rows).
     * @param newLengthD2 The new length for the second dimension (columns).
     * @param fillNewWithLast If true, the new array will be padded with the last available element in each row.
     * If false, the new array will be padded with the default value (0.0f).
     * @return A new 2D float array padded to the specified dimensions.
     * @throws IllegalArgumentException if the original array is null or if the new lengths are less than or equal to zero.
     */
    public static float[][] padArray(float[][] original, int newLengthD1, int newLengthD2, boolean fillNewWithLast) {
        if (original == null) throw new IllegalArgumentException("Original array cannot be null.");
        if (newLengthD1 <= 0 || newLengthD2 <= 0) throw new IllegalArgumentException("New lengths must be > 0.");

        float[][] padded = new float[newLengthD1][newLengthD2];

        float defaultValue = 0.0f; // default value
        float lastOriginalValue = 0.0f;

        // find the last value in the original array
        if (original.length > 0) {
            for (int r = original.length - 1; r >= 0; r--) {
                if (original[r] != null && original[r].length > 0) {
                    lastOriginalValue = original[r][original[r].length - 1];
                    break;
                }
            }
        }

        for (int i = 0; i < newLengthD1; i++) {
            float[] row = new float[newLengthD2];
            float[] srcRow = i < original.length ? original[i] : null;

            float fillValue = fillNewWithLast 
                    ? (srcRow != null && srcRow.length > 0 ? srcRow[srcRow.length - 1] : lastOriginalValue)
                    : defaultValue;

            if (srcRow != null) {
                int copyLength = Math.min(srcRow.length, newLengthD2);
                System.arraycopy(srcRow, 0, row, 0, copyLength);

                for (int j = copyLength; j < newLengthD2; j++) {
                    row[j] = fillValue;
                }
            } else {
                for (int j = 0; j < newLengthD2; j++) {
                    row[j] = fillValue;
                }
            }

            padded[i] = row;
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
     * @throws IllegalArgumentException if the source or target arrays are null or if the channel counts do not match.
     */
    public static void copyArray(float[][] source, float[][] target) {
        SamplesValidation.validateSamples(source);
        SamplesValidation.validateSamples(target);
        if (source.length != target.length) {
            throw new IllegalArgumentException("Channel count does not match.");
        }
        for (int ch = 0; ch < source.length; ch++) {
            int min = Math.min(source[ch].length, target[ch].length);
            System.arraycopy(source[ch], 0, target[ch], 0, min);
        }
    }

    /**
     * Creates a deep copy of a 2D float array (matrix).
     * Each channel in the source array is cloned separately.
     * The resulting array is a new object with identical contents to the source array.
     * 
     * @param source The source 2D float array to clone.
     * @return A new 2D float array containing a deep copy of the source array.
     * @throws IllegalArgumentException if the source array is null or empty.
     */
    public static float[][] cloneArray(float[][] source) {
        SamplesValidation.validateSamples(source);
        float[][] clone = new float[source.length][];
        for (int ch = 0; ch < source.length; ch++) {
            clone[ch] = source[ch].clone();
        }
        return clone;
    }

    /**
     * Fills a 2D float array (matrix) with a specified value.
     * This method sets all elements in each channel to the specified value.
     * 
     * @param samples The 2D float array to fill with the specified value.
     * @param value The value to fill the array with.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static void fill(float[][] samples, float value) {
        SamplesValidation.validateSamples(samples);
        for (int ch = 0; ch < samples.length; ch++) {
            Arrays.fill(samples[ch], value);
        }
    }

    // Audio-specific operations

    /**
     * Reverses the polarity of the audio samples.
     * This method negates each sample in the provided 1D float array.
     * The output array is filled with the negated values.
     * 
     * @param samples The audio samples to reverse polarity.
     * @param output The output array to fill with negated values.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void reversePolarity(float[] samples, float[] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            output[i] = -samples[i];
        }
    }

    /**
     * Reverses the polarity of the audio samples.
     * This method negates each sample in the provided 1D float array.
     * The output array is filled with the negated values.
     * 
     * @param samples The audio samples to reverse polarity.
     * @return A new 1D float array with negated values.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[] reversePolarity(float[] samples) {
        SamplesValidation.validateSamples(samples);
        float[] reversed = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            reversed[i] = -samples[i];
        }
        return reversed;
    }

    /**
     * Reverses the polarity of the audio samples.
     * This method negates each sample in the provided 2D float array and writes the negated values to the provided output array.
     * 
     * @param samples The audio samples to reverse polarity.
     * @param output The output array to fill with negated values.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void reversePolarity(float[][] samples, float[][] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                output[i][j] = -samples[i][j];
            }
        }
    }

    /**
     * Reverses the polarity of the audio samples.
     * This method negates each sample in the provided 2D float array.
     * The output array is filled with the negated values.
     * 
     * @param samples The audio samples to reverse polarity.
     * @return A new 2D float array with negated values.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] reversePolarity(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float[][] reversed = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            reversed[i] = new float[samples[i].length];
            for (int j = 0; j < samples[i].length; j++) {
                reversed[i][j] = -samples[i][j];
            }
        }
        return reversed;
    }

    /**
     * Swaps the channels of the audio samples.
     * This method reverses the order of channels in the provided 2D float array.
     * The output array is filled with the swapped channels.
     * 
     * @param samples The audio samples to swap channels.
     * @param output The output array to fill with swapped channels.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void swapChannels(float[][] samples, float[][] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            output[i] = samples[samples.length - 1 - i].clone();
        }
    }

    /**
     * Swaps the channels of the audio samples.
     * This method reverses the order of channels in the provided 2D float array.
     * The output array is filled with the swapped channels.
     * 
     * @param samples The audio samples to swap channels.
     * @return A new 2D float array with swapped channels.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] swapChannels(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float[][] swapped = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            swapped[i] = samples[samples.length - 1 - i].clone();
        }
        return swapped;
    }

    /**
     * Reverses the order of samples in the provided 1D float array.
     * This method fills the output array with the reversed samples.
     * 
     * @param samples The audio samples to reverse.
     * @param output The output array to fill with reversed samples.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void reverse(float[] samples, float[] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            output[i] = samples[samples.length - 1 - i];
        }
    }

    /**
     * Reverses the order of samples in the provided 1D float array.
     * This method creates a new 1D float array with the reversed samples.
     * 
     * @param samples The audio samples to reverse.
     * @return A new 1D float array with reversed samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[] reverse(float[] samples) {
        SamplesValidation.validateSamples(samples);
        float[] reversed = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            reversed[i] = samples[samples.length - 1 - i];
        }
        return reversed;
    }

    /**
     * Reverses the order of samples in the provided 2D float array.
     * This method fills the output array with the reversed samples.
     * 
     * @param samples The audio samples to reverse.
     * @param output The output array to fill with reversed samples.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void reverse(float[][] samples, float[][] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                output[i][j] = samples[i][samples[i].length - 1 - j];
            }
        }
    }

    /**
     * Reverses the order of samples in the provided 2D float array.
     * This method creates a new 2D float array with the reversed samples.
     * 
     * @param samples The audio samples to reverse.
     * @return A new 2D float array with reversed samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] reverse(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float[][] reversed = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            reversed[i] = new float[samples[i].length];
            for (int j = 0; j < samples[i].length; j++) {
                reversed[i][j] = samples[i][samples[i].length - 1 - j];
            }
        }
        return reversed;
    }

    // Volume analysis

    /**
     * Calculates the maximum absolute volume of the audio samples.
     * This method iterates through all samples in the provided 1D float array
     * and returns the maximum absolute value found.
     * 
     * @param samples The audio samples to analyze, represented as a 1D float array.
     * @return The maximum absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsMaxVolume(float[] samples) {
        SamplesValidation.validateSamples(samples);
        float max = 0.0f;
        for (float sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        return max;
    }

    /**
     * Calculates the maximum absolute volume of the audio samples.
     * This method iterates through all samples in the provided 2D float array
     * and returns the maximum absolute value found.
     * 
     * @param samples The audio samples to analyze, represented as a 2D float array.
     * @return The maximum absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsMaxVolume(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float max = 0.0f;
        for (float[] channel : samples) {
            for (float sample : channel) {
                max = Math.max(max, Math.abs(sample));
            }
        }
        return max;
    }

    /**
     * Calculates the average absolute volume of the audio samples.
     * This method iterates through all samples in the provided 1D float array
     * and returns the average absolute value found.
     * 
     * @param samples The audio samples to analyze, represented as a 1D float array.
     * @return The average absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsAvgVolume(float[] samples) {
        SamplesValidation.validateSamples(samples);
        float sum = 0.0f;
        int count = 0;
        for (float sample : samples) {
            sum += Math.abs(sample);
            count++;
        }
        return sum / count;
    }

    /**
     * Calculates the average absolute volume of the audio samples.
     * This method iterates through all samples in the provided 2D float array
     * and returns the average absolute value found.
     * 
     * @param samples The audio samples to analyze, represented as a 2D float array.
     * @return The average absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsAvgVolume(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float sum = 0.0f;
        int count = 0;
        for (float[] channel : samples) {
            for (float sample : channel) {
                sum += Math.abs(sample);
                count++;
            }
        }
        return sum / count;
    }

    /**
     * Normalizes the audio samples to a maximum absolute volume of 1.0.
     * This method scales each sample in the provided 1D float array by the maximum absolute volume.
     * The output array is filled with the normalized samples.
     * 
     * @param samples The audio samples to normalize, represented as a 1D float array.
     * @param output The output array to fill with normalized samples.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void normalize(float[] samples, float[] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        float max = getAbsMaxVolume(samples);
        if (max < 1e-6f) {
            return; // Avoid division by zero
        }
        for (int i = 0; i < samples.length; i++) {
            output[i] = samples[i] / max;
        }
    }

    /**
     * Normalizes the audio samples to a maximum absolute volume of 1.0.
     * This method scales each sample in the provided 1D float array by the maximum absolute volume.
     * The output array is filled with the normalized samples.
     * 
     * @param samples The audio samples to normalize, represented as a 1D float array.
     * @return A new 1D float array with normalized samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[] normalize(float[] samples) {
        SamplesValidation.validateSamples(samples);
        float max = getAbsMaxVolume(samples);
        if (max < 1e-6f) {
            return samples; // Avoid division by zero
        }
        float[] normalized = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            normalized[i] = samples[i] / max;
        }
        return normalized;
    }

    /**
     * Normalizes the audio samples to a maximum absolute volume of 1.0.
     * This method scales each sample in the provided 2D float array by the maximum absolute volume.
     * The output array is filled with the normalized samples.
     * 
     * @param samples The audio samples to normalize, represented as a 2D float array.
     * @param output The output array to fill with normalized samples.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void normalize(float[][] samples, float[][] output) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        float max = getAbsMaxVolume(samples);
        if (max < 1e-6f) {
            return; // Avoid division by zero
        }
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                output[i][j] = samples[i][j] / max;
            }
        }
    }

    /**
     * Normalizes the audio samples to a maximum absolute volume of 1.0.
     * This method scales each sample in the provided 2D float array by the maximum absolute volume.
     * The output array is filled with the normalized samples.
     * 
     * @param samples The audio samples to normalize, represented as a 2D float array.
     * @return A new 2D float array with normalized samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] normalize(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        float max = getAbsMaxVolume(samples);
        if (max < 1e-6f) {
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

    /**
     * Adjusts the gain of the audio samples.
     * This method scales each sample in the provided 1D float array by the specified gain.
     * The output array is filled with the adjusted samples.
     * 
     * @param samples The audio samples to adjust, represented as a 1D float array.
     * @param output The output array to fill with adjusted samples.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @throws IllegalArgumentException if the samples or output array is null or empty or if dimensions do not match.
     */
    public static void adjustGain(float[] samples, float[] output, float gain) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            output[i] = samples[i] * gain;
        }
    }

    /**
     * Adjusts the gain of the audio samples.
     * This method scales each sample in the provided 1D float array by the specified gain.
     * The output array is filled with the adjusted samples.
     * 
     * @param samples The audio samples to adjust, represented as a 1D float array.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @return A new 1D float array with adjusted samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[] adjustGain(float[] samples, float gain) {
        SamplesValidation.validateSamples(samples);
        float[] adjusted = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            adjusted[i] = samples[i] * gain;
        }
        return adjusted;
    }

    /**
     * Adjusts the gain of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain.
     * The output array is filled with the adjusted samples.
     * 
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param output The output array to fill with adjusted samples.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @throws IllegalArgumentException if the samples or output array is null or empty, or if dimensions do not match.
     */
    public static void adjustGain(float[][] samples, float[][] output, float gain) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                output[i][j] = samples[i][j] * gain;
            }
        }
    }

    /**
     * Adjusts the gain of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain.
     * The output array is filled with the adjusted samples.
     * 
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @return A new 2D float array with adjusted samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] adjustGain(float[][] samples, float gain) {
        SamplesValidation.validateSamples(samples);
        float[][] adjusted = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            adjusted[i] = new float[samples[i].length];
            for (int j = 0; j < samples[i].length; j++) {
                adjusted[i][j] = samples[i][j] * gain;
            }
        }
        return adjusted;
    }
    
    /**
     * Adjusts the gain and pan of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain and pan values.
     * The output array is filled with the adjusted samples.
     * <p>If the samples contain more than two channels (i.e., stereo),
     * then the pan value is applied only to the left and right channels;
     * additional channels remain unaffected.
     * 
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param output The output array to fill with adjusted samples.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @param pan The pan value to apply. A value of 0.0f is center, -1.0f is left, and 1.0f is right.
     * @return true if the samples were adjusted, false otherwise.
     * @throws IllegalArgumentException if the samples or output array is null or empty, or if dimensions do not match.
     */
    public static boolean adjustGainAndPan(float[][] samples, float[][] output, float gain, float pan) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        SamplesValidation.validateSamplesDimensions(samples, output);

        if (pan == 0.0f && gain == 1.0f) {
            return false;
        }

        for (int ch = 0; ch < samples.length; ch++) {
            if (output[ch] == null || samples[ch] == null || output[ch].length != samples[ch].length) {
                throw new IllegalArgumentException("Input and output arrays must have the same number of samples.");
            }
            System.arraycopy(samples[ch], 0, output[ch], 0, samples[ch].length);
        }

        float leftVol = 1.0f;
        float rightVol = 1.0f;
        if (pan != 0.0f) {
            float angle = (float)((pan + 1.0f) * Math.PI / 4.0f);
            leftVol  = (float)Math.cos(angle);
            rightVol = (float)Math.sin(angle); 
        }

        for (int ch = 0; ch < samples.length; ch++) {
            float channelVol = getVolumeForChannel(ch, gain, leftVol, rightVol);
            float[] channelSamples = output[ch];
            for (int frame = 0; frame < channelSamples.length; frame++) {
                channelSamples[frame] *= channelVol;
            }
        }

        return true;
    }

    /**
     * Returns the volume for a given channel.
     * This method applies the gain and pan values to a channel and returns the resulting volume.
     * If channel index is out of range (stereo), it returns the gain value.
     * 
     * @param ch The channel to calculate the volume for (0 or 1).
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @param leftVol The volume for the left channel.
     * @param rightVol The volume for the right channel.
     * @return The volume for the given channel.
     */
    private static float getVolumeForChannel(int ch, float gain, float leftVol, float rightVol) {
        if (ch == 0) return gain * leftVol;
        if (ch == 1) return gain * rightVol;
        return gain;
    }

    /**
     * Adjusts the gain and pan of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain and pan values.
     * The output array is filled with the adjusted samples.
     * <p>If the samples contain more than two channels (i.e., stereo),
     * then the pan value is applied only to the left and right channels;
     * additional channels remain unaffected.
     * 
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @param pan The pan value to apply. A value of 0.0f is center, -1.0f is left, and 1.0f is right.
     * @return A new 2D float array with adjusted samples, or the original samples if no adjustment was made.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] adjustGainAndPan(float[][] samples, float gain, float pan) {
        SamplesValidation.validateSamples(samples);

        float[][] output = new float[samples.length][];
        for (int i = 0; i < samples.length; i++) {
            output[i] = new float[samples[i].length];
        }
        boolean changed = adjustGainAndPan(samples, output, gain, pan);
        if (changed) {
            return output;
        } else {
            return samples;
        }
    }
}