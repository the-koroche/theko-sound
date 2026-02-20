/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.resamplers;

import static org.theko.sound.properties.AudioSystemProperties.SHARED_RESAMPLER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.util.MathUtilities;

/**
 * The AudioResampler class provides utility methods for resampling audio data.
 * It supports resampling audio at different speeds using a speed multiplier.
 * The class includes methods for converting audio data between byte arrays
 * and float samples, applying time scaling.
 * 
 * @since 1.1.0
 * @author Theko
 */
public class AudioResampler {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioResampler.class);

    /**
     * A shared instance of AudioResampler with default settings.
     * This instance can be used for quick access without needing to create a new one.
     */
    public static final AudioResampler SHARED = new AudioResampler();

    /**
     * The resample method used for audio resampling.
     * It can be set to different algorithms like Lanczos, Linear, or Cubic.
     */
    protected final ResampleMethod resampleMethod;

    /**
     * Constructs an AudioResampler with the specified resample method.
     * 
     * @param resamplerMethod The resample method to use for audio resampling.
     */
    public AudioResampler(ResampleMethod resamplerMethod) {
        this.resampleMethod = resamplerMethod;
    }

    /**
     * Constructs an AudioResampler with the default shared method and quality.
     * The default method is set to the shared resample method defined in AudioSystemProperties.
     */
    public AudioResampler() {
        this(SHARED_RESAMPLER);
    }

    /**
     * Resamples the given audio samples to a new length.
     * 
     * @param samples The audio samples to resample, represented as a 2D float array.
     * @param speedMultiplier The speed multiplier for the resampling process.
     * @return A 2D float array containing the resampled audio samples.
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels.
     */
    public float[][] resample(float[][] samples, float speedMultiplier) {
        return resample(samples, (int) (samples[0].length / speedMultiplier));
    }

    /**
     * Resamples the given audio samples to a new length.
     * 
     * @param samples The audio samples to resample, represented as a 2D float array.
     * @param newLength The target length of the resampled audio samples.
     * @return A 2D float array containing the resampled audio samples.
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels.
     */
    public float[][] resample(float[][] samples, int newLength) {
        newLength = MathUtilities.clamp(newLength, 1, samples[0].length * 50);
        float[][] output = new float[samples.length][newLength];
        resample(samples, output, newLength);
        return output;
    }

    /**
     * Resamples the given audio samples to a new length.
     * 
     * @param samples The audio samples to resample, represented as a 2D float array.
     * @param output The output array to store the resampled audio samples, with new length.
     * @param speedMultiplier The speed multiplier for the resampling process.
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels.
     */
    public void resample(float[][] samples, float[][] output, float speedMultiplier) {
        resample(samples, output, (int) (samples[0].length / speedMultiplier));
    }
    
    /**
     * Resamples the given audio samples to a new length.
     * 
     * @param samples The audio samples to resample, represented as a 2D float array.
     * @param output The output array to store the resampled audio samples, with new length.
     * @param newLength The target length of the resampled audio samples.
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels.
     */
    public void resample(float[][] samples, float[][] output, int newLength) {
        newLength = Math.max(1, newLength);
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        if (samples.length != output.length) {
            throw new IllegalArgumentException("Input and output arrays must have the same number of channels.");
        }
        for (int ch = 0; ch < samples.length; ch++) {
            if (!timeScale(samples[ch], output[ch], newLength)) {
                if (samples[ch].length != newLength) {
                    throw new RuntimeException("Error resampling audio.");
                }
                System.arraycopy(samples[ch], 0, output[ch], 0, samples[ch].length);
            }
        }
    }

    private boolean timeScale(float[] input, float[] output, int newLength) {
        if (input.length == newLength) 
            return false;

        if (newLength <= 0)
            throw new IllegalArgumentException("New length must be greater than zero.");

        if (resampleMethod == null) {
            logger.error("Resample method is null.");
            throw new IllegalArgumentException("Resample method is null.");
        }

        resampleMethod.resample(input, output, newLength);
        return true;
    }
}