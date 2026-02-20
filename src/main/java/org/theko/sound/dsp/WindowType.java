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

/**
 * Enum representing various window functions used in digital signal processing (DSP).
 * <p>
 * Window functions are commonly applied to signals to reduce spectral leakage
 * when performing Fourier transforms. Each window type provides a different
 * trade-off between main lobe width and side lobe attenuation.
 * 
 *
 * @since 1.4.1
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
