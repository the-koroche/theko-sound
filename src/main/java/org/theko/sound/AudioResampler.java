package org.theko.sound;

public class AudioResampler {
    private AudioResampler () {
    }

    public static byte[] resample(byte[] data, AudioFormat sourceFormat, float speedMultiplier) {
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        float[][] samples = SampleConverter.toSamples(data, sourceFormat);
        resample(samples, sourceFormat, speedMultiplier);

        return SampleConverter.fromSamples(samples, sourceFormat);
    }

    public static float[][] resample(float[][] samples, AudioFormat sourceFormat, float speedMultiplier) {
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = timeScale(samples[ch], speedMultiplier);
        }
        return samples;
    }

    private static float[] timeScale(float[] input, float speedMultiplier) {
        int newLength = (int) (input.length / speedMultiplier);
        return lanczosResample(input, newLength, 3);
    }

    public static float[] lanczosResample(float[] input, int targetLength, int a) {
        float[] output = new float[targetLength];
        for (int i = 0; i < targetLength; i++) {
            float index = (float) i * input.length / targetLength;
            int i0 = (int) Math.floor(index);
            output[i] = 0;
            
            for (int j = -a + 1; j <= a; j++) {
                int idx = i0 + j;
                if (idx >= 0 && idx < input.length) {
                    output[i] += input[idx] * lanczosKernel(index - idx, a);
                }
            }
        }
        return output;
    }

    private static float lanczosKernel(float x, int a) {
        if (x == 0) return 1;
        if (Math.abs(x) >= a) return 0;
        return (float) (Math.sin(Math.PI * x) * Math.sin(Math.PI * x / a) / (Math.PI * Math.PI * x * x / a));
    }
}
