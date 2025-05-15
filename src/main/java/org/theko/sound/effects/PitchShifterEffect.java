package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatControl;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;

public class PitchShifterEffect extends AudioEffect {
    private FloatControl pitchControl;

    public PitchShifterEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.pitchControl = new FloatControl("Pitch", -12.0f, 12.0f, 0.0f);
    }

    @Override
    public float[][] process(float[][] samples) {
        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = pitchShift(samples[ch], pitchControl.getValue(), 2048, 1024);
        }
        return samples;
    }

    private float[] pitchShift(float[] input, float semitones, int fftSize, int hopSize) {
        int len = input.length;
        float[] output = new float[len];

        double pitchRatio = Math.pow(2.0, semitones / 12.0);
        float[] window = WindowFunction.generate(fftSize, WindowType.HAMMING);
        float[] windowBuf = new float[fftSize];

        for (int pos = 0; pos + fftSize <= len; pos += hopSize) {
            // Apply window
            for (int i = 0; i < fftSize; i++) {
                windowBuf[i] = input[pos + i] * window[i];
            }

            // FFT
            float[] real = new float[fftSize];
            float[] imag = new float[fftSize];
            System.arraycopy(windowBuf, 0, real, 0, fftSize);
            FFT.fft(real, imag);

            // Pitch scale frequencies
            float[] newReal = new float[fftSize];
            float[] newImag = new float[fftSize];
            for (int i = 0; i < fftSize / 2; i++) {
                int newIndex = (int) (i / pitchRatio);
                if (newIndex < fftSize / 2) {
                    newReal[i] = real[newIndex];
                    newImag[i] = imag[newIndex];
                } else {
                    newReal[i] = 0;
                    newImag[i] = 0;
                }
                if (i > 0) {
                    newReal[fftSize - i] = newReal[i];
                    newImag[fftSize - i] = -newImag[i];
                }
            }

            // IFFT
            FFT.ifft(newReal, newImag);
            for (int i = 0; i < fftSize; i++) {
                float value = newReal[i] * window[i];
                if (pos + i < output.length) {
                    output[pos + i] += value;
                }
            }
        }

        return output;
    }
}