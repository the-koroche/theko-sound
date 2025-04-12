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

    private int delayInSamples;

    public DelayEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.sampleRate = (int) audioFormat.getSampleRate();
        this.numChannels = audioFormat.getChannels();

        // Инициализация контроллеров
        delayTimeController = new FloatController("Delay Time", 0.01f, 2.0f, 0.3f);
        feedbackController = new FloatController("Feedback", 0.0f, 0.99f, 0.5f);
        mixController = new FloatController("Mix", 0.0f, 1.0f, 0.5f);

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

        for (int ch = 0; ch < numChannels; ch++) {
            CircularBuffer buffer = delayBuffers[ch];
            float[] channelSamples = samples[ch];

            for (int i = 0; i < channelSamples.length; i++) {
                float delayedSample = buffer.getDelayed(delayInSamples);
                float inputSample = channelSamples[i];

                float outputSample = inputSample * (1.0f - mix) + delayedSample * mix;
                channelSamples[i] = outputSample;

                // Пишем в буфер сумму входного сигнала и ослабленного эха
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
}
