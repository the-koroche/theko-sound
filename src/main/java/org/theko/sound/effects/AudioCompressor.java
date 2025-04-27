package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatController;

public class AudioCompressor extends AudioEffect {

    private FloatController threshold;    // dB
    private FloatController ratio;        // unitless
    private FloatController attackTime;   // seconds
    private FloatController releaseTime;  // seconds
    private FloatController makeupGain;   // dB

    private float envelope = 0.0f;

    public AudioCompressor(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.threshold = new FloatController("Threshold", -60.0f, 0.0f, -24.0f);
        this.ratio = new FloatController("Ratio", 1.0f, 20.0f, 3.0f);
        this.attackTime = new FloatController("Attack", 0.0001f, 0.2f, 0.01f);
        this.releaseTime = new FloatController("Release", 0.001f, 2.0f, 0.2f);
        this.makeupGain = new FloatController("Makeup Gain", -24.0f, 24.0f, 2.0f);
    }

    public FloatController getThreshold() {
        return threshold;
    }

    public FloatController getRatio() {
        return ratio;
    }

    public FloatController getAttack() {
        return attackTime;
    }

    public FloatController getRelease() {
        return releaseTime;
    }

    public FloatController getMakeupGain() {
        return makeupGain;
    }

    @Override
    public float[][] process(float[][] data) {
        int channels = data.length;
        int samples = data[0].length;
        float sampleRate = audioFormat.getSampleRate();

        float attackCoeff = (float) Math.exp(-1.0 / (sampleRate * attackTime.getValue()));
        float releaseCoeff = (float) Math.exp(-1.0 / (sampleRate * releaseTime.getValue()));
        float makeup = (float) Math.pow(10.0, makeupGain.getValue() / 20.0);

        for (int i = 0; i < samples; i++) {
            // Считаем общий уровень сигнала (rms-like, но просто средний абсолютный)
            float level = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                level += Math.abs(data[ch][i]);
            }
            level /= channels;

            // Переводим уровень в dB
            float dbLevel = 20.0f * (float) Math.log10(level + 1e-8f);

            // Считаем сколько нужно сжать
            float gainReductionDb = 0.0f;
            if (dbLevel > threshold.getValue()) {
                gainReductionDb = (threshold.getValue() + (dbLevel - threshold.getValue()) / ratio.getValue()) - dbLevel;
            }

            // Делаем плавное изменение
            float targetGain = (float) Math.pow(10.0, gainReductionDb / 20.0);
            if (targetGain < envelope) {
                envelope = attackCoeff * (envelope - targetGain) + targetGain;
            } else {
                envelope = releaseCoeff * (envelope - targetGain) + targetGain;
            }

            // Применяем gain и makeup
            float finalGain = envelope * makeup;

            for (int ch = 0; ch < channels; ch++) {
                data[ch][i] *= finalGain;
            }
        }

        return data;
    }
}
