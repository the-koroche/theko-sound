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

package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.dsp.ChannelSplittedFilter;
import org.theko.sound.dsp.FilterType;
import org.theko.sound.dsp.CascadeFilter;

/**
 * A basic audio filter effect that applies low-pass, high-pass, or band-pass filtering to an audio signal.
 *
 * <p>
 * The type of filtering is controlled via the {@link #getLowPassControl()}, {@link #getHighPassControl()},
 * and {@link #getBandPassControl()} methods. The cutoff frequency, bandwidth, and gain can be adjusted
 * through the respective controls.
 *
 * <p>
 * The filter can process audio in either a single-pass or double-pass configuration, allowing for
 * different levels of filtering intensity and response characteristics.
 *
 * @since 2.3.2
 * @author Theko
 */
public class AudioFilterEffect extends AudioEffect {

    private static final int SINGLE_PASS_ORDERS = 4;
    private static final int DOUBLE_PASS_ORDERS = 8;
    
    protected final FloatControl cutoff = new FloatControl("Cutoff", 10, 22000, 1000);
    protected final FloatControl bandwidth = new FloatControl("Bandwidth", 0.1f, 3.0f, 1.92f);
    protected final FloatControl gain = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);

    protected final FloatControl lowPass = new FloatControl("Low Pass", 0.0f, 1.0f, 1.0f);
    protected final FloatControl highPass = new FloatControl("High Pass", 0.0f, 1.0f, 0.5f);
    protected final FloatControl bandPass = new FloatControl("Band Pass", 0.0f, 1.0f, 0.0f);

    protected final BooleanControl doublePass = new BooleanControl("Double Pass", false);
    
    protected final List<AudioControl> filterControls = List.of(
        cutoff, bandwidth, gain,
        lowPass, highPass, bandPass,
        doublePass
    );

    protected ChannelSplittedFilter<CascadeFilter> lowPassFilter;
    protected ChannelSplittedFilter<CascadeFilter> bandPassFilter;
    protected ChannelSplittedFilter<CascadeFilter> highPassFilter;

    private float lastCutoff = -1;
    private float lastBandwidth = -1;
    private float lastGain = -1;
    private boolean lastDoublePass = false;
    private int lastSampleRate = -1;
    private int lastChannels = -1;
    
    public AudioFilterEffect() {
        super(Type.REALTIME);
        addEffectControls(filterControls);
        
        createNewFilters(2 /* channels */);
    }    

    /**
     * Returns the cutoff control of the audio filter effect.
     * This control allows adjusting the cutoff frequency of the filter in Hz.
     * 
     * @return The FloatControl representing the cutoff control of the audio filter effect.
     */
    public FloatControl getCutoffControl() {
        return cutoff;
    }

    /**
     * Returns the bandwidth control of the audio filter effect.
     * This control allows adjusting the bandwidth of the filter in octaves.
     * 
     * @return The FloatControl representing the bandwidth control of the audio filter effect.
     */
    public FloatControl getBandwidthControl() {
        return bandwidth;
    }

    /**
     * Returns the gain control of the audio filter effect.
     * This control allows adjusting the overall gain of the filter, with a value of 1.0 being unity gain.
     * 
     * @return The FloatControl representing the gain control of the audio filter effect.
     */
    public FloatControl getGainControl() {
        return gain;
    }

    /**
     * Returns the low-pass control of the audio filter effect.
     * This control allows adjusting the proportion of low frequencies passed through the filter.
     * A value of 1.0 will pass all low frequencies, while a value of 0.0 will pass none.
     * 
     * @return The FloatControl representing the low-pass control of the audio filter effect.
     */
    public FloatControl getLowPassControl() {
        return lowPass;
    }

    /**
     * Returns the high-pass control of the audio filter effect.
     * This control allows adjusting the proportion of high frequencies passed through the filter.
     * A value of 1.0 will pass all high frequencies, while a value of 0.0 will pass none.
     * 
     * @return The FloatControl representing the high-pass control of the audio filter effect.
     */
    public FloatControl getHighPassControl() {
        return highPass;
    }

    /**
     * Returns the band-pass control of the audio filter effect.
     * This control allows adjusting the proportion of midrange frequencies passed through the filter.
     * A value of 1.0 will pass all midrange frequencies, while a value of 0.0 will pass none.
     * 
     * @return The FloatControl representing the band-pass control of the audio filter effect.
     */
    public FloatControl getBandPassControl() {
        return bandPass;
    }

    /**
     * Returns the double-pass control of the audio filter effect.
     * This control allows enabling or disabling double-pass filtering, which can
     * provide a more intense filtering effect.
     * 
     * @return The BooleanControl representing the double-pass control of the audio filter effect.
     */
    public BooleanControl getDoublePassControl() {
        return doublePass;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        float cutoffVal = cutoff.getValue();
        float bandwidthVal = bandwidth.getValue();
        float gainVal = gain.getValue();
        
        if (lastChannels != samples.length ||
            lastDoublePass != doublePass.isEnabled()) {
            createNewFilters(samples.length);
            lastChannels = samples.length;
            lastDoublePass = doublePass.isEnabled();
        }
        updateFiltersCoefficients(cutoffVal, bandwidthVal, gainVal, sampleRate);

        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                float input = samples[ch][i];
                float low = lowPassFilter.process(input, ch, sampleRate);
                float high = highPassFilter.process(input, ch, sampleRate);
                float band = bandPassFilter.process(input, ch, sampleRate);

                float mixed = low * lowPass.getValue()
                            + high * highPass.getValue()
                            + band * bandPass.getValue();
                samples[ch][i] = mixed;
            }
        }
    }

    /**
     * Creates new filters for the effect, based on the given number of channels and whether double pass is enabled.
     * 
     * @param channels the number of channels to create filters for
     */
    protected void createNewFilters(int channels) {
        int order = (doublePass.isEnabled() ? DOUBLE_PASS_ORDERS : SINGLE_PASS_ORDERS);
        lowPassFilter = new ChannelSplittedFilter<>(new CascadeFilter(FilterType.LOWPASS, order), channels);
        bandPassFilter = new ChannelSplittedFilter<>(new CascadeFilter(FilterType.BANDPASS, order), channels);
        highPassFilter = new ChannelSplittedFilter<>(new CascadeFilter(FilterType.HIGHPASS, order), channels);
    }

    /**
     * Updates the filters used by the effect with the given cutoff frequency, bandwidth, and gain.
     * The update is only performed if the sample rate has changed, to avoid unnecessary computation.
     * 
     * @param cutoff the cutoff frequency
     * @param bandwidth the bandwidth
     * @param gain the gain
     * @param sampleRate the sample rate in Hz
     */
    protected void updateFiltersCoefficients(float cutoff, float bandwidth, float gain, int sampleRate) {
        if (isDirty(sampleRate)) {
            updateEachFilter(lowPassFilter, cutoff, bandwidth, gain, sampleRate);
            updateEachFilter(bandPassFilter, cutoff, bandwidth, gain, sampleRate);
            updateEachFilter(highPassFilter, cutoff, bandwidth, gain, sampleRate);

            lastCutoff = cutoff;
            lastBandwidth = bandwidth;
            lastGain = gain;
            lastSampleRate = sampleRate;
        }
    }

    private static void updateEachFilter(ChannelSplittedFilter<CascadeFilter> filters, float cutoff, float bandwidth, float gain,int sampleRate) {
        for (CascadeFilter filter : filters.getFilters()) {
            filter.update(cutoff, bandwidth, gain, sampleRate);
        }
    }

    private boolean isDirty(int sampleRate) {
        return lastCutoff != cutoff.getValue() ||
                lastBandwidth != bandwidth.getValue() || 
                lastGain != gain.getValue() || 
                lastSampleRate != sampleRate || 
                lastDoublePass != doublePass.isEnabled();
    }
}