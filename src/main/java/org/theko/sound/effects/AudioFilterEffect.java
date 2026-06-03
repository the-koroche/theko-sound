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

package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.dsp.ChannelSplittedFilter;
import org.theko.sound.dsp.CutoffAudioFilter;
import org.theko.sound.dsp.FilterType;

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
 * @since 0.2.3-beta
 * @author Theko
 */
public class AudioFilterEffect<T extends CutoffAudioFilter> extends AudioEffect {

    protected final FloatControl
        cutoff = new FloatControl("Cutoff", 10, 22000, 1000),
        passes = new FloatControl("Passes", 1, 32, 4);

    protected final FloatControl
            lowPass = new FloatControl("Low Pass", 0.0f, 1.0f, 1.0f),
            highPass = new FloatControl("High Pass", 0.0f, 1.0f, 0.5f),
            bandPass = new FloatControl("Band Pass", 0.0f, 1.0f, 0.0f);

    protected final List<AudioControl> filterControls = List.of(
        cutoff, passes,
        lowPass, highPass, bandPass
    );

    private final Class<T> filterClass;
    protected ChannelSplittedFilter<T> lowPassFilter;
    protected ChannelSplittedFilter<T> bandPassFilter;
    protected ChannelSplittedFilter<T> highPassFilter;

    private int lastChannels = -1;

    /**
     * Creates a new instance of the {@link AudioFilterEffect} class.
     */
    public AudioFilterEffect(Class<T> filterClass) {
        super(Type.REALTIME);
        this.filterClass = filterClass;
        addEffectControls(filterControls);

        createNewFilters(2 /* default channels */);

        cutoff.addConsumer((event, type) -> {
            synchronized (this) {
                lowPassFilter.getFilters().forEach(filter -> filter.setCutoffValue(cutoff.getValue()));
                bandPassFilter.getFilters().forEach(filter -> filter.setCutoffValue(cutoff.getValue()));
                highPassFilter.getFilters().forEach(filter -> filter.setCutoffValue(cutoff.getValue()));
            }
        });
        passes.addConsumer((event, type) -> {
            synchronized (this) {
                createNewFilters(lastChannels);
            }
        });
    }

    /**
     * Returns the cutoff control of the audio filter effect.
     * This control allows adjusting the cutoff frequency of the filter in Hz.
     *
     * @return The FloatControl representing the cutoff control of the audio filter effect
     */
    public FloatControl getCutoffControl() {
        return cutoff;
    }

    /**
     * Returns the passes control of the audio filter effect.
     * This control allows adjusting the number of passes of the filter.
     *
     * @return The FloatControl representing the passes control of the audio filter effect
     */
    public FloatControl getPassesControl() {
        return passes;
    }

    /**
     * Returns the low-pass control of the audio filter effect.
     * This control allows adjusting the proportion of low frequencies passed through the filter.
     * A value of 1.0 will pass all low frequencies, while a value of 0.0 will pass none.
     *
     * @return The FloatControl representing the low-pass control of the audio filter effect
     */
    public FloatControl getLowPassControl() {
        return lowPass;
    }

    /**
     * Returns the high-pass control of the audio filter effect.
     * This control allows adjusting the proportion of high frequencies passed through the filter.
     * A value of 1.0 will pass all high frequencies, while a value of 0.0 will pass none.
     *
     * @return The FloatControl representing the high-pass control of the audio filter effect
     */
    public FloatControl getHighPassControl() {
        return highPass;
    }

    /**
     * Returns the band-pass control of the audio filter effect.
     * This control allows adjusting the proportion of midrange frequencies passed through the filter.
     * A value of 1.0 will pass all midrange frequencies, while a value of 0.0 will pass none.
     *
     * @return The FloatControl representing the band-pass control of the audio filter effect
     */
    public FloatControl getBandPassControl() {
        return bandPass;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        if (lastChannels != samples.length) {
            synchronized (this) {
                createNewFilters(samples.length);
            }
            lastChannels = samples.length;
        }
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
        try {
            lowPassFilter = new ChannelSplittedFilter<>(
                filterClass.getDeclaredConstructor(FilterType.class, int.class)
                    .newInstance(FilterType.LOWPASS, (int)passes.getValue()),
                channels
            );
            bandPassFilter = new ChannelSplittedFilter<>(
                filterClass.getDeclaredConstructor(FilterType.class, int.class)
                    .newInstance(FilterType.BANDPASS, (int)passes.getValue()),
                channels
            );
            highPassFilter = new ChannelSplittedFilter<>(
                filterClass.getDeclaredConstructor(FilterType.class, int.class)
                    .newInstance(FilterType.HIGHPASS, (int)passes.getValue()),
                channels
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public ChannelSplittedFilter<T> getLowPassFilter() {
        return lowPassFilter;
    }

    public ChannelSplittedFilter<T> getBandPassFilter() {
        return bandPassFilter;
    }

    public ChannelSplittedFilter<T> getHighPassFilter() {
        return highPassFilter;
    }
}