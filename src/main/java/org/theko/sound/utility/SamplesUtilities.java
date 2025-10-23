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

import org.theko.sound.samples.SamplesValidation;

/**
 * Utility class for audio sample manipulation.
 * <p>This class provides methods to reverse polarity, swap channels, reverse samples,
 * apply stereo separation, normalize samples, and adjust gain and pan.
 * 
 * @author Theko
 * @since 2.0.0
 */
public final class SamplesUtilities {

    private static final float PAN_EPSILON = 0.000001f;
    private static final float GAIN_EPSILON = 0.000001f;
    
    private SamplesUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Reverses the polarity of the audio samples.
     * This method negates each sample in the provided 1D float array.
     *
     * @param samples The audio samples to reverse polarity.
     * @return A new 1D float array with reversed polarity.
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
     * This method negates each sample in the provided 2D float array.
     *
     * @param samples The audio samples to reverse polarity.
     * @return A new 2D float array with reversed polarity.
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
     * Reverses the order of samples in the audio samples.
     * This method reverses the samples in the provided 1D float array.
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
     * Reverses the order of samples in each channel of the audio samples.
     * This method reverses the samples in each channel of the provided 2D float array.
     *
     * @param samples The audio samples to reverse.
     * @return A new 2D float array with reversed samples in each channel.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] reverse(float[][] samples) {
        SamplesValidation.validateSamples(samples);
        
        float[][] reversed = new float[samples.length][samples[0].length];
        for (int i = 0; i < samples.length; i++) {
            for (int j = 0; j < samples[i].length; j++) {
                reversed[i][j] = samples[i][samples[i].length - 1 - j];
            }
        }
        return reversed;
    }

    /**
     * Separates stereo samples into mid and side channels based on the specified separation factor.
     * <p>
     * This method applies a stereo separation effect to the provided 2D float array of samples.
     *
     * @param samples The stereo audio samples to separate, represented as a 2D float array.
     * @param separation The separation factor, typically in the range [-1, 1].
     * @return A new 2D float array with separated mid and side channels.
     * @throws IllegalArgumentException if the samples array is null, does not contain at least two channels,
     *                                  or if the channels do not have the same length.
     */
    public static float[][] stereoSeparation(float[][] samples, float separation) {
        SamplesValidation.validateSamples(samples);
        if (samples.length < 2)
            throw new IllegalArgumentException("Samples must contain at least two channels.");

        if (!SamplesValidation.checkLength(samples)) {
            throw new IllegalArgumentException("Channels must have the same length.");
        }

        if (samples.length != 2)
            throw new IllegalArgumentException("This method only supports stereo (2-channel) samples.");

        float normalizedSeparation = Math.max(-1.0f, Math.min(separation, 1.0f));
        float amount = (normalizedSeparation + 1.0f) / 2.0f; // [-1, 1] → [0, 1]

        int frameCount = samples[0].length;
        float[][] separated = new float[2][frameCount];

        for (int i = 0; i < frameCount; i++) {
            float left = samples[0][i];
            float right = samples[1][i];

            float mid = (left + right) / 2.0f;
            float side = (left - right) / 2.0f;

            separated[0][i] = mid + side * amount;
            separated[1][i] = mid - side * amount;
        }

        return separated;
    }

    /**
     * Normalizes the audio samples to a maximum absolute volume of 1.0.
     * This method scales each sample in the provided 1D float array by the maximum absolute volume.
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
     * Adjusts the gain and pan of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain and pan values.
     *
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @param pan The pan value to apply. A value of 0.0f is center, -1.0f is left, and 1.0f is right.
     * @return A new 2D float array with adjusted samples.
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
    
    /**
     * Adjusts the gain and pan of the audio samples.
     * This method scales each sample in the provided 2D float array by the specified gain and pan values.
     *
     * @param samples The audio samples to adjust, represented as a 2D float array.
     * @param output The output array to store the adjusted samples.
     * @param gain The gain value to apply. A value of 1.0f leaves the volume unchanged.
     * @param pan The pan value to apply. A value of 0.0f is center, -1.0f is left, and 1.0f is right.
     * @return true if the samples were changed, false otherwise
     * @throws IllegalArgumentException if:
     *         <ul>
     *             <li>the samples array is null or empty,</li>
     *             <li>the output array is null or empty,</li>
     *             <li>the output array does not have the same number of channels as the input array,</li>
     *             <li>or the input and output arrays do not have the same number of samples.</li>
     *         </ul>
     */
    public static boolean adjustGainAndPan(float[][] samples, float[][] output, float gain, float pan) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);

        if (pan * pan <= PAN_EPSILON && gain * gain <= GAIN_EPSILON) {
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
        if (pan * pan > PAN_EPSILON) {
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

    private static float getVolumeForChannel(int ch, float gain, float leftVol, float rightVol) {
        if (ch == 0) return gain * leftVol;
        if (ch == 1) return gain * rightVol;
        return gain;
    }
}
