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

import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.util.MathUtilities;

/**
 * The AudioResampler class provides utility methods for resampling audio data.
 * It supports resampling audio at different speeds using a speed multiplier.
 * The class includes methods for converting audio data between byte arrays
 * and float samples, applying time scaling.
 *
 * @since 0.1.1-beta
 * @author Theko
 */
public class ResamplingProcessor {

    /**
     * A shared instance of AudioResampler with default settings.
     * This instance can be used for quick access without needing to create a new one.
     */
    public static final Resampler SHARED_RESAMPLER = AudioSystemProperties.SHARED_RESAMPLER;

    /**
     * The resample method used for audio resampling.
     * It can be set to different algorithms like Lanczos, Linear, or Cubic.
     */
    protected final Resampler resampleMethod;

    /**
     * Constructs an AudioResampler with the specified resample method.
     *
     * @param resamplerMethod The resample method to use for audio resampling
     */
    public ResamplingProcessor(Resampler resamplerMethod) {
        this.resampleMethod = resamplerMethod;
    }

    /**
     * Constructs an AudioResampler with the default shared method and quality.
     * The default method is set to the shared resample method defined in AudioSystemProperties.
     */
    public ResamplingProcessor() {
        this(SHARED_RESAMPLER);
    }

    /**
     * Returns the class of the resample method used by this AudioResampler.
     *
     * @return The class of the resample method
     */
    public Class<?> getResampleMethodClass() {
        return resampleMethod.getClass();
    }

    /**
     * Resamples the given audio samples to a new length.
     *
     * @param samples The audio samples to resample, represented as a 2D float array
     * @param speedMultiplier The speed multiplier for the resampling process
     * @return A 2D float array containing the resampled audio samples
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels
     */
    public float[][] resample(float[][] samples, float speedMultiplier) {
        SamplesValidation.validateSamples(samples);
        return resample(samples, (int) (samples[0].length / speedMultiplier));
    }

    /**
     * Resamples the given audio samples to a new length.
     *
     * @param samples The audio samples to resample, represented as a 2D float array
     * @param newLength The target length of the resampled audio samples
     * @return A 2D float array containing the resampled audio samples
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels
     */
    public float[][] resample(float[][] samples, int newLength) {
        SamplesValidation.validateSamples(samples);
        newLength = MathUtilities.clamp(newLength, 1, samples[0].length * 50);

        float[][] output = new float[samples.length][newLength];
        resample(samples, output, newLength);
        return output;
    }

    /**
     * Resamples the given audio samples to a new length.
     *
     * @param samples The audio samples to resample, represented as a 2D float array
     * @param output The output array to store the resampled audio samples, with new length
     * @param speedMultiplier The speed multiplier for the resampling process
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels
     */
    public void resample(float[][] samples, float[][] output, float speedMultiplier) {
        SamplesValidation.validateSamples(samples);
        resample(samples, output, (int) (samples[0].length / speedMultiplier));
    }

    /**
     * Resamples the given audio samples to a new length.
     *
     * @param samples The audio samples to resample, represented as a 2D float array
     * @param output The output array to store the resampled audio samples, with new length
     * @param newLength The target length of the resampled audio samples
     * @throws IllegalArgumentException if the new length is less than or equal to zero, or the input and output arrays do not have the same number of channels
     */
    public void resample(float[][] samples, float[][] output, int newLength) {
        newLength = Math.max(1, newLength);
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        if (samples.length != output.length) {
            throw new IllegalArgumentException("Input and output arrays must have the same number of channels.");
        }
        
        resampleMethod.resample(samples, output, newLength);
    }
}