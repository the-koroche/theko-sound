package org.theko.sound.resampling;

/**
 * The LanczosResampler class implements the ResamplerMethod interface and provides
 * functionality for resampling an input array of audio samples to a target length
 * using the Lanczos resampling algorithm. This algorithm is known for its high-quality
 * interpolation, particularly in audio and image processing.
 *
 * <p>The resampling process involves calculating the value of each sample in the
 * target array by applying the Lanczos kernel to a window of samples from the input
 * array. The quality of the resampling is determined by the size of the window,
 * which is controlled by the `quality` parameter.</p>
 *
 * <p>Key methods:</p>
 * <ul>
 *   <li>{@link #resample(float[], int, int)} - Resamples the input array to the specified target length.</li>
 *   <li>{@link #lanczosKernel(float, int)} - Computes the Lanczos kernel value for a given offset and window size.</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     LanczosResampler resampler = new LanczosResampler();
 *     float[] input = { ... }; // Input audio samples
 *     int targetLength = 44100; // Desired output length
 *     int quality = 3; // Quality level (higher values increase computation time)
 *     float[] output = resampler.resample(input, targetLength, quality);
 * </pre>
 *
 * <p>Note: The quality parameter determines the size of the interpolation window.
 * Higher values result in better quality but increase computational cost.</p>
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class LanczosResampler implements ResamplerMethod {
    @Override
        public float[] resample(float[] input, int targetLength, int quality) {
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
                for (int j = -quality + 1; j <= quality; j++) {
                    int idx = i0 + j;
                    if (idx >= 0 && idx < input.length) {
                        // Apply the Lanczos kernel to the sample
                        output[i] += input[idx] * lanczosKernel(index - idx, quality);
                    }
                }
            }

            return output;
        }

        private float lanczosKernel(float x, int a) {
            if (x == 0) return 1; // The central sample is fully weighted
            if (Math.abs(x) >= a) return 0; // Outside the window, no contribution
            // Apply the Lanczos formula
            return (float) (Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a) / (Math.PI * Math.PI * x * x / a));
        }
}
