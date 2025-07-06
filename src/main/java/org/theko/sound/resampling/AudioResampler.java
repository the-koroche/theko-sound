package org.theko.sound.resampling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.utility.ArrayUtilities;

/**
 * The AudioResampler class provides utility methods for resampling audio data.
 * It supports resampling audio at different speeds using a speed multiplier.
 * The class includes methods for converting audio data between byte arrays
 * and float samples, applying time scaling.
 * 
 * @since v1.1.0
 * 
 * @author Theko
 */
public class AudioResampler {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioResampler.class);

    public static final AudioResampler SHARED = new AudioResampler();

    protected int quality;
    protected final ResampleMethod resampleMethod;

    public AudioResampler (ResampleMethod resamplerMethod, int quality) {
        this.resampleMethod = resamplerMethod;
        this.quality = quality;
    }

    public AudioResampler () {
        this(AudioSystemProperties.RESAMPLER_SHARED_METHOD, AudioSystemProperties.RESAMPLER_SHARED_QUALITY);
    }

    public void setQuality (int quality) {
        if (quality < 1) {
            logger.error("Quality argument is less than 1.");
            throw new IllegalArgumentException("Quality must be greater than or equal to 1.");
        } else if (quality > 8 && AudioSystemProperties.RESAMPLER_LOG_HIGH_QUALITY) {
            logger.warn("High quality can cause performance issues.");
        }
        this.quality = quality;
    }

    public float[][] resample (float[][] samples, float speedMultiplier) {
        if (speedMultiplier == 0) {
            throw new IllegalArgumentException("Speed multiplier cannot be zero.");
        }
        int newLength = (int) (samples[0].length / speedMultiplier);
        float[][] output = new float[samples.length][newLength];
        for (int ch = 0; ch < samples.length; ch++) {
            output[ch] = timeScale(samples[ch], newLength);
        }
        return output;
    }
    
    public float[][] resample (float[][] samples, int newLength) {
        float[][] output = new float[samples.length][newLength];
        for (int ch = 0; ch < samples.length; ch++) {
            output[ch] = timeScale(samples[ch], newLength);
        }
        return output;
    }

    private float[] timeScale (float[] input, int newLength) {
        if (input.length == newLength) 
            return input;

        if (newLength <= 0)
            throw new IllegalArgumentException("New length must be greater than zero.");

        if (resampleMethod == null) {
            logger.error("Resample method is null.");
            return input;
        }

        float[] output = resampleMethod.resample(input, newLength, quality);
        return ArrayUtilities.cutArray(output, 0, newLength);
    }
}