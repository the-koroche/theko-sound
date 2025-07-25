package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.envelope.ASREnvelope;

/**
 * The AudioLimiter class is a real-time audio effect that applies dynamic range compression
 * to audio signals. It limits the amplitude of the audio signal to prevent clipping and 
 * distortion, while also providing soft saturation for smoother transitions.
 * 
 * <p>This class uses an attack-sustain-release (ASR) envelope to control the gain reduction
 * dynamically, ensuring a natural-sounding compression effect. The limiter has configurable
 * parameters for gain, soft saturation threshold, limiter ceiling, and envelope timing.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Gain control: Adjusts the overall input gain in decibels (dB).</li>
 *   <li>Soft saturation threshold: Defines the level at which soft saturation begins.</li>
 *   <li>Limiter ceiling: Sets the maximum output level to prevent clipping.</li>
 *   <li>ASR envelope: Configurable attack, sustain, and release times for dynamic control.</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
 * AudioLimiter limiter = new AudioLimiter(format);
 * limiter.getGain().setValue(6.0f); // Set gain to +6 dB
 * limiter.getLimiterCeiling().setValue(-1.0f); // Set limiter ceiling to -1 dB
 * float[][] processedSamples = limiter.process(inputSamples);
 * </pre>
 * 
 * <p>Note: The limiter processes audio in real-time and is designed for multi-channel audio.
 * It ensures that all channels are processed consistently to maintain stereo or surround sound integrity.
 * 
 * @see AudioEffect
 * @see FloatControl
 * @see ASREnvelope
 * 
 * @since v1.4.1
 * @author Theko
 */
public class AudioLimiter extends AudioEffect {
    protected final FloatControl gain = 
            new FloatControl("Gain", -24.0f, 24.0f, 0.0f); // dB
    protected final FloatControl softSaturationThreshold = 
            new FloatControl("Soft Saturation Threshold", -12.0f, 0.0f, -6.0f); // dB
    protected final FloatControl limiterCeiling = 
            new FloatControl("Limiter Ceiling", -20.0f, 0.0f, -0.1f); // dB
    protected final ASREnvelope envelope = 
            new ASREnvelope(0.005f, 0.2f, 0.05f); // 5 ms attack, 200 ms release, 50 ms sustain

    protected final List<AudioControl> limiterControls = List.of(
        gain,
        softSaturationThreshold,
        limiterCeiling,
        envelope.getAttack(),
        envelope.getRelease(),
        envelope.getSustain()
    );

    public AudioLimiter () {
        super(Type.REALTIME);

        addControls(limiterControls);
    }

    public FloatControl getGain () {
        return gain;
    }

    public FloatControl getSoftSaturationThreshold () {
        return softSaturationThreshold;
    }

    public FloatControl getLimiterCeiling () {
        return limiterCeiling;
    }

    public ASREnvelope getEnvelope () {
        return envelope;
    }

    public FloatControl getAttack() {
        return envelope.getAttack();
    }

    public FloatControl getRelease () {
        return envelope.getRelease();
    }

    public FloatControl getSustain () {
        return envelope.getSustain();
    }

    @Override
    protected void effectRender (float[][] samples, int sampleRate) {
        int channels = samples.length;
        int length = samples[0].length;
    
        float linearGain = (float) Math.pow(10.0, gain.getValue() / 20.0);
        float softThreshold = (float) Math.pow(10.0, softSaturationThreshold.getValue() / 20.0);
        float ceiling = (float) Math.pow(10.0, limiterCeiling.getValue() / 20.0);
    
        float attackCoeff = (float) Math.exp(-1.0 / (sampleRate * getAttack().getValue()));
        float releaseCoeff = (float) Math.exp(-1.0 / (sampleRate * getRelease().getValue()));
        float sustainSamples = getSustain().getValue() * sampleRate;
    
        float envelopeValue = 1.0f;
        float sustainCounter = 0.0f;
    
        for (int i = 0; i < length; i++) {
            // Calculate the maximum absolute value across all channels
            float maxAbs = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                float value = samples[ch][i] * linearGain;
                maxAbs = Math.max(maxAbs, Math.abs(value));
            }
    
            float gainReduction = 1.0f;
    
            if (maxAbs > softThreshold) {
                if (maxAbs > ceiling) {
                    // Hard limiter
                    gainReduction = ceiling / maxAbs;
                    sustainCounter = sustainSamples;
                } else {
                    // Soft saturation
                    float excess = (maxAbs - softThreshold) / (ceiling - softThreshold);
                    gainReduction = (float) (1.0 / (1.0 + excess * excess));
                }
            }
    
            if (gainReduction < envelopeValue) {
                envelopeValue = attackCoeff * (envelopeValue - gainReduction) + gainReduction;
                sustainCounter = sustainSamples; // Reset sustain counter on attack
            } else {
                if (sustainCounter > 0.0f) {
                    sustainCounter--;
                } else {
                    envelopeValue = releaseCoeff * (envelopeValue - 1.0f) + 1.0f;
                }
            }
    
            // Apply gain reduction to all channels
            for (int ch = 0; ch < channels; ch++) {
                samples[ch][i] *= linearGain * envelopeValue;
            }
        }
    }
}
