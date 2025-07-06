package org.theko.sound.dsp;

/**
 * Utility class for applying window functions to audio sample data.
 * <p>
 * This class provides static methods to apply various windowing functions
 * (such as Hamming, Hann, Blackman, etc.) to arrays of floating-point audio samples.
 * Windowing is commonly used in digital signal processing to reduce spectral leakage
 * when performing operations such as the Fast Fourier Transform (FFT).
 * </p>
 * <p>
 * The class cannot be instantiated.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * float[] samples = ...; // audio data
 * float[] windowed = WindowFunction.apply(samples, WindowType.HAMMING);
 * </pre>
 * 
 * @since v1.4.1
 *
 * @author theko
 */
public class WindowFunction {

    private WindowFunction () {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Applies a windowing function to the given data array.
     *
     * @param data the input array of samples to be windowed
     * @param type the type of window function to apply
     * @return a new array containing the windowed samples
     */
    public static float[] apply (float[] data, WindowType type) {
        double[] window = type.generate(data.length);
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (float)(data[i] * window[i]);
        }
        return result;
    }

    /**
     * Generates a window of the specified type and size.
     *
     * @param size the size of the window to generate
     * @param type the type of window function to use
     * @return a new array containing the generated window coefficients as floats
     */
    public static float[] generate (int size, WindowType type) {
        double[] window = type.generate(size);
        float[] result = new float[window.length];
        for (int i = 0; i < window.length; i++) {
            result[i] = (float)(window[i]);
        }
        return result;
    }
}