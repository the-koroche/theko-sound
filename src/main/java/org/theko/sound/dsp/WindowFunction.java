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
 * A class that provides static methods for generating window functions used in digital signal processing (DSP).
 * 
 * <p>
 * Window functions are commonly applied to signals to reduce spectral leakage when performing Fourier transforms.
 * Each window type provides a different trade-off between main lobe width and side lobe attenuation.
 * </p>
 * 
 * <p>
 * Usage example:
 * <pre>
 * float[] samples = ...; // audio data
 * float[] windowedSamples = WindowFunction.apply(samples, WindowType.HANN);
 * </pre>
 * </p>
 * 
 * @since 1.4.1
 * @author Theko
 */
public class WindowFunction {

    private WindowFunction() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Applies a window function to the given data.
     * 
     * @param data the input array of samples to be windowed
     * @param type the type of window function to apply
     * @return a new array containing the windowed samples
     */
    public static float[] apply(float[] data, WindowType type) {
        float[] window = generate(type, data.length);
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i] * window[i];
        }
        return result;
    }

    /**
     * Modifies the given array of samples by applying a window function.
     * 
     * @param data the input array of samples to be windowed, in float[channel][sample] format
     * @param type the type of window function to apply
     * @return the modified array of samples
     */
    public static float[] applyInPlace(float[] data, WindowType type) {
        for (int i = 0; i < data.length; i++) {
            data[i] *= generateOne(type, i, data.length);
        }
        return data;
    }

    /**
     * Applies a window function to the given data.
     * 
     * @param data the input array of samples to be windowed, in float[channel][sample] format
     * @param type the type of window function to apply
     * @return a new array containing the windowed samples
     */
    public static float[][] apply(float[][] data, WindowType type) {
        for (int i = 0; i < data.length; i++) {
            data[i] = apply(data[i], type);
        }
        return data;
    }

    public static float[][] applyInPlace(float[][] data, WindowType type) {
        for (int i = 0; i < data.length; i++) {
            applyInPlace(data[i], type);
        }
        return data;
    }

    /**
     * Generates a window function of the specified type and size.
     * 
     * @param type the type of window function to generate
     * @param size the size of the window to generate
     * @return a new array containing the generated window function coefficients
     */
    public static float[] generate(WindowType type, int size) {
        return generate(type, size, getDefaultParam(type));
    }

    /**
     * Generates a window function of the specified type and size.
     * 
     * @param type the type of window function to generate
     * @param size the size of the window to generate
     * @param param the parameter value for the window function
     * @return a new array containing the generated window function coefficients
     */
    public static float[] generate(WindowType type, int size, float param) {
        float[] window = new float[size];
        for (int i = 0; i < size; i++) {
            window[i] = generateOne(type, i, size, param);
        }
        return window;
    }

    /**
     * Generates a window function of the specified type and size.
     * 
     * @param type the type of window function to generate
     * @param size the size of the window to generate
     * @return a single float window value
     */
    public static float generateOne(WindowType type, int i, int size) {
        return generateOne(type, i, size, getDefaultParam(type));
    }

    /**
     * Generates a window function of the specified type and size.
     * 
     * @param type the type of window function to generate
     * @param size the size of the window to generate
     * @param param the parameter value for the window function
     * @return a single float window value
     */
    public static float generateOne(WindowType type, int i, int size, float param) {
        switch (type) {
            case RECTANGULAR:
                return 1.0f;
            case HANN:
                return (float)(0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1))));
            case HAMMING:
                return (float)(0.54 - 0.46 * Math.cos(2 * Math.PI * i / (size - 1)));
            case BLACKMAN:
                return (float)(0.42 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1))
                                   + 0.08 * Math.cos(4 * Math.PI * i / (size - 1)));
            case BLACKMAN_HARRIS:
                return (float)(0.35875 - 0.48829 * Math.cos(2 * Math.PI * i / (size - 1))
                                   + 0.14128 * Math.cos(4 * Math.PI * i / (size - 1))
                                   - 0.01168 * Math.cos(6 * Math.PI * i / (size - 1)));
            case FLAT_TOP:
                return (float)(1 - 1.93 * Math.cos(2 * Math.PI * i / (size - 1))
                                   + 1.29 * Math.cos(4 * Math.PI * i / (size - 1))
                                   - 0.388 * Math.cos(6 * Math.PI * i / (size - 1))
                                   + 0.032 * Math.cos(8 * Math.PI * i / (size - 1)));
            case TRIANGULAR:
                return (float)(1 - Math.abs((i - (size - 1) / 2.0) / ((size - 1) / 2.0)));
            case WELCH:
                return (float)(1 - Math.pow((i - (size - 1) / 2.0) / ((size - 1) / 2.0), 2));
            case COSINE:
                return (float)(Math.sin(Math.PI * i / (size - 1)));
            case KAISER:
                double beta = param;
                double denom = besselI0(beta);
                double ratio = (2.0 * i) / (size - 1) - 1.0;
                return (float)(besselI0(beta * Math.sqrt(1 - ratio * ratio)) / denom);
            case GAUSSIAN:
                double sigma = param;
                double m = (size - 1) / 2.0;
                double n = (i - m) / (sigma * m);
                return (float)Math.exp(-0.5 * n * n);
            case TUKEY:
                double alpha = param;
                if (i < alpha * (size - 1) / 2)
                    return (float)(0.5 * (1 + Math.cos(Math.PI * ((2.0 * i) / (alpha * (size - 1)) - 1))));
                else if (i > (size - 1) * (1 - alpha / 2))
                    return (float)(0.5 * (1 + Math.cos(Math.PI * ((2.0 * i) / (alpha * (size - 1)) - 2 / alpha + 1))));
                else
                    return 1.0f;
            case NUTTALL:
                double a = 2 * Math.PI * i / (size - 1);
                return (float)(0.355768 - 0.487396 * Math.cos(a)
                                + 0.144232 * Math.cos(2 * a)
                                - 0.012604 * Math.cos(3 * a));
            default:
                throw new IllegalArgumentException("Unsupported window type: " + type);
        }
    }

    /** Bessel function of the first kind of order zero. */
    private static double besselI0(double x) {
        double sum = 1.0, y = x * x / 4.0, t = y;
        for (int k = 1; t > 1e-10; k++) {
            sum += t;
            t *= y / (k * k);
        }
        return sum;
    }

    /** Returns the default parameter value for the specified window type. */
    private static float getDefaultParam(WindowType type) {
        switch (type) {
            case KAISER: return 8.6f;
            case GAUSSIAN: return 0.4f;
            case TUKEY: return 0.5f;
            default: return 0f;
        }
    }

    /**
     * Returns the amplitude compensation factor for the specified window type.
     * <p>
     * This factor compensates for the average amplitude loss caused by the window function.
     * It is typically used to normalize the output after windowing.
     * </p>
     *
     * @param type the window type
     * @return the compensation factor (multiplier)
     */
    public static float getCompensation(WindowType type) {
        switch (type) {
            case RECTANGULAR: return 1.0f;     // No attenuation
            case HAMMING: return 0.54f;
            case HANN: return 0.5f;
            case BLACKMAN: return 0.42f;
            case BLACKMAN_HARRIS: return 0.4f;
            case FLAT_TOP: return 0.2156f;  // Strong attenuation, wide main lobe
            case TRIANGULAR: return 0.5f;
            case WELCH: return 0.67f;    // Parabolic shape, moderate energy loss
            case COSINE: return 0.6366f;  // Integral of sine over its period / length
            case KAISER: return 1.0f;     // Depends on beta, default unscaled
            case GAUSSIAN: return 0.4f;     // Approx. for sigma = 0.4
            case TUKEY: return 0.5f;     // Approx. for alpha = 0.5 (between RECT and HANN)
            case NUTTALL: return 0.3558f;
            default: return 1.0f;
        }
    }
}
