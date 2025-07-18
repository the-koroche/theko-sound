package org.theko.sound.dsp;

/**
 * Enum representing various window functions used in digital signal processing (DSP).
 * <p>
 * Window functions are commonly applied to signals to reduce spectral leakage
 * when performing Fourier transforms. Each window type provides a different
 * trade-off between main lobe width and side lobe attenuation.
 * </p>
 *
 * @since v1.4.1
 * @author Theko
 */
public enum WindowType {

    /** No windowing, all coefficients are 1.0 */
    RECTANGULAR,

    /** Hamming window, reduces side lobes compared to rectangular */
    HAMMING,

    /** Hann (Hanning) window, similar to Hamming but with different coefficients */
    HANN,

    /** Blackman window, offers better side lobe suppression */
    BLACKMAN,

    /** Blackman-Harris window, further improves side lobe attenuation */
    BLACKMAN_HARRIS,

    /** Flat-top window, designed for accurate amplitude measurements */
    FLAT_TOP,

    /** Triangular window, linear tapering */
    TRIANGULAR,

    /** Welch window, parabolic shape */
    WELCH,

    /** Cosine window, single cosine cycle */
    COSINE,

    /** Kaiser window (requires beta parameter to generate) */
    KAISER,

    /** Gaussian window (requires sigma parameter to generate) */
    GAUSSIAN,

    /** Nuttall window, smoother and wider than Blackman-Harris */
    NUTTALL,

    /** Tukey window (parameter alpha between rectangular and Hann) */
    TUKEY

}
