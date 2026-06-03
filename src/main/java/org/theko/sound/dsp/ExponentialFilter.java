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

import java.util.List;

import org.theko.events.EventConsumer;
import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.Controllable;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;


public class ExponentialFilter extends CutoffAudioFilter implements Controllable {

    public class Stage extends CutoffAudioFilter implements Controllable {

        private float x1, y, a;

        protected final FloatControl
            cutoff = new FloatControl("Cutoff", 1, 22000, 1000),
            gain = new FloatControl("Gain", 0, 2, 1);

        protected final List<AudioControl> filterControls = List.of(cutoff, gain);

        private int lastSampleRate = -1;

        public Stage(FilterType filterType) {
            super(filterType);

            // Add listeners
            EventConsumer<AudioControlEvent, AudioControlEventType> consumer =
                    (event, type) -> update(lastSampleRate);
            cutoff.addConsumer(consumer);
            gain.addConsumer(consumer);
        }

        public FilterType getFilterType() {
            return filterType;
        }

        @Override
        public FloatControl getCutoff() {
            return cutoff;
        }

        public FloatControl getGain() {
            return gain;
        }

        protected float lp(float x) {
            y = y + a * (x - y);
            return y;
        }

        protected float hp(float x) {
            float low = lp(x);
            return x - low;
        }

        protected float bp(float x) {
            return lp(hp(x));
        }

        protected float notch(float x) {
            float low = lp(x);
            float high = x - low;
            return low + high; // = x, but with phases shifted
        }

        protected float peak(float x, float gain) {
            return x + gain * bp(x);
        }

        protected float allpass(float x) {
            float yOut = -a * x + x1 + a * y;
            x1 = x;
            y = yOut;
            return yOut;
        }

        public void update(int sampleRate) {
            a = 1.0f - (float)Math.exp(-2 * Math.PI * cutoff.getValue() / sampleRate);
            lastSampleRate = sampleRate;
        }

        @Override
        public float process(float input, int sampleRate) {
            if (lastSampleRate != sampleRate) {
                a = 1.0f - (float)Math.exp(-2 * Math.PI * cutoff.getValue() / sampleRate);
                lastSampleRate = sampleRate;
            }
            switch (filterType) {
                case LOWPASS:  return lp(input);
                case HIGHPASS: return hp(input);
                case BANDPASS: return bp(input);
                case NOTCH: return notch(input);
                case PEAK: return peak(input, gain.getValue());
                case ALLPASS: return allpass(input);
                default:
                    throw new IllegalArgumentException("Unsupported filter type: " + filterType);
            }
        }

        @Override
        public Stage copyFilter() {
            Stage copy = new Stage(filterType);
            copy.cutoff.setValue(cutoff.getValue());
            copy.gain.setValue(gain.getValue());
            copy.a = a;
            copy.y = y;
            return copy;
        }

        @Override
        public List<AudioControl> getAllControls() {
            return filterControls;
        }
    }

    protected final FloatControl
            cutoff = new FloatControl("Cutoff", 1, 22000, 1000),
            gain = new FloatControl("Gain", 0, 2, 1);

    protected final List<AudioControl> filterControls = List.of(cutoff, gain);

    private final Stage[] stages;

    public ExponentialFilter(FilterType filterType, int stagesCount) {
        super(filterType);
        this.stages = new Stage[stagesCount];
        for (int i = 0; i < stagesCount; i++) {
            stages[i] = new Stage(filterType);
        }

        // Add listeners
        cutoff.addConsumer((event, type) -> {
            for (Stage stage : stages) {
                stage.cutoff.setValue(cutoff.getValue());
            }
        });

        gain.addConsumer((event, type) -> {
            for (Stage stage : stages) {
                stage.gain.setValue(gain.getValue());
            }
        });
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public int getOrder() {
        return stages.length;
    }

    @Override
    public FloatControl getCutoff() {
        return cutoff;
    }

    public FloatControl getGain() {
        return gain;
    }

    public void update(int sampleRate) {
        for (Stage stage : stages) {
            stage.update(sampleRate);
        }
    }

    @Override
    public float process(float input, int sampleRate) {
        float output = input;
        for (Stage stage : stages) {
            output = stage.process(output, sampleRate);
        }
        return output;
    }

    @Override
    public ExponentialFilter copyFilter() {
        ExponentialFilter copy = new ExponentialFilter(filterType, stages.length);
        copy.cutoff.setValue(cutoff.getValue());
        copy.gain.setValue(gain.getValue());
        for (int i = 0; i < stages.length; i++) {
            copy.stages[i] = stages[i].copyFilter();
        }
        return copy;
    }

    @Override
    public List<AudioControl> getAllControls() {
        return filterControls;
    }
}
