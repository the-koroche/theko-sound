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
 * Represents a second-order Butterworth filter.
 * 
 * @see AudioFilter
 * 
 * @since 2.3.2
 * @author Theko
 */
public class BiquadFilter {

    private static final float SMOOTH_FACTOR = 0.005f;

    // current coefficients
    private float b0c, b1c, b2c, a1c, a2c;
    // target coefficients
    private float b0t, b1t, b2t, a1t, a2t;

    private float x1, x2, y1, y2;

    public void update(FilterType type, float cutoff, float q, float gain, float sampleRate) {
        if (gain <= 0) gain = 1.0f;
        if (cutoff <= 0 || cutoff >= sampleRate / 2) cutoff = sampleRate / 4;
        if (q <= 0) q = 0.707f; // Butterworth default

        float A = gain;
        float omega = (float) (2.0 * Math.PI * cutoff / sampleRate);
        float sn = (float) Math.sin(omega);
        float cs = (float) Math.cos(omega);
        float alpha = sn / (2.0f * q);

        float b0, b1, b2, a0, a1, a2;

        switch (type) {
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

        // normalization
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;

        b0t = b0;
        b1t = b1;
        b2t = b2;
        a1t = a1;
        a2t = a2;
    }

    public float process(float input) {
        b0c += (b0t - b0c) * SMOOTH_FACTOR;
        b1c += (b1t - b1c) * SMOOTH_FACTOR;
        b2c += (b2t - b2c) * SMOOTH_FACTOR;
        a1c += (a1t - a1c) * SMOOTH_FACTOR;
        a2c += (a2t - a2c) * SMOOTH_FACTOR;

        float y0 = b0c * input + b1c * x1 + b2c * x2 - a1c * y1 - a2c * y2;

        x2 = x1;
        x1 = input;
        y2 = y1;
        y1 = y0;

        return y0;
    }

    public float[] process(float[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = process(in[i]);
        }
        return out;
    }
}
