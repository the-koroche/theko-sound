package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatControl;

public class LowPassFilter extends AudioEffect {
    protected final FloatControl lowPassFreq;
    protected final FloatControl release;

    private float currentFc;
    private float[] x1, x2, y1, y2;
    private int channelsCount = 0;
    private boolean initialized = false;
    private FilterCoefficients prevCoeffs;
    private boolean coeffsInitialized = false;

    public LowPassFilter(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        lowPassFreq = new FloatControl("Low-Pass Frequency", 0, 22000, 1000);
        release = new FloatControl("Release", 0, 1, 0.2f);
        currentFc = lowPassFreq.getValue();
        prevCoeffs = new FilterCoefficients(0, 0, 0, 0, 0);
    }

    @Override
    public float[][] process(float[][] samples) {
        int numChannels = samples.length;
        if (numChannels == 0) return samples;

        int bufferSize = samples[0].length;

        // Инициализация состояний фильтра
        if (!initialized || channelsCount != numChannels) {
            initializeStates(numChannels);
        }

        // Плавное изменение частоты среза
        updateCutoffFrequency();

        // Интерполяция коэффициентов
        FilterCoefficients newCoeffs = calculateFilterCoefficients();
        FilterCoefficients interpolatedCoeffs = interpolateCoefficients(newCoeffs);

        // Обработка с интерполированными коэффициентами
        for (int c = 0; c < numChannels; c++) {
            processChannel(samples[c], c, interpolatedCoeffs);
        }
        
        prevCoeffs = newCoeffs;
        return samples;
    }

        private FilterCoefficients interpolateCoefficients(FilterCoefficients newCoeffs) {
        if (!coeffsInitialized) {
            coeffsInitialized = true;
            return newCoeffs;
        }
        
        float blend = 0.1f; // Регулировка плавности изменения
        return new FilterCoefficients(
            lerp(prevCoeffs.b0, newCoeffs.b0, blend),
            lerp(prevCoeffs.b1, newCoeffs.b1, blend),
            lerp(prevCoeffs.b2, newCoeffs.b2, blend),
            lerp(prevCoeffs.a1, newCoeffs.a1, blend),
            lerp(prevCoeffs.a2, newCoeffs.a2, blend)
        );
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private void initializeStates(int numChannels) {
        x1 = new float[numChannels];
        x2 = new float[numChannels];
        y1 = new float[numChannels];
        y2 = new float[numChannels];
        initialized = true;
    }

    private void updateCutoffFrequency() {
        float maxChange = getAudioFormat().getSampleRate() * 0.01f; // Макс. изменение 1% от SR
        float targetFc = lowPassFreq.getValue();
        float delta = targetFc - currentFc;
        
        // Ограничение скорости изменения
        if (Math.abs(delta) > maxChange) {
            targetFc = currentFc + Math.signum(delta) * maxChange;
        }
        
        float releaseVal = release.getValue();
        currentFc += (targetFc - currentFc) * releaseVal;
        
        float nyquist = getAudioFormat().getSampleRate() / 2.0f;
        currentFc = Math.min(Math.max(currentFc, 10.0f), nyquist * 0.9999f);
    }

    private FilterCoefficients calculateFilterCoefficients() {
        float fs = getAudioFormat().getSampleRate();
        float omega0 = (float) (2 * Math.PI * currentFc / fs);
        float Q = 1.0f / (float) Math.sqrt(2);
        float alpha = (float) (Math.sin(omega0) / (2 * Q));

        float b0 = (1 - (float) Math.cos(omega0)) / 2;
        float b1 = 1 - (float) Math.cos(omega0);
        float b2 = b0;
        float a0 = 1 + alpha;
        float a1 = -2 * (float) Math.cos(omega0);
        float a2 = 1 - alpha;

        // Нормировка коэффициентов
        return new FilterCoefficients(
            b0 / a0,
            b1 / a0,
            b2 / a0,
            a1 / a0,
            a2 / a0
        );
    }

    private void processChannel(float[] channel, int channelIdx, FilterCoefficients coeffs) {
        float cx1 = x1[channelIdx];
        float cx2 = x2[channelIdx];
        float cy1 = y1[channelIdx];
        float cy2 = y2[channelIdx];

        for (int i = 0; i < channel.length; i++) {
            float x = channel[i];
            float y = coeffs.b0 * x 
                     + coeffs.b1 * cx1 
                     + coeffs.b2 * cx2 
                     - coeffs.a1 * cy1 
                     - coeffs.a2 * cy2;

            // Обновление состояний
            cx2 = cx1;
            cx1 = x;
            cy2 = cy1;
            cy1 = y;

            channel[i] = y;
        }

        // Сохранение конечных состояний
        x1[channelIdx] = cx1;
        x2[channelIdx] = cx2;
        y1[channelIdx] = cy1;
        y2[channelIdx] = cy2;
    }

    private static class FilterCoefficients {
        final float b0, b1, b2, a1, a2;

        FilterCoefficients(float b0, float b1, float b2, float a1, float a2) {
            this.b0 = b0;
            this.b1 = b1;
            this.b2 = b2;
            this.a1 = a1;
            this.a2 = a2;
        }
    }

    public FloatControl getCutoffControl() {
        return lowPassFreq;
    }

    public FloatControl getReleaseControl() {
        return release;
    }
}