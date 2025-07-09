package org.theko.sound.resampling;

/**
 * CubicResampleMethod implements a cubic resampling algorithm
 * using Catmull-Rom splines for smooth interpolation.
 * This method is suitable for high-quality audio resampling,
 * providing a good balance between performance and quality.
 * 
 * @since v2.0.0
 * @author Theko
 * 
 * @see ResampleMethod
 * @see LinearResampleMethod
 */
public class CubicResampleMethod implements ResampleMethod {

    @Override
    public float[] resample (float[] input, int targetLength, int quality) {
        float[] output = new float[targetLength];
        float scale = (float) input.length / targetLength;

        for (int i = 0; i < targetLength; i++) {
            float pos = i * scale;
            int i1 = (int) pos;
            float t = pos - i1;

            // Get 4 sample points: p0, p1, p2, p3
            float p0 = getSample(input, i1 - 1);
            float p1 = getSample(input, i1);
            float p2 = getSample(input, i1 + 1);
            float p3 = getSample(input, i1 + 2);

            output[i] = catmullRom(p0, p1, p2, p3, t);
        }

        return output;
    }

    private float getSample (float[] input, int index) {
        if (index < 0) return input[0];
        if (index >= input.length) return input[input.length - 1];
        return input[index];
    }

    // Catmull-Rom spline interpolation
    private float catmullRom (float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        return 0.5f * (
            2 * p1 +
            (p2 - p0) * t +
            (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
            (3 * p1 - p0 - 3 * p2 + p3) * t3
        );
    }
}