package org.theko.sound.resampling;

import java.util.Arrays;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;

public class PhaseVocoderResampler implements ResampleMethod {

    private static final float TWO_PI = (float)(2 * Math.PI);

    @Override
    public float[] resample(float[] input, int targetLength, int quality) {
        int N = 1 << quality;           // FFT размер
        int Ha = N / 4;                 // шаг анализа
        float ratio = (float) targetLength / input.length;
        int Hs = Math.max(1, Math.round(Ha * ratio));  // шаг синтеза

        float[] window = WindowFunction.generate(N, WindowType.HANN);
        int numFrames = (input.length - N) / Ha;

        float[] output = new float[targetLength + N];
        float[] prevPhase = new float[N / 2 + 1];
        float[] sumPhase = new float[N / 2 + 1];

        float[] fftReal = new float[N];
        float[] fftImag = new float[N];
        float[] mag = new float[N / 2 + 1];
        float[] phase = new float[N / 2 + 1];

        for (int frame = 0; frame < numFrames; frame++) {
            int inPos = frame * Ha;
            int outPos = frame * Hs;

            // Анализ
            Arrays.fill(fftReal, 0);
            Arrays.fill(fftImag, 0);
            System.arraycopy(input, inPos, fftReal, 0, N);
            for (int i = 0; i < N; i++) fftReal[i] *= window[i];
            FFT.fft(fftReal, fftImag);

            // Магнитуда и текущая фаза
            for (int k = 0; k <= N / 2; k++) {
                float re = fftReal[k], im = fftImag[k];
                mag[k] = (float)Math.hypot(re, im);
                phase[k] = (float)Math.atan2(im, re);
            }

            // Instantaneous frequency
            for (int k = 0; k <= N / 2; k++) {
                float delta = phase[k] - prevPhase[k] - TWO_PI * Ha * k / N;
                delta = (float)(((delta + Math.PI) % TWO_PI) - Math.PI);
                float instFreq = TWO_PI * k / N + delta / Ha;
                sumPhase[k] += Hs * instFreq;
                prevPhase[k] = phase[k];
            }

            // Сборка спектра с новой фазой
            for (int k = 0; k <= N / 2; k++) {
                float re = mag[k] * (float)Math.cos(sumPhase[k]);
                float im = mag[k] * (float)Math.sin(sumPhase[k]);
                fftReal[k] = re;
                fftImag[k] = im;
                if (k > 0 && k < N / 2) {
                    fftReal[N - k] = re;
                    fftImag[N - k] = -im;
                }
            }

            // Обратное преобразование
            FFT.ifft(fftReal, fftImag);
            for (int i = 0; i < N; i++) {
                output[outPos + i] += fftReal[i] * window[i] / N;
            }
        }

        // Обрезаем до нужной длины
        float[] truncated = new float[targetLength];
        System.arraycopy(output, 0, truncated, 0, targetLength);
        return truncated;
    }
}
