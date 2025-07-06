package org.theko.sound.dsp;

/**
 * Enum representing various window functions used in digital signal processing (DSP).
 * <p>
 * Window functions are commonly applied to signals to reduce spectral leakage
 * when performing Fourier transforms. Each window type provides a different
 * trade-off between main lobe width and side lobe attenuation.
 * </p>
 * <ul>
 *   <li>{@link #RECTANGULAR} - No windowing, all coefficients are 1.0.</li>
 *   <li>{@link #HAMMING} - Hamming window, reduces side lobes compared to rectangular.</li>
 *   <li>{@link #HANN} - Hann (Hanning) window, similar to Hamming but with different coefficients.</li>
 *   <li>{@link #BLACKMAN} - Blackman window, offers better side lobe suppression.</li>
 *   <li>{@link #BLACKMAN_HARRIS} - Blackman-Harris window, further improves side lobe attenuation.</li>
 *   <li>{@link #FLAT_TOP} - Flat-top window, designed for accurate amplitude measurements.</li>
 *   <li>{@link #TRIANGULAR} - Triangular window, linear tapering.</li>
 *   <li>{@link #WELCH} - Welch window, parabolic shape.</li>
 *   <li>{@link #COSINE} - Cosine window, single cosine cycle.</li>
 * </ul>
 * <p>
 * The {@link #generate(int)} method returns an array of window coefficients for the selected type.
 * </p>
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public enum WindowType {

    RECTANGULAR, HAMMING, HANN, BLACKMAN, BLACKMAN_HARRIS, FLAT_TOP, TRIANGULAR, WELCH, COSINE;

    /**
     * Returns an array of window coefficients for the specified window type.
     * @param size the size of the window
     * @return an array of window coefficients
     */
    public double[] generate (int size) {
        double[] window = new double[size];
        switch (this) {
            // rectangular window
            case RECTANGULAR:
                for (int i = 0; i < size; i++) window[i] = 1.0;
                break;
            // Hanning window
            case HANN:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1)));
                break;
            // Hamming window
            case HAMMING:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (size - 1));
                break;
            // Blackman window
            case BLACKMAN:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1)) 
                                + 0.08 * Math.cos(4 * Math.PI * i / (size - 1));
                break;
            // Blackman-Harris window
            case BLACKMAN_HARRIS:
                for (int i = 0; i < size; i++)
                    window[i] = 0.35875 - 0.48829 * Math.cos(2 * Math.PI * i / (size - 1))
                                + 0.14128 * Math.cos(4 * Math.PI * i / (size - 1))
                                - 0.01168 * Math.cos(6 * Math.PI * i / (size - 1));
                break;
            // Flat-top window
            case FLAT_TOP:
                for (int i = 0; i < size; i++)
                    window[i] = 1 - 1.93 * Math.cos(2 * Math.PI * i / (size - 1))
                                + 1.29 * Math.cos(4 * Math.PI * i / (size - 1))
                                - 0.388 * Math.cos(6 * Math.PI * i / (size - 1))
                                + 0.032 * Math.cos(8 * Math.PI * i / (size - 1));
                break;
            // Triangular window
            case TRIANGULAR:
                for (int i = 0; i < size; i++) 
                    window[i] = 1 - Math.abs((i - (size - 1) / 2.0) / ((size - 1) / 2.0));
                break;
            // Welch window
            case WELCH:
                for (int i = 0; i < size; i++) 
                    window[i] = 1 - Math.pow((i - (size - 1) / 2.0) / ((size - 1) / 2.0), 2);
                break;
            // Cosine window
            case COSINE:
                for (int i = 0; i < size; i++) 
                    window[i] = Math.sin(Math.PI * i / (size - 1));
                break;
        }
        return window;
    }
}
