package org.theko.sound.effects;

import org.theko.sound.control.FloatControl;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.resampling.LanczosResampleMethod;
import org.theko.sound.resampling.ResampleMethod;
import org.theko.sound.utility.ArrayUtilities;

/**
 * ResamplerEffect is an audio effect that allows for real-time resampling of audio samples.
 * It uses a specified resampling method to adjust the speed of the audio playback.
 * 
 * This effect can be applied to audio samples to change their playback speed without altering
 * the pitch, making it useful for various audio processing tasks.
 * 
 * @since v2.0.0
 * @author Theko
 */
public class ResamplerEffect extends AudioEffect implements VaryingSizeEffect{

    private final FloatControl speedControl = new FloatControl("Speed", 0.001f, 50.0f, 1.0f);
    private AudioResampler resampler;

    public ResamplerEffect(ResampleMethod method) {
        super(Type.REALTIME);
        resampler = new AudioResampler(method, AudioSystemProperties.RESAMPLER_SHARED_QUALITY);
    }

    public ResamplerEffect() {
        this(new LanczosResampleMethod());
    }

    public FloatControl getSpeedControl() {
        return speedControl;
    }

    @Override
    public void render(float[][] samples, int sampleRate, int length) {
        if (samples == null || samples.length == 0 || samples[0].length == 0) {
            return; // Nothing to resample
        }

        float[][] toResample = ArrayUtilities.cutArray(samples, 0, samples.length, 0, length);
        float[][] resampled = resampler.resample(toResample, speedControl.getValue());

        int channels = Math.min(samples.length, resampled.length);
        for (int ch = 0; ch < channels; ch++) {
            int minCopy = Math.min(samples[ch].length, resampled[ch].length);
            for (int i = 0; i < minCopy; i++) {
                samples[ch][i] = resampled[ch][i];
            }
            // Zero out the rest if output is shorter than buffer
            for (int i = minCopy; i < samples[ch].length; i++) {
                samples[ch][i] = 0.0f;
            }
        }
    }

    @Override
    public int getTargetLength(int length) {
        return (int) Math.ceil(length * speedControl.getValue());
    }
}
