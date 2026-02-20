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

package org.theko.sound.dsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.theko.sound.samples.SamplesValidation;

/**
 * A channel splintered filter is an wrapper around a list of audio filters.
 * <p>
 * It designed to process multiple audio channels at the same time, without the need to create a new filter for each channel.
 * @param <T> the type of audio filter used for each channel
 * 
 * @see AudioFilter
 * @see CascadeFilter
 * 
 * @since 0.2.4-beta
 * @author Theko
 */
public class ChannelSplittedFilter<T extends AudioFilter> {
    private final List<T> filters;

    /**
     * Creates a channel splintered filter with the provided list of filters.
     * 
     * @param filters the list of filters to use for each channel
     * @throws NullPointerException if the filters list is null
     * @throws IllegalArgumentException if the filters list is empty or contains null elements
     */
    public ChannelSplittedFilter(List<T> filters) {
        Objects.requireNonNull(filters);
        if (filters.isEmpty()) {
            throw new IllegalArgumentException("Filters cannot be empty");
        }
        this.filters = filters;
        for (T filter : filters) {
            if (filter == null) {
                throw new IllegalArgumentException("Filters cannot contain null elements");
            }
        }
    }

    /**
     * Creates a channel splintered filter with a single filter copied for each channel.
     * 
     * @param filter the filter to use for each channel
     * @param channels the number of channels (filters) to use
     * @throws NullPointerException if the filter is null
     * @throws IllegalArgumentException if the number of channels is less than 1
     */
    public ChannelSplittedFilter(T filter, int channels) {
        this.filters = new ArrayList<>();
        recreate(filter, channels);
    }

    /**
     * Recreates the channel splintered filter with a new list of filters.
     * The provided filter is copied for each channel, and the new filters are stored in the internal list.
     * 
     * @param filter the new filter to use for each channel
     * @param channels the number of channels (filters) to use
     * @throws NullPointerException if the filter is null
     * @throws IllegalArgumentException if the number of channels is less than 1
     */
    @SuppressWarnings("unchecked")
    public void recreate(T filter, int channels) {
        Objects.requireNonNull(filter);
        if (channels < 1) {
            throw new IllegalArgumentException("Number of channels must be at least 1.");
        }
        filters.clear();
        for (int i = 0; i < channels; i++) {
            filters.add((T) filter.copyFilter());
        }
    }

    /**
     * Processes a single audio sample using the filter at the specified channel.
     * 
     * @param sample the input sample
     * @param channel the channel (filter) to use
     * @param sampleRate the sample rate in Hz
     * @return the processed sample
     */
    public float process(float sample, int channel, int sampleRate) {
        return filters.get(channel).process(sample, sampleRate);
    }

    /**
     * Processes an array of audio samples and writes the processed samples to the provided output array.
     * 
     * @param samples the input samples
     * @param output the output samples
     * @param channel the channel (filter) to use
     * @param sampleRate the sample rate in Hz
     */
    public void process(float[] samples, float[] output, int channel, int sampleRate) {
        filters.get(channel).process(samples, output, sampleRate);
    }

    /**
     * Processes an array of audio samples and writes the processed samples to the provided output array.
     * 
     * @param samples the input samples
     * @param output the output samples
     * @param sampleRate the sample rate in Hz
     * @throws IllegalArgumentException if the samples and output arrays do not have the same number of channels and samples.
     */
    public void process(float[][] samples, float[][] output, int sampleRate) {
        SamplesValidation.validateSamples(samples);
        SamplesValidation.validateSamples(output);
        if (SamplesValidation.checkSamplesDimensions(samples, output) != SamplesValidation.DimensionsResult.EXACT) {
            throw new IllegalArgumentException("Input and output arrays must have the same number of channels and samples.");
        }
        for (int ch = 0; ch < samples.length; ch++) {
            filters.get(ch).process(samples[ch], output[ch], sampleRate);
        }
    }

    /**
     * Returns the number of channels (filters) used by this filter.
     * 
     * @return the number of channels used by this filter
     */
    public int getChannelCount() {
        return filters.size();
    }

    /**
     * Returns the filter used by the specified channel.
     * 
     * @param channel the channel to retrieve the filter from
     * @return the filter used by the specified channel
     * @throws IndexOutOfBoundsException if the channel is out of range
     */
    public T getFilter(int channel) {
        return filters.get(channel);
    }

    /**
     * Returns an unmodifiable list of the filters used by this channel
     * splitted filter. The list contains one filter per channel.
     * 
     * @return an unmodifiable list of the filters used by this channel
     * splitted filter
     * 
     */
    public List<T> getFilters() {
        return Collections.unmodifiableList(filters);
    }
}
