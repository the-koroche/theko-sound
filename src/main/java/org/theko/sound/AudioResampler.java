package org.theko.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.resampling.LanczosResampler;
import org.theko.sound.resampling.ResamplerMethod;

/**
 * The AudioResampler class provides utility methods for resampling audio data.
 * It supports resampling audio at different speeds using a speed multiplier.
 * The class includes methods for converting audio data between byte arrays
 * and float samples, applying time scaling, and performing Lanczos resampling.
 * 
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Resampling audio data using a speed multiplier.</li>
 *   <li>Time scaling of audio samples for speed adjustment.</li>
 *   <li>High-quality Lanczos resampling algorithm for interpolation.</li>
 * </ul>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * {@code
 * byte[] resampledData = AudioResampler.resample(originalData, sourceFormat, speedMultiplier);
 * }
 * </pre>
 * 
 * <p><strong>Note:</strong> The class is not instantiable as it has a private constructor.</p>
 * 
 * <p><strong>Exceptions:</strong></p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - Thrown if the speed multiplier is less than or equal to 0.</li>
 * </ul>
 * 
 * @since v1.1.0
 * 
 * @author Theko
 */
public class AudioResampler {
    private static final Logger logger = LoggerFactory.getLogger(AudioResampler.class);

    public static final AudioResampler SHARED = new AudioResampler();
    private static final int DEFAULT_QUALITY = 3;

    protected int quality;
    protected final ResamplerMethod resamplerMethod;

    public AudioResampler(ResamplerMethod resamplerMethod, int quality) {
        this.resamplerMethod = resamplerMethod;
        this.quality = quality;
    }

    public AudioResampler() {
        this(new LanczosResampler(), DEFAULT_QUALITY);
    }

    public void setQuality(int quality) {
        if (quality < 1) {
            logger.error("Quality argument is less than 1.");
            throw new IllegalArgumentException("Quality must be greater than or equal to 1.");
        } else if (quality > 8) {
            logger.warn("High quality can cause performance issues.");
        }
        this.quality = quality;
    }

    /**
     * Resamples audio data using a speed multiplier.
     * This method modifies the speed of the audio by the given multiplier.
     *
     * @param data The audio data to be resampled (raw byte data).
     * @param sourceFormat The audio format of the source data.
     * @param speedMultiplier The factor by which the audio speed will be modified.
     *                        A value greater than 1 speeds up the audio, and a value 
     *                        less than 1 slows it down.
     * @return A byte array containing the resampled audio data.
     * @throws IllegalArgumentException if the speed multiplier is less than or equal to 0.
     */
    public byte[] resample(byte[] data, AudioFormat sourceFormat, float speedMultiplier) {
        // Validate speed multiplier
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        // Convert byte data to float samples (for manipulation)
        float[][] samples = SampleConverter.toSamples(data, sourceFormat);

        // Resample the samples with the specified speed multiplier
        resample(samples, speedMultiplier);

        // Convert the resampled float samples back to byte data
        return SampleConverter.fromSamples(samples, sourceFormat);
    }

    /**
     * Resamples audio samples using a speed multiplier.
     *
     * @param samples The audio samples (as an array of float arrays, one for each channel).
     * @param speedMultiplier The factor by which the audio speed will be modified.
     * @return A 2D array of resampled audio samples.
     * @throws IllegalArgumentException if the speed multiplier is less than or equal to 0.
     */
    public float[][] resample(float[][] samples, float speedMultiplier) {
        // Validate speed multiplier
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be greater than zero.");
        }

        // Resample each channel
        for (int ch = 0; ch < samples.length; ch++) {
            samples[ch] = timeScale(samples[ch], speedMultiplier);
        }

        return samples;
    }

    /**
     * Applies time scaling to a single audio channel (array of samples).
     * This method modifies the length of the input based on the speed multiplier.
     *
     * @param input The original audio samples of one channel.
     * @param speedMultiplier The factor by which to change the length of the samples.
     * @return A new array of resampled audio samples for the given channel.
     */
    private float[] timeScale(float[] input, float speedMultiplier) {
        // Calculate the new length after applying the speed multiplier
        int newLength = (int) (input.length / speedMultiplier);

        // Perform Lanczos resampling to obtain the new samples
        return resamplerMethod.resample(input, newLength, quality);
    }
}