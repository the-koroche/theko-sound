package org.theko.sound.resampling;

/**
 * LanczosResampleMethod implements a Lanczos resampling algorithm.
 * This method uses the Lanczos kernel for interpolation,
 * known for its high-quality interpolation in audio and image processing.
 * 
 * @since v1.4.1
 * @author Theko
 */
public class LanczosResampleMethod implements ResampleMethod {
    
    @Override
    public float[] resample (float[] input, int targetLength, int quality) {
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

    private float lanczosKernel (float x, int a) {
        if (x == 0) return 1; // The central sample is fully weighted
        if (Math.abs(x) >= a) return 0; // Outside the window, no contribution
        // Apply the Lanczos formula
        return (float) (Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a) / (Math.PI * Math.PI * x * x / a));
    }
}
