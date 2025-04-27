package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatController;

public class DelayEffect extends AudioEffect {
    private final int sampleRate;
    private final int numChannels;
    private CircularBuffer[] delayBuffers;

    // Контроллеры параметров
    private final FloatController delayTimeController;
    private final FloatController feedbackController;
    private final FloatController mixController;
    private final FloatController cutoffFrequencyController; // Для частоты среза

    private int delayInSamples;

    private float[] previousFilteredSamples;

    public DelayEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.sampleRate = (int) audioFormat.getSampleRate();
        this.numChannels = audioFormat.getChannels();

        // Инициализация контроллеров
        delayTimeController = new FloatController("Delay Time", 0.01f, 2.0f, 1f);
        feedbackController = new FloatController("Feedback", 0.0f, 0.99f, 0.5f);
        mixController = new FloatController("Mix", 0.0f, 1.0f, 0.5f);
        cutoffFrequencyController = new FloatController("Cutoff Frequency", 20.0f, 1000.0f, 200.0f); // контроллер для частоты среза

        previousFilteredSamples = new float[numChannels];
        updateDelayBuffers();
    }

    private void updateDelayBuffers() {
        this.delayInSamples = (int) (delayTimeController.getValue() * sampleRate);
        delayBuffers = new CircularBuffer[numChannels];
        for (int i = 0; i < numChannels; i++) {
            delayBuffers[i] = new CircularBuffer(delayInSamples);
        }
    }

    @Override
    public float[][] process(float[][] samples) {
        // Проверим, не изменилось ли время задержки
        int newDelayInSamples = (int) (delayTimeController.getValue() * sampleRate);
        if (newDelayInSamples != delayInSamples) {
            updateDelayBuffers();
        }

        float feedback = feedbackController.getValue();
        float mix = mixController.getValue();
        float cutoffFrequency = cutoffFrequencyController.getValue();

        // Коэффициент фильтра LPF
        float a = getLpfCoefficient(cutoffFrequency);

        for (int ch = 0; ch < numChannels; ch++) {
            CircularBuffer buffer = delayBuffers[ch];
            float[] channelSamples = samples[ch];
            float previousSample = previousFilteredSamples[ch];

            for (int i = 0; i < channelSamples.length; i++) {
                // Применяем фильтр низких частот
                float inputSample = channelSamples[i];
                float filteredSample = a * inputSample + (1 - a) * previousSample;

                // Сохраняем последний отфильтрованный сэмпл
                previousFilteredSamples[ch] = filteredSample;

                // Применяем задержку и обработку
                float delayedSample = buffer.getDelayed(delayInSamples);
                float outputSample = filteredSample * (1.0f - mix) + delayedSample * mix;
                channelSamples[i] = outputSample;

                // Пишем в буфер
                buffer.put(inputSample + delayedSample * feedback);
            }
        }

        return samples;
    }

    // Геттеры контроллеров для внешнего доступа
    public FloatController getDelayTimeController() {
        return delayTimeController;
    }

    public FloatController getFeedbackController() {
        return feedbackController;
    }

    public FloatController getMixController() {
        return mixController;
    }

    public FloatController getCutoffFrequencyController() {
        return cutoffFrequencyController;
    }

    private static class CircularBuffer {
        private final float[] buffer;
        private int pointer;

        public CircularBuffer(int size) {
            this.buffer = new float[size];
            this.pointer = 0;
        }

        public void put(float sample) {
            buffer[pointer] = sample;
            pointer = (pointer + 1) % buffer.length;
        }

        public float getDelayed(int delay) {
            int index = (pointer - delay + buffer.length) % buffer.length;
            return buffer[index];
        }
    }

    // Метод для вычисления коэффициента LPF
    private float getLpfCoefficient(float cutoffFrequency) {
        // Частота среза в Гц и частота дискретизации
        float tau = 1.0f / (2 * (float) Math.PI * cutoffFrequency);
        float a = tau / (tau + 1.0f / sampleRate);
        return a;
    }
}
