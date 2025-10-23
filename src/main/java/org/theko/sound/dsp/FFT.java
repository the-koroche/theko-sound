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
 * The {@code FFT} class provides static methods for performing the Fast Fourier Transform (FFT)
 * and its inverse (IFFT) on complex sequences represented by separate arrays of real and imaginary parts.
 * <p>
 * This implementation uses the Cooley-Tukey radix-2 decimation-in-time algorithm and operates in-place
 * on the provided arrays. The input arrays must have a length that is a power of two.
 * 
 * <p>
 * Usage example:
 * <pre>
 * float[] real = ...; // real parts of the input
 * float[] imag = ...; // imaginary parts of the input
 * FFT.fft(real, imag); // Computes the FFT
 * FFT.ifft(real, imag); // Computes the inverse FFT
 * </pre>
 * 
 * This class cannot be instantiated.
 * 
 * @since 1.3.0
 * @author Theko
 */
public class FFT {

    private FFT() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Computes the Fast Fourier Transform (FFT) of a complex sequence defined by
     * the real and imaginary parts stored in the given arrays.
     *
     * @param real the real part of the complex sequence
     * @param imag the imaginary part of the complex sequence
     * @throws IllegalArgumentException if the length of the two arrays is not
     *         equal
     */
    public static void fft(float[] real, float[] imag) {
        int n = real.length;
        int logN = Integer.numberOfTrailingZeros(n); // log2(n)

        // Reorder the real and imaginary parts using bit reversal
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - logN);
            if (i < j) {
                swap(real, i, j);
                swap(imag, i, j);
            }
        }

        // Perform the Cooley-Tukey FFT algorithm
        for (int size = 2; size <= n; size *= 2) {
            int halfSize = size / 2;
            float wAngle = (float) (-2 * Math.PI / size);
            float wReal = (float) Math.cos(wAngle);
            float wImag = (float) Math.sin(wAngle);

            // Process each subarray of the size
            for (int start = 0; start < n; start += size) {
                float uReal = 1.0f;
                float uImag = 0.0f;

                // Process each pair of elements in the subarray
                for (int i = 0; i < halfSize; i++) {
                    int evenIndex = start + i;
                    int oddIndex = start + i + halfSize;

                    // Compute the complex products and add them to the even and odd elements
                    float tReal = uReal * real[oddIndex] - uImag * imag[oddIndex];
                    float tImag = uReal * imag[oddIndex] + uImag * real[oddIndex];

                    real[oddIndex] = real[evenIndex] - tReal;
                    imag[oddIndex] = imag[evenIndex] - tImag;
                    real[evenIndex] += tReal;
                    imag[evenIndex] += tImag;

                    // Compute the new values of uReal and uImag
                    float tmpReal = uReal * wReal - uImag * wImag;
                    uImag = uReal * wImag + uImag * wReal;
                    uReal = tmpReal;
                }
            }
        }
    }

    /**
     * Computes the inverse Fast Fourier Transform (IFFT) of the given real and 
     * imaginary components of a complex sequence. This method modifies the input 
     * arrays in place to perform the transformation.
     *
     * <p>The method first negates the imaginary parts, calls the FFT method,
     * and then scales down the real and imaginary components by dividing them 
     * by the length of the input arrays. Finally, it negates the imaginary 
     * parts again to complete the inverse transformation.
     *
     * @param real the array containing the real parts of the complex input and 
     *             output.
     * @param imag the array containing the imaginary parts of the complex input 
     *             and output.
     */
    public static void ifft(float[] real, float[] imag) {
        // Negate the imaginary parts
        int n = real.length;
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }

        // Call the FFT method
        fft(real, imag);

        // Scale down the real and imaginary components
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] /= n;
        }

        // Negate the imaginary parts again to complete the inverse transformation
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }
    }

    /**
     * Swaps two elements in an array of floats
     * @param arr the array
     * @param i the first index
     * @param j the second index
     */
    private static void swap(float[] arr, int i, int j) {
        float temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
