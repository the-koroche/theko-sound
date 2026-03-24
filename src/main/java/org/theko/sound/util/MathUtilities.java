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

package org.theko.sound.util;

/**
 * Utility class for mathematical operations related to audio processing.
 * <p>This class provides methods for remapping values between ranges, linear interpolation,
 * and quantization of floating-point values.
 *
 * @since 0.2.0-beta
 * @author Theko
 */
public final class MathUtilities {

    private MathUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Remaps a value from one range to another without clamping the result.
     *
     * @param x The value to remap
     * @param inMin The minimum of the input range
     * @param inMax The maximum of the input range
     * @param outMin The minimum of the output range
     * @param outMax The maximum of the output range
     * @return The remapped value
     */
    public static float remapUnclamped(float x, float inMin, float inMax, float outMin, float outMax) {
        if (inMax == inMin) return outMin;
        return ((x - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }

    /**
     * Remaps a value from one range to another without clamping the result.
     *
     * @param x The value to remap
     * @param inMin The minimum of the input range
     * @param inMax The maximum of the input range
     * @param outMin The minimum of the output range
     * @param outMax The maximum of the output range
     * @return The remapped value
     */
    public static double remapUnclamped(double x, double inMin, double inMax, double outMin, double outMax) {
        if (inMax == inMin) return outMin;
        return ((x - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }

    /**
     * Remaps a value from one range to another, clamping the result to the output range.
     *
     * @param x The value to remap
     * @param inMin The minimum of the input range
     * @param inMax The maximum of the input range
     * @param outMin The minimum of the output range
     * @param outMax The maximum of the output range
     * @return The remapped value, clamped to the output range
     */
    public static float remapClamped(float x, float inMin, float inMax, float outMin, float outMax) {
        return Math.min(outMax, Math.max(outMin, remapUnclamped(x, inMin, inMax, outMin, outMax)));
    }

    /**
     * Remaps a value from one range to another, clamping the result to the output range.
     *
     * @param x The value to remap
     * @param inMin The minimum of the input range
     * @param inMax The maximum of the input range
     * @param outMin The minimum of the output range
     * @param outMax The maximum of the output range
     * @return The remapped value, clamped to the output range
     */
    public static double remapClamped(double x, double inMin, double inMax, double outMin, double outMax) {
        double result = remapUnclamped(x, inMin, inMax, outMin, outMax);
        if (result < outMin) return outMin;
        if (result > outMax) return outMax;
        return result;
    }

    /**
     * Linearly interpolates between two values based on a parameter t.
     *
     * @param a The start value
     * @param b The end value
     * @param t The interpolation parameter, typically in the range [0, 1]
     * @return The interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Linearly interpolates between two values based on a parameter t.
     *
     * @param a The start value
     * @param b The end value
     * @param t The interpolation parameter, typically in the range [0, 1]
     * @return The interpolated value
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Quantizes a floating-point value to the nearest multiple of a specified step size.
     *
     * @param x The value to quantize
     * @param step The step size for quantization
     * @return The quantized value
     */
    public static float quantize(float x, float step) {
        return Math.round(x / step) * step;
    }

    /**
     * Quantizes a floating-point value to the nearest multiple of a specified step size.
     *
     * @param x The value to quantize
     * @param step The step size for quantization
     * @return The quantized value
     */
    public static double quantize(double x, double step) {
        return Math.round(x / step) * step;
    }

    /** Clamps a value within a specified range.
     *
     * @param x The value to clamp
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return The clamped value
     */
    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(min, x));
    }

    /** Clamps a value within a specified range.
     *
     * @param x The value to clamp
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return The clamped value
     */
    public static double clamp(double x, double min, double max) {
        return Math.min(max, Math.max(min, x));
    }

    /** Clamps a value within a specified range.
     *
     * @param x The value to clamp
     * @param min The minimum value of the range
     * @param max The maximum value of the range
     * @return The clamped value
     */
    public static int clamp(int x, int min, int max) {
        return Math.min(max, Math.max(min, x));
    }

    /**
     * Maps a tension value (t) from the range [-1..1] to a power value
     * (pow) in the range [0.2..5]. The mapping is linear for positive t values
     * and scales the negative t values to fit within the range.
     * @param t The tension value to map
     * @return The mapped power value
     */
    public static float mapTension(float t) {
        // -1..1 -> 0.2..5
        t = clamp(t, -1f, 1f);
        return (t >= 0f)
            ? (1f + t * 4f)      // 1..5
            : (1f + t * 0.8f);   // 0.2..1
    }

    /**
     * Checks if a given integer is a power of 2.
     *
     * A power of 2 is an integer of the form 2^n, where n is an integer, e.g. 1, 2, 4, 8, 16, etc.
     *
     * This method uses the bitwise AND (&) operator to check if the given integer is a power of 2.
     * The expression x > 0 && (x & (x - 1)) == 0 is true if and only if x is a power of 2 and positive.
     *
     * @param x The integer to check
     * @return true if x is a power of 2, false otherwise
     */
    public static boolean isPowerOf2(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }
}
