package org.theko.sound.effects;

import java.util.ArrayList;
import java.util.List;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.envelope.ASREnvelope;

public class AudioLimiter extends AudioEffect {
    private FloatControl gain;
    private FloatControl softSaturationThreshold;
    private FloatControl limiterCeiling;
    private ASREnvelope envelope;

    public AudioLimiter(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
    
        this.gain = new FloatControl("Gain", -24.0f, 24.0f, 0.0f); // dB
        this.softSaturationThreshold = new FloatControl("Soft Saturation Threshold", -12.0f, 0.0f, -6.0f); // dB
        this.limiterCeiling = new FloatControl("Limiter Ceiling", -20.0f, 0.0f, -0.1f); // dB
        this.envelope = new ASREnvelope(0.005f, 0.2f, 0.05f); // 5 ms attack, 200 ms release, 50 ms sustain
    }

    public FloatControl getGain() {
        return gain;
    }

    public FloatControl getSoftSaturationThreshold() {
        return softSaturationThreshold;
    }

    public FloatControl getLimiterCeiling() {
        return limiterCeiling;
    }

    public FloatControl getAttack() {
        return envelope.getAttack();
    }

    public FloatControl getRelease() {
        return envelope.getRelease();
    }

    public FloatControl getSustain() {
        return envelope.getSustain();
    }

    @Override
    protected float[][] process(float[][] samples) {
        int channels = samples.length;
        int frames = samples[0].length;
        float sampleRate = audioFormat.getSampleRate();
    
        float linearGain = (float) Math.pow(10.0, gain.getValue() / 20.0);
        float softThreshold = (float) Math.pow(10.0, softSaturationThreshold.getValue() / 20.0);
        float ceiling = (float) Math.pow(10.0, limiterCeiling.getValue() / 20.0);
    
        float attackCoeff = (float) Math.exp(-1.0 / (sampleRate * getAttack().getValue()));
        float releaseCoeff = (float) Math.exp(-1.0 / (sampleRate * getRelease().getValue()));
        float sustainSamples = getSustain().getValue() * sampleRate;
    
        float envelope = 1.0f;
        float sustainCounter = 0.0f;
    
        for (int i = 0; i < frames; i++) {
            // Вычисляем "максимальный" уровень среди всех каналов
            float maxAbs = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                float value = samples[ch][i] * linearGain;
                maxAbs = Math.max(maxAbs, Math.abs(value));
            }
    
            float gainReduction = 1.0f;
    
            if (maxAbs > softThreshold) {
                if (maxAbs > ceiling) {
                    // Жесткая лимитация
                    gainReduction = ceiling / maxAbs;
                    sustainCounter = sustainSamples; // задержка отпускания
                } else {
                    // Мягкое насыщение
                    float excess = (maxAbs - softThreshold) / (ceiling - softThreshold);
                    gainReduction = (float) (1.0 / (1.0 + excess * excess)); // типа мягкая компрессия
                }
            }
    
            if (gainReduction < envelope) {
                envelope = attackCoeff * (envelope - gainReduction) + gainReduction;
                sustainCounter = sustainSamples; // если снова пережимаем — сбрасываем sustain
            } else {
                if (sustainCounter > 0.0f) {
                    sustainCounter--;
                } else {
                    envelope = releaseCoeff * (envelope - 1.0f) + 1.0f;
                }
            }
    
            // Применяем итоговое усиление + лимитера
            for (int ch = 0; ch < channels; ch++) {
                samples[ch][i] *= linearGain * envelope;
            }
        }
    
        return samples;
    }

    @Override
    public List<AudioControl> getAllControls() {
        List<AudioControl> controls = new ArrayList<>();
        controls.add(gain);
        controls.add(softSaturationThreshold);
        controls.add(limiterCeiling);
        controls.addAll(envelope.getAllControls());
        return controls;
    }
}
