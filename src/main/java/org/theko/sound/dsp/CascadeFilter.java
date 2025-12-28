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

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;

/**
 * Cascade is a digital audio filter that processes mono channel audio samples using a configurable
 * cascade of biquad filter stages. The filter supports adjustable cutoff frequency, bandwidth, and gain,
 * and can be created with different filter types (e.g., low-pass, high-pass) via the FilterType argument.
 * 
 * <p>
 * The filter order must be an even number between 2 and 8, corresponding to the number of biquad stages used.
 * Each stage processes the output of the previous stage, allowing for higher-order filtering.
 * 
 * <p>
 * The filter exposes controls for cutoff, bandwidth, and gain, which can be accessed and modified
 * at runtime. Coefficients are updated based on the current control values and the provided sample rate.
 * 
 * @see ChannelSplittedFilter
 * @see BiquadStage
 * @see FilterType
 * 
 * @since 2.3.2
 * @author Theko
 */
public class CascadeFilter implements AudioFilter, Controllable {

    protected final FloatControl cutoff = new FloatControl("Cutoff", 1, 22000, 4000);
    protected final FloatControl bandwidth = new FloatControl("Bandwidth", 0.1f, 3.0f, 1.92f); // 1.92 octaves ≈ Q 0.707
    protected final FloatControl gain = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);

    protected final List<AudioControl> filterControls = List.of(cutoff, bandwidth, gain);

    protected final FilterType filterType;
    private final BiquadStage[] biquads;

    /**
     * Constructs a new AudioFilter with the specified filter type and order.
     * 
     * @param filterType the type of filter to apply
     * @param order      the order of the filter (must be an even number between 2 and 8)
     */
    public CascadeFilter(FilterType filterType, int order) {
        if (order % 2 != 0 || order < 2 || order > 8) {
            throw new IllegalArgumentException("Order must be an even number between 2 and 8");
        }

        this.filterType = filterType;
        int stages = order / 2;
        biquads = new BiquadStage[stages];
        for (int i = 0; i < stages; i++) {
            biquads[i] = new BiquadStage();
        }
    }

    /**
     * Returns the cutoff control for the filter.
     * 
     * @return the cutoff control
     */
    public FloatControl getCutoffControl() {
        return cutoff;
    }

    /**
     * Returns the bandwidth control for the filter.
     * 
     * @return the bandwidth control
     */
    public FloatControl getBandwidthControl() {
        return bandwidth;
    }

    /**
     * Returns the gain control for the filter.
     * 
     * @return the gain control
     */
    public FloatControl getGainControl() {
        return gain;
    }

    /**
     * Returns the cutoff frequency of the filter.
     * 
     * @return the cutoff frequency in Hz
     */
    public float getCutoff() {
        return cutoff.getValue();
    }

    /**
     * Sets the cutoff frequency of the filter.
     * 
     * @param cutoff the cutoff frequency in Hz
     */
    public void setCutoff(float cutoff) {
        this.cutoff.setValue(cutoff);
    }

    /**
     * Returns the bandwidth of the filter.
     * 
     * @return the bandwidth in Hz
     */
    public float getBandwidth() {
        return bandwidth.getValue();
    }

    /**
     * Sets the bandwidth of the filter.
     * 
     * @param bandwidth the bandwidth in Hz
     */
    public void setBandwidth(float bandwidth) {
        this.bandwidth.setValue(bandwidth);
    }

    /**
     * Returns the linear gain multiplier of the filter.
     * 
     * @return the gain multiplier
     */
    public float getGain() {
        return gain.getValue();
    }

    /**
     * Sets the linear gain multiplier of the filter.
     * 
     * @param gain the gain multiplier
     */
    public void setGain(float gain) {
        this.gain.setValue(gain);
    }

    /**
     * Returns the filter type of the filter.
     * 
     * @return the filter type
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**
     * Returns the order of the filter.
     * 
     * @return the order of the filter
     */
    public int getOrder() {
        return biquads.length * 2;
    }

    /**
     * Sets the parameters of the filter for each channel separately.
     * This method does not update the filter coefficients.
     * Use the {@link #update(int)} method to update the filter coefficients.
     * 
     * @param cutoff the cutoff frequency in Hz
     * @param bandwidth the bandwidth in Hz
     * @param gain the linear gain multiplier
     */
    public void setParameters(float cutoff, float bandwidth, float gain) {
        this.cutoff.setValue(cutoff);
        this.bandwidth.setValue(bandwidth);
        this.gain.setValue(gain);
    }

    /**
     * Updates the filter coefficients based on the current control values and the provided sample rate.
     * @param sampleRate the sample rate
     */
    public void update(int sampleRate) {
        for (BiquadStage biquad : biquads) {
            biquad.update(
                filterType,
                cutoff.getValue(),
                octavesToQ(bandwidth.getValue()),
                gain.getValue(), sampleRate
            );
        }
    }

    /**
     * Updates the filter coefficients for each channel based on the provided parameters.
     * 
     * @param cutoff the cutoff frequency in Hz
     * @param bandwidth the bandwidth in octaves
     * @param gain the linear gain multiplier
     * @param sampleRate the sample rate
     */
    public void update(float cutoff, float bandwidth, float gain, int sampleRate) {
        setParameters(cutoff, bandwidth, gain);
        for (BiquadStage biquad : biquads) {
            biquad.update(filterType, cutoff, octavesToQ(bandwidth), gain, sampleRate);
        }
    }

    private float octavesToQ(float bandwidth) {
        if (bandwidth <= 0) return 0.707f; // default Q

        if (bandwidth < 0.1f) bandwidth = 0.1f;
        if (bandwidth > 3.0f) bandwidth = 3.0f;

        // Q = 1 / (2*sinh(ln(2)/2 * BW))
        double bw = bandwidth;
        double q = 1.0 / (2.0 * Math.sinh(Math.log(2)/2.0 * bw));
        return (float) q;
    }

    @Override
    public float process(float sample, int sampleRate) {
        for (BiquadStage biquad : biquads) {
            sample = biquad.process(sample, sampleRate);
        }

        return sample;
    }

    @Override
    public AudioFilter copyFilter() {
        CascadeFilter copy = new CascadeFilter(filterType, biquads.length * 2);
        copy.setParameters(cutoff.getValue(), bandwidth.getValue(), gain.getValue());
        return copy;
    }
    
    @Override
    public List<AudioControl> getAllControls() {
        return filterControls;
    }
}
