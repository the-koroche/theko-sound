package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatController;
import org.theko.sound.fft.FFT;

public class LowPassFilter extends AudioEffect {
    protected final FloatController lowPassFreq;

    public LowPassFilter(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        lowPassFreq = new FloatController("LOW-PASS FREQUENCY", 0, 22000, 2000);
    }

    @Override
    public float[][] process(float[][] samples) {
        float cutoffFrequency = lowPassFreq.getValue();
        float sampleRate = audioFormat.getSampleRate();
        
        // Обрабатываем каждый канал отдельно
        for (int channel = 0; channel < samples.length; channel++) {
            int n = samples[channel].length;
            int newSize = nextPowerOfTwo(n);
            
            // Создаем массивы для реальной и мнимой частей
            float[] real = new float[newSize];
            float[] imag = new float[newSize];
            
            // Копируем данные в реальную часть (мнимая часть остается нулевой)
            System.arraycopy(samples[channel], 0, real, 0, n);
            
            // Выполняем прямое БПФ преобразование
            FFT.fft(real, imag);
            
            // Применяем фильтр низких частот в частотной области
            applyLowPassFilter(real, imag, sampleRate, cutoffFrequency);
            
            // Выполняем обратное БПФ преобразование
            FFT.ifft(real, imag);
            
            // Копируем результат обратно в samples
            System.arraycopy(real, 0, samples[channel], 0, n);
        }
        
        return samples;
    }

    private void applyLowPassFilter(float[] real, float[] imag, float sampleRate, float cutoffFrequency) {
        int n = real.length;
        float binWidth = sampleRate / n;
        
        // Вычисляем индекс частоты среза
        int cutoffBin = Math.min((int) (cutoffFrequency / binWidth), n / 2);
        
        float smoothFactor = 5f; // 20% от cutoff для плавного спада  
        int smoothBins = (int) (cutoffBin * smoothFactor);  
        
        for (int i = cutoffBin; i < cutoffBin + smoothBins && i < n / 2; i++) {  
            float factor = 0.0f * (1 + (float) Math.cos(Math.PI * (i - cutoffBin) / (2 * smoothBins)));  
            real[i] *= factor;  
            imag[i] *= factor;  
            real[n - i] *= factor;  
            imag[n - i] *= factor;  
        }
        
        // Плавный спад амплитуды около частоты среза (опционально)
        if (cutoffBin > 0 && cutoffBin < n/2) {
            // Можно добавить плавный спад вместо резкого обрезания
            float fadeRatio = 0.1f; // 10% от cutoff для плавного спада
            int fadeBins = (int)(cutoffBin * fadeRatio);
            
            for (int i = cutoffBin - fadeBins; i <= cutoffBin + fadeBins && i < n/2; i++) {
                if (i > 0) {
                    float factor = 0.01f * (1 + (float)Math.cos(Math.PI * (i - cutoffBin + fadeBins) / (2 * fadeBins)));
                    real[i] *= factor;
                    imag[i] *= factor;
                    real[n - i] *= factor;
                    imag[n - i] *= factor;
                }
            }
        }
    }

    private void applyHannWindow(float[] data) {
        int n = data.length;
        for (int i = 0; i < n; i++) {
            data[i] *= 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (n - 1)));
        }
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power;
    }
}