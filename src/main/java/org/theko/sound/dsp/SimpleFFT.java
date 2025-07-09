package org.theko.sound.dsp;

public class SimpleFFT {
    public static FFTResult[] fft(float[] samples, float sampleRate) {
        float[] real = samples.clone();
        float[] imag = new float[samples.length];
        int n = samples.length;

        FFT.fft(real, imag);

        FFTResult[] results = new FFTResult[n / 2];
        for (int i = 0; i < n / 2; i++) {
            float frequency = (float) i * sampleRate / n;
            float amplitude = (float) Math.hypot(real[i], imag[i]);
            float phase = (float) Math.atan2(imag[i], real[i]);
            results[i] = new FFTResult(frequency, amplitude, phase);
        }
        return results;
    }

    public static float[] ifft(FFTResult[] fftResults) {
        int n = fftResults.length * 2;
        float[] real = new float[n];
        float[] imag = new float[n];

        for (int i = 0; i < fftResults.length; i++) {
            real[i] = fftResults[i].amplitude * (float) Math.cos(fftResults[i].phase);
            imag[i] = fftResults[i].amplitude * (float) Math.sin(fftResults[i].phase);
        }
        for (int i = 1; i < fftResults.length; i++) {
            int j = n - i;
            real[j] = real[i];
            imag[j] = -imag[i];
        }

        FFT.ifft(real, imag);

        return real; // already contains result
    }
}
