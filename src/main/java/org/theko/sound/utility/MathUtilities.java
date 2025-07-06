package org.theko.sound.utility;

public class MathUtilities {

    private MathUtilities () {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static double remapUnclamped (double x, double inMin, double inMax, double outMin, double outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public static double remapClamped (double x, double inMin, double inMax, double outMin, double outMax) {
        return Math.min(outMax, Math.max(outMin, remapUnclamped(x, inMin, inMax, outMin, outMax)));
    }

    public static double lerp (double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static float quantize (float x, float step) {
        return Math.round(x / step) * step;
    }
}
