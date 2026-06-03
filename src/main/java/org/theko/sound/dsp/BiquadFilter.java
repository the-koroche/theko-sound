package org.theko.sound.dsp;

import java.util.List;

import org.theko.events.EventConsumer;
import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.Controllable;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;

public class BiquadFilter extends CutoffAudioFilter implements Controllable {

    public class Stage extends CutoffAudioFilter implements Controllable {

        private float b0, b1, b2, a1, a2;
        private float x1, x2, y1, y2;

        protected final FloatControl
                cutoff = new FloatControl("Cutoff", 1, 22000, 1000),
                q      = new FloatControl("Q", 0, 3, 0.707f),
                gain   = new FloatControl("Gain", 0, 2, 1);

        protected final List<AudioControl> filterControls = List.of(cutoff, q, gain);

        private int lastSampleRate = -1;

        public Stage(FilterType filterType) {
            super(filterType);

            // Add listeners
            EventConsumer<AudioControlEvent, AudioControlEventType> consumer =
                    (event, type) -> update(lastSampleRate);
            this.cutoff.addConsumer(consumer);
            this.q.addConsumer(consumer);
            this.gain.addConsumer(consumer);
        }

        @Override
        public FloatControl getCutoff() {
            return cutoff;
        }

        public FloatControl getQ() {
            return q;
        }

        public FloatControl getGain() {
            return gain;
        }

        public void update(int sampleRate) {
            float A = (float)Math.sqrt(gain.getValue());
            float omega = (float) (2.0 * Math.PI * cutoff.getValue() / sampleRate);
            float sn = (float) Math.sin(omega);
            float cs = (float) Math.cos(omega);
            float alpha = sn / (2.0f * q.getValue());

            float a0;

            switch (filterType) {
                case LOWPASS:
                    b0 = (1 - cs) / 2;
                    b1 = 1 - cs;
                    b2 = (1 - cs) / 2;
                    a0 = 1 + alpha;
                    a1 = -2 * cs;
                    a2 = 1 - alpha;
                    break;

                case HIGHPASS:
                    b0 = (1 + cs) / 2;
                    b1 = -(1 + cs);
                    b2 = (1 + cs) / 2;
                    a0 = 1 + alpha;
                    a1 = -2 * cs;
                    a2 = 1 - alpha;
                    break;

                case BANDPASS:
                    b0 = alpha;
                    b1 = 0;
                    b2 = -alpha;
                    a0 = 1 + alpha;
                    a1 = -2 * cs;
                    a2 = 1 - alpha;
                    break;

                case NOTCH:
                    b0 = 1;
                    b1 = -2 * cs;
                    b2 = 1;
                    a0 = 1 + alpha;
                    a1 = -2 * cs;
                    a2 = 1 - alpha;
                    break;

                case PEAK:
                    b0 = 1 + alpha * A;
                    b1 = -2 * cs;
                    b2 = 1 - alpha * A;
                    a0 = 1 + alpha / A;
                    a1 = -2 * cs;
                    a2 = 1 - alpha / A;
                    break;

                case ALLPASS:
                    b0 = 1 - alpha;
                    b1 = -2 * cs;
                    b2 = 1 + alpha;
                    a0 = 1 + alpha;
                    a1 = -2 * cs;
                    a2 = 1 - alpha;
                    break;

                default:
                    throw new IllegalArgumentException("Invalid filter type");
            }

            b0 /= a0;
            b1 /= a0;
            b2 /= a0;
            a1 /= a0;
            a2 /= a0;
        }

        @Override
        public float process(float input, int sampleRate) {
            if (sampleRate != lastSampleRate) {
                update(sampleRate);
                lastSampleRate = sampleRate;
            }
            float y0 = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

            x2 = x1;
            x1 = input;
            y2 = y1;
            y1 = y0;

            return y0;
        }

        @Override
        public AudioFilter copyFilter() {
            Stage copy = new Stage(filterType);
            copy.b0 = b0;
            copy.b1 = b1;
            copy.b2 = b2;
            copy.a1 = a1;
            copy.a2 = a2;
            copy.x1 = x1;
            copy.x2 = x2;
            copy.y1 = y1;
            copy.y2 = y2;
            return copy;
        }

        @Override
        public List<AudioControl> getAllControls() {
            return filterControls;
        }
    }

    protected final FloatControl
        cutoff = new FloatControl("Cutoff", 1, 22000, 1000),
        q      = new FloatControl("Q", 0, 3, 0.707f),
        gain   = new FloatControl("Gain", 0, 2, 1);

    protected final List<AudioControl> filterControls = List.of(cutoff, q, gain);

    private Stage[] biquads;

    public BiquadFilter(FilterType filterType, int order) {
        super(filterType);
        if (order % 2 != 0) {
            order++;
        }
        if (order < 2) {
            throw new IllegalArgumentException("Order must be an even number and at least 2");
        }

        int stages = order / 2;
        biquads = new Stage[stages];
        for (int i = 0; i < stages; i++) {
            biquads[i] = new Stage(filterType);
        }

        // Add listeners
        cutoff.addConsumer((event, type) -> {
            for (Stage biquad : biquads) {
                biquad.cutoff.setValue(cutoff.getValue());
            }
        });

        q.addConsumer((event, type) -> {
            for (Stage biquad : biquads) {
                biquad.q.setValue(q.getValue());
            }
        });

        gain.addConsumer((event, type) -> {
            for (Stage biquad : biquads) {
                biquad.gain.setValue(gain.getValue());
            }
        });
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public int getOrder() {
        return biquads.length * 2;
    }

    public FloatControl getCutoff() {
        return cutoff;
    }

    public FloatControl getQ() {
        return q;
    }

    public FloatControl getGain() {
        return gain;
    }

    public void setParams(float cutoff, float q, float gain) {
        this.cutoff.setValue(cutoff);
        this.q.setValue(q);
        this.gain.setValue(gain);
    }

    public void update(int sampleRate) {
        for (Stage biquad : biquads) {
            biquad.update(sampleRate);
        }
    }

    @Override
    public float process(float sample, int sampleRate) {
        for (Stage biquad : biquads) {
            sample = biquad.process(sample, sampleRate);
        }
        return sample;
    }

    @Override
    public AudioFilter copyFilter() {
        BiquadFilter copy = new BiquadFilter(filterType, getOrder());
        copy.cutoff.setValue(cutoff.getValue());
        copy.q.setValue(q.getValue());
        copy.gain.setValue(gain.getValue());
        return copy;
    }

    @Override
    public List<AudioControl> getAllControls() {
        return filterControls;
    }
}
