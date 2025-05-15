package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.control.FloatControl;

/**
 * The AudioCompressor class is a real-time audio effect that applies dynamic range compression
 * to audio signals. It reduces the volume of loud sounds or amplifies quiet sounds by narrowing
 * the dynamic range of the audio signal.
 * 
 * <p>This class provides controls for various compression parameters, including threshold, ratio,
 * attack time, release time, and makeup gain. The compression is applied in real-time to the
 * input audio data.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Threshold: The level (in dB) above which compression is applied.</li>
 *   <li>Ratio: The amount of compression applied to signals above the threshold.</li>
 *   <li>Attack Time: The time (in seconds) it takes for the compressor to respond to signals
 *       above the threshold.</li>
 *   <li>Release Time: The time (in seconds) it takes for the compressor to stop compressing
 *       after the signal falls below the threshold.</li>
 *   <li>Makeup Gain: The amount of gain (in dB) applied to the compressed signal to compensate
 *       for the reduction in volume.</li>
 * </ul>
 * 
 * <p>The compression algorithm uses an envelope follower to smoothly adjust the gain reduction
 * over time, ensuring natural-sounding results. The gain reduction is calculated based on the
 * input signal's level and the specified compression parameters.
 * 
 * <p>Usage:
 * <pre>
 * AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
 * AudioCompressor compressor = new AudioCompressor(format);
 * compressor.getThreshold().setValue(-20.0f);
 * compressor.getRatio().setValue(4.0f);
 * compressor.getAttack().setValue(0.01f);
 * compressor.getRelease().setValue(0.2f);
 * compressor.getMakeupGain().setValue(3.0f);
 * 
 * float[][] processedData = compressor.process(inputData);
 * </pre>
 * 
 * @see AudioEffect
 * @see FloatControl
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioCompressor extends AudioEffect {
    private final FloatControl threshold;    // dB
    private final FloatControl ratio;        // unitless
    private final FloatControl attackTime;   // seconds
    private final FloatControl releaseTime;  // seconds
    private final FloatControl makeupGain;   // dB

    private float envelope = 0.0f;

    public AudioCompressor(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.threshold = new FloatControl("Threshold", -60.0f, 0.0f, -24.0f);
        this.ratio = new FloatControl("Ratio", 1.0f, 20.0f, 3.0f);
        this.attackTime = new FloatControl("Attack", 0.0001f, 0.2f, 0.01f);
        this.releaseTime = new FloatControl("Release", 0.001f, 2.0f, 0.2f);
        this.makeupGain = new FloatControl("Makeup Gain", -24.0f, 24.0f, 2.0f);
    }

    public FloatControl getThreshold() {
        return threshold;
    }

    public FloatControl getRatio() {
        return ratio;
    }

    public FloatControl getAttack() {
        return attackTime;
    }

    public FloatControl getRelease() {
        return releaseTime;
    }

    public FloatControl getMakeupGain() {
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
