package org.theko.sound.utility;

import org.theko.sound.LengthMismatchException;

/**
 * Utility class for audio sample manipulation.
 * <p>This class provides methods to reverse polarity, swap channels, reverse samples,
 * apply stereo separation, normalize samples, and adjust gain and pan.
 * 
 * @author Theko
 * @since v2.0.0
 */
public class SamplesUtilities {

    private static final float PAN_EPSILON = 0.000001f;
    private static final float GAIN_EPSILON = 0.000001f;
    
    private SamplesUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
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

    /**
     * Swaps the channels of the audio samples.
     * This method reverses the order of channels in the provided 2D float array.
     *
     * @param samples The audio samples to swap channels.
     * @return A new 2D float array with swapped channels.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
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

    /**
     * Reverses the order of samples in each channel of the audio samples.
     * This method reverses the samples in each channel of the provided 2D float array.
     *
     * @param samples The audio samples to reverse.
     * @return A new 2D float array with reversed samples in each channel.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
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

    /**
     * Separates stereo samples into mid and side channels based on the specified separation factor.
     * This method applies a stereo separation effect to the provided 2D float array of samples.
     *
     * @param samples The stereo audio samples to separate, represented as a 2D float array.
     * @param separation The separation factor, typically in the range [-1, 1].
     * @return A new 2D float array with separated mid and side channels.
     * @throws IllegalArgumentException if the samples array is null, does not contain at least two channels,
     *                                  or if the channels do not have the same length.
     */
    public static float[][] stereoSeparation(float[][] samples, float separation) {
        if (samples == null || samples.length < 2)
            throw new IllegalArgumentException("Samples must contain at least two channels.");

        try {
            checkLength(samples);
        } catch (LengthMismatchException ex) {
            throw new IllegalArgumentException("All channels must have the same length.", ex);
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
     * This method scales each sample in the provided 2D float array by the maximum absolute volume.
     *
     * @param samples The audio samples to normalize, represented as a 2D float array.
     * @return A new 2D float array with normalized samples.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float[][] normalize(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }

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
     * This method iterates through all samples in the provided 2D float array
     * and returns the maximum absolute value found.
     *
     * @param samples The audio samples to analyze, represented as a 2D float array.
     * @return The maximum absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsMaxVolume(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }

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
     * This method iterates through all samples in the provided 2D float array
     * and returns the average absolute value found.
     *
     * @param samples The audio samples to analyze, represented as a 2D float array.
     * @return The average absolute volume as a float.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static float getAbsAvgVolume(float[][] samples) {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }

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
     */
    @MutatesSamples
    public static void adjustGainAndPan (float[][] samples, float gain, float pan) {
        if (pan * pan <= PAN_EPSILON && gain * gain <= GAIN_EPSILON) return;

        float leftVol = 1.0f;
        float rightVol = 1.0f;
        if (pan * pan > PAN_EPSILON) {
            float angle = (float)((pan + 1.0f) * Math.PI / 4.0f);
            leftVol  = (float)Math.cos(angle);
            rightVol = (float)Math.sin(angle); 
        }

        for (int ch = 0; ch < samples.length; ch++) {
            float channelVol = getVolumeForChannel(ch, gain, leftVol, rightVol);
            float[] channelSamples = samples[ch];
            for (int frame = 0; frame < channelSamples.length; frame++) {
                channelSamples[frame] *= channelVol;
            }
        }
    }

    private static float getVolumeForChannel (int ch, float gain, float leftVol, float rightVol) {
        if (ch == 0) return gain * leftVol;
        if (ch == 1) return gain * rightVol;
        return gain;
    }

    /**
     * Checks if all channels in the samples array have the same length.
     * Throws a LengthMismatchException if any channel has a different length.
     *
     * @param samples The 2D float array representing audio samples.
     * @throws LengthMismatchException if any channel has a different length.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
    public static void checkLength(float[][] samples) throws LengthMismatchException {
        if (samples == null || samples.length == 0) {
            throw new IllegalArgumentException("Samples array cannot be null or empty.");
        }
        checkLength(samples, samples[0].length);
    }

    /**
     * Checks if all channels in the samples array have the specified length.
     * Throws a LengthMismatchException if any channel has a different length.
     *
     * @param samples The 2D float array representing audio samples.
     * @param length The expected length of each channel.
     * @throws LengthMismatchException if any channel has a different length.
     * @throws IllegalArgumentException if the samples array is null or empty.
     */
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
