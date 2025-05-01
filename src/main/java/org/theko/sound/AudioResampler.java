package org.theko.sound;

/**
 * The AudioResampler class provides utility methods for resampling audio data.
 * It supports resampling audio at different speeds using a speed multiplier.
 * The class includes methods for converting audio data between byte arrays
 * and float samples, applying time scaling, and performing Lanczos resampling.
 * 
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Resampling audio data using a speed multiplier.</li>
 *   <li>Time scaling of audio samples for speed adjustment.</li>
 *   <li>High-quality Lanczos resampling algorithm for interpolation.</li>
 * </ul>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * {@code
 * byte[] resampledData = AudioResampler.resample(originalData, sourceFormat, speedMultiplier);
 * }
 * </pre>
 * 
 * <p><strong>Note:</strong> The class is not instantiable as it has a private constructor.</p>
 * 
 * <p><strong>Exceptions:</strong></p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - Thrown if the speed multiplier is less than or equal to 0.</li>
 * </ul>
 * 
 * @author Alex Soloviov
 */
public class AudioResampler {

    // Private constructor to prevent instantiation
    private AudioResampler () {
    }

    /**
     * Resamples audio data using a speed multiplier.
     * This method modifies the speed of the audio by the given multiplier.
     *
     * @param data The audio data to be resampled (raw byte data).
     * @param sourceFormat The audio format of the source data.
     * @param speedMultiplier The factor by which the audio speed will be modified.
     *                        A value greater than 1 speeds up the audio, and a value 
     *                        less than 1 slows it down.
     * @return A byte array containing the resampled audio data.
     * @throws IllegalArgumentException if the speed multiplier is less than or equal to 0.
     */
    public static byte[] resample(byte[] data, AudioFormat sourceFormat, float speedMultiplier) {
        // Validate speed multiplier
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        // Convert byte data to float samples (for manipulation)
        float[][] samples = SampleConverter.toSamples(data, sourceFormat);

        // Resample the samples with the specified speed multiplier
        resample(samples, sourceFormat, speedMultiplier);

        // Convert the resampled float samples back to byte data
        return SampleConverter.fromSamples(samples, sourceFormat);
    }

    /**
     * Resamples audio samples using a speed multiplier.
     *
     * @param samples The audio samples (as an array of float arrays, one for each channel).
     * @param sourceFormat The format of the audio data.
     * @param speedMultiplier The factor by which the audio speed will be modified.
     * @return A 2D array of resampled audio samples.
     * @throws IllegalArgumentException if the speed multiplier is less than or equal to 0.
     */
    public static float[][] resample(float[][] samples, AudioFormat sourceFormat, float speedMultiplier) {
        // Validate speed multiplier
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        // Resample each channel
        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = timeScale(samples[ch], speedMultiplier);
        }

        return samples;
    }

    /**
     * Applies time scaling to a single audio channel (array of samples).
     * This method modifies the length of the input based on the speed multiplier.
     *
     * @param input The original audio samples of one channel.
     * @param speedMultiplier The factor by which to change the length of the samples.
     * @return A new array of resampled audio samples for the given channel.
     */
    private static float[] timeScale(float[] input, float speedMultiplier) {
        // Calculate the new length after applying the speed multiplier
        int newLength = (int) (input.length / speedMultiplier);

        // Perform Lanczos resampling to obtain the new samples
        return lanczosResample(input, newLength, 3);
    }

    /**
     * Resamples audio data using the Lanczos resampling algorithm.
     * This method adjusts the length of the input array to match the target length
     * by interpolating the samples using the Lanczos kernel.
     *
     * @param input The original audio data to be resampled.
     * @param targetLength The desired length for the resampled audio data.
     * @param a The "a" parameter of the Lanczos kernel, controlling the number of taps.
     * @return A new array of resampled audio data.
     */
    public static float[] lanczosResample(float[] input, int targetLength, int a) {
        // Create an output array with the target length
        float[] output = new float[targetLength];

        // Iterate through each index in the target output array
        for (int i = 0; i < targetLength; i++) {
            // Compute the corresponding index in the original input array
            float index = (float) i * input.length / targetLength;
            int i0 = (int) Math.floor(index);

            // Reset the value at the current output index
            output[i] = 0;

            // Perform the Lanczos interpolation with a window around the current index
            for (int j = -a + 1; j <= a; j++) {
                int idx = i0 + j;
                if (idx >= 0 && idx < input.length) {
                    // Apply the Lanczos kernel to the sample
                    output[i] += input[idx] * lanczosKernel(index - idx, a);
                }
            }
        }

        return output;
    }

    /**
     * The Lanczos kernel function used for interpolation.
     * This kernel determines how the samples are weighted based on their distance
     * from the target index. A higher "a" value increases the sharpness of the kernel.
     *
     * @param x The distance between the target index and the current sample.
     * @param a The "a" parameter of the Lanczos kernel, controlling the number of taps.
     * @return The weight of the sample based on the Lanczos kernel.
     */
    private static float lanczosKernel(float x, int a) {
        if (x == 0) return 1; // The central sample is fully weighted
        if (Math.abs(x) >= a) return 0; // Outside the window, no contribution
        // Apply the Lanczos formula
        return (float) (Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a) / (Math.PI * Math.PI * x * x / a));
    }
}
