package org.theko.sound.resampling;

/**
 * The LinearResampler class implements the ResamplerMethod interface and provides
 * functionality to resample an input array of floating-point audio samples to a
 * specified target length using linear interpolation.
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class LinearResampleMethod implements ResampleMethod {
    
    @Override
    public float[] resample (float[] input, int targetLength, int quality) {
        float[] output = new float[targetLength];
        float scale = (float) input.length / targetLength;
        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i0 = (int) pos;
            float t = pos - i0;
            
            if (i0 < 0) {
                output[i] = input[0];
            } else if (i0 >= input.length - 1) {
                output[i] = input[input.length - 1];
            } else {
                output[i] = input[i0] * (1 - t) + input[i0 + 1] * t;
            }
        }
        return output;
    }
}
