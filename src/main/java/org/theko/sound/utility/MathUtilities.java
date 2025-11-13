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

package org.theko.sound.utility;

/**
 * Utility class for mathematical operations related to audio processing.
 * <p>This class provides methods for remapping values between ranges, linear interpolation,
 * and quantization of floating-point values.
 * 
 * @since 2.0.0
 * @author Theko
 */
public final class MathUtilities {

    private MathUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Remaps a value from one range to another without clamping the result.
     *
     * @param x The value to remap.
     * @param inMin The minimum of the input range.
     * @param inMax The maximum of the input range.
     * @param outMin The minimum of the output range.
     * @param outMax The maximum of the output range.
     * @return The remapped value.
     */
    public static float remapUnclamped(float x, float inMin, float inMax, float outMin, float outMax) {
        if (inMax == inMin) return outMin;
        return ((x - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }

    /**
     * Remaps a value from one range to another without clamping the result.
     *
     * @param x The value to remap.
     * @param inMin The minimum of the input range.
     * @param inMax The maximum of the input range.
     * @param outMin The minimum of the output range.
     * @param outMax The maximum of the output range.
     * @return The remapped value.
     */
    public static double remapUnclamped(double x, double inMin, double inMax, double outMin, double outMax) {
        if (inMax == inMin) return outMin;
        return ((x - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin;
    }

    /**
     * Remaps a value from one range to another, clamping the result to the output range.
     *
     * @param x The value to remap.
     * @param inMin The minimum of the input range.
     * @param inMax The maximum of the input range.
     * @param outMin The minimum of the output range.
     * @param outMax The maximum of the output range.
     * @return The remapped value, clamped to the output range.
     */
    public static float remapClamped(float x, float inMin, float inMax, float outMin, float outMax) {
        return Math.min(outMax, Math.max(outMin, remapUnclamped(x, inMin, inMax, outMin, outMax)));
    }

    /**
     * Remaps a value from one range to another, clamping the result to the output range.
     *
     * @param x The value to remap.
     * @param inMin The minimum of the input range.
     * @param inMax The maximum of the input range.
     * @param outMin The minimum of the output range.
     * @param outMax The maximum of the output range.
     * @return The remapped value, clamped to the output range.
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
     * @param a The start value.
     * @param b The end value.
     * @param t The interpolation parameter, typically in the range [0, 1].
     * @return The interpolated value.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Linearly interpolates between two values based on a parameter t.
     *
     * @param a The start value.
     * @param b The end value.
     * @param t The interpolation parameter, typically in the range [0, 1].
     * @return The interpolated value.
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Quantizes a floating-point value to the nearest multiple of a specified step size.
     *
     * @param x The value to quantize.
     * @param step The step size for quantization.
     * @return The quantized value.
     */
    public static float quantize(float x, float step) {
        return Math.round(x / step) * step;
    }

    /**
     * Quantizes a floating-point value to the nearest multiple of a specified step size.
     *
     * @param x The value to quantize.
     * @param step The step size for quantization.
     * @return The quantized value.
     */
    public static double quantize(double x, double step) {
        return Math.round(x / step) * step;
    }

    /** Clamps a value within a specified range.
     * 
     * @param x The value to clamp.
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @return The clamped value.
     */
    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(min, x));
    }

    /** Clamps a value within a specified range.
     * 
     * @param x The value to clamp.
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @return The clamped value.
     */
    public static double clamp(double x, double min, double max) {
        return Math.min(max, Math.max(min, x));
    }

    /** Clamps a value within a specified range.
     * 
     * @param x The value to clamp.
     * @param min The minimum value of the range.
     * @param max The maximum value of the range.
     * @return The clamped value.
     */
    public static int clamp(int x, int min, int max) {
        return Math.min(max, Math.max(min, x));
    }

    /**
     * Returns the cotangent of a given angle in radians.
     *
     * @param x The angle in radians.
     * @return The cotangent of the angle.
     */
    public static double cot(double x) {
        return 1.0 / Math.tan(x);
    }

    /**
     * Returns the secant of a given angle in radians.
     *
     * @param x The angle in radians.
     * @return The secant of the angle.
     */
    public static double sec(double x) {
        return 1.0 / Math.cos(x);
    }

    /**
     * Returns the cosecant of a given angle in radians.
     *
     * @param x The angle in radians.
     * @return The cosecant of the angle.
     */
    public static double csc(double x) {
        return 1.0 / Math.sin(x);
    }

    /**
     * Returns the hyperbolic cotangent of a number.
     * The hyperbolic cotangent is defined as 1/tanh(x).
     * 
     * @param x The number.
     * @return The hyperbolic cotangent.
     */
    public static double coth(double x) {
        return 1.0 / Math.tanh(x);
    }

    /**
     * Returns the hyperbolic secant of a given number.
     * The hyperbolic secant is defined as 1/cosh(x).
     * 
     * @param x The number.
     * @return The hyperbolic secant of the number.
     */
    public static double sech(double x) {
        return 1.0 / Math.cosh(x);
    }

    /**
     * Returns the hyperbolic cosecant of a given number.
     * The hyperbolic cosecant is defined as 1/sinh(x).
     * 
     * @param x The number.
     * @return The hyperbolic cosecant of the number.
     */
    public static double csch(double x) {
        return 1.0 / Math.sinh(x);
    }
}
