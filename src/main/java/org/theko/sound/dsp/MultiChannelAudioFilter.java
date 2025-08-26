/*
 * Copyright 2025 Alex Soloviov (aka Theko)
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

package org.theko.sound.dsp;

/**
 * A class that represents a multi-channel audio filter.
 * It contains an array of AudioFilter objects that process audio samples for each channel separately.
 * 
 * @since 2.3.2
 * @author Theko
 */
public class MultiChannelAudioFilter {
    private AudioFilter[] audioFilters;

    /**
     * Constructs a new MultiChannelAudioFilter object with the specified filter type, order, and number of channels.
     * @param type the filter type
     * @param order the order of the filter (must be an even number between 2 and 8)
     * @param numChannels the number of channels
     * @throws IllegalArgumentException if the number of channels is less than 1
     */
    public MultiChannelAudioFilter(FilterType type, int order, int numChannels) {
        if (numChannels < 1) {
            throw new IllegalArgumentException("Number of channels must be at least 1.");
        }
        audioFilters = new AudioFilter[numChannels];
        for (int i = 0; i < numChannels; i++) {
            audioFilters[i] = new AudioFilter(type, order);
        }
    }

    /**
     * Processes the given audio samples for each channel separately.
     * @param samples a 2D array of audio samples
     * @param sampleRate the sample rate
     * @return the processed audio samples
     */
    public float[][] process(float[][] samples, int sampleRate) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] = audioFilters[i].process(samples[i], sampleRate);
        }
        return samples;
    }

    /**
     * Processes the given audio samples for the specified channel.
     * @param samples the audio samples
     * @param sampleRate the sample rate
     * @param channel the channel index
     * @return the processed audio samples
     */
    public float[] process(float[] samples, int sampleRate, int channel) {
        return audioFilters[channel].process(samples, sampleRate);
    }

    /**
     * Processes the given audio sample for the specified channel.
     * @param sample the audio sample
     * @param sampleRate the sample rate
     * @param channel the channel index
     * @return the processed audio sample
     */
    public float process(float sample, int sampleRate, int channel) {
        return audioFilters[channel].process(sample, sampleRate);
    }

    /**
     * Sets the parameters of the filter for each channel separately.
     * This method does not update the filter coefficients.
     * Use the {@link #updateCoefficients(int)} method to update the filter coefficients.
     * 
     * @param cutoff the cutoff frequency in Hz
     * @param bandwidth the bandwidth in Hz
     * @param gain the linear gain multiplier
     */
    public void setParameters(float cutoff, float bandwidth, float gain) {
        for (int i = 0; i < audioFilters.length; i++) {
            audioFilters[i].setParameters(cutoff, bandwidth, gain);
        }
    }

    /**
     * Updates the filter coefficients for each channel based on the current control values and the provided sample rate.
     * @param sampleRate the sample rate
     */
    public void updateCoefficients(int sampleRate) {
        for (int i = 0; i < audioFilters.length; i++) {
            audioFilters[i].updateCoefficients(sampleRate);
        }
    }

    /**
     * Returns the array of AudioFilter objects that process audio samples for each channel separately.
     * @return the array of AudioFilter objects
     */
    public AudioFilter[] getAudioFilters() {
        return audioFilters;
    }
}
