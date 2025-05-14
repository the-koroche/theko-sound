package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatControl;

import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;

public class LowPassFilter extends AudioEffect {
    private final FloatControl lowPassFreq;
    private final FloatControl release;

    public LowPassFilter(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        lowPassFreq = new FloatControl("Low-Pass Frequency", 0, 22000, 1000);
        release = new FloatControl("Release", 0, 1, 0.2f);
    }

    @Override
    protected float[][] process(float[][] samples) {
        int numChannels = samples.length;
        int frames = samples[0].length;
        int roundedFrames = 1 << (32 - Integer.numberOfLeadingZeros(frames - 1));
        float[] real = new float[roundedFrames];
        float[] imag = new float[roundedFrames];

        float sampleRate = getAudioFormat().getSampleRate();
        float cutoff = lowPassFreq.getValue();

        for (int ch = 0; ch < numChannels; ch++) {
            // Copy samples to fft buffer
            System.arraycopy(samples[ch], 0, real, 0, frames);
            for (int i = frames; i < roundedFrames; i++) real[i] = 0.0f;
            for (int i = 0; i < roundedFrames; i++) imag[i] = 0.0f;

            // Window
            WindowFunction.apply(real, WindowType.BLACKMAN_HARRIS);

            // FFT
            FFT.fft(real, imag);

            // Low pass filter
            int cutoffIndex = (int) (cutoff / sampleRate * roundedFrames);
            for (int i = cutoffIndex; i < roundedFrames / 2; i++) {
                real[i] = 0.0f;
                imag[i] = 0.0f;

                // symmetric mirror
                int mirror = roundedFrames - i;
                if (mirror < roundedFrames && mirror > i && mirror != i) {
                    real[mirror] = 0.0f;
                    imag[mirror] = 0.0f;
                }
            }

            // IFFT
            FFT.ifft(real, imag);

            // Copy filtered samples from fft buffer
            System.arraycopy(real, 0, samples[ch], 0, frames);
        }

        return samples;
    }

    public FloatControl getCutoff() {
        return lowPassFreq;
    }

    public FloatControl getRelease() {
        return release;
    }
}
