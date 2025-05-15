package org.theko.sound.effects;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioResampler;
import org.theko.sound.control.FloatControl;

/**
 * The {@code ResamplerEffect} class is an audio effect that resamples audio data
 * in real-time based on a specified speed factor. It extends the {@code AudioEffect}
 * class and is annotated with {@code @NonFixedSizeEffect}, indicating that the
 * effect does not have a fixed input/output size.
 *
 * <p>This class uses an {@link AudioResampler} to perform the resampling operation
 * and provides a {@link FloatControl} to adjust the speed of the resampling.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Allows real-time resampling of audio data.</li>
 *   <li>Provides a {@link FloatControl} to dynamically adjust the speed factor
 *       within a range of 0.001 to 32.0.</li>
 *   <li>Supports custom resampler implementations via the {@code setResampler} method.</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>
 * {@code
 * AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
 * ResamplerEffect effect = new ResamplerEffect(format);
 * effect.getSpeedControl().setValue(2.0f); // Double the playback speed
 * float[][] processedData = effect.process(inputData);
 * }
 * </pre>
 *
 * @see AudioEffect
 * @see AudioResampler
 * @see FloatControl
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
@NonFixedSizeEffect
public class ResamplerEffect extends AudioEffect {
    private final FloatControl speed;
    private AudioResampler resampler;

    public ResamplerEffect(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        speed = new FloatControl("Speed", 0.001f,32f, 1f);
        setResampler(new AudioResampler());
    }

    public void setResampler(AudioResampler resampler) {
        if (resampler == null) {
            throw new IllegalArgumentException("New resampler cannot be null.");
        }
        this.resampler = resampler;
    }

    @Override
    public float[][] process(float[][] data) {
        return resampler.resample(data, speed.getValue());
    }

    public FloatControl getSpeedControl() {
        return speed;
    }
}
