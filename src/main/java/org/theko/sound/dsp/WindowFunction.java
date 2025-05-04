package org.theko.sound.dsp;

public class WindowFunction {
    public static float[] apply(float[] data, WindowType type) {
        double[] window = type.generate(data.length);
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (float)(data[i] * window[i]);
        }
        return result;
    }

    public static float[] generate(int size, WindowType type) {
        double[] window = type.generate(size);
        float[] result = new float[window.length];
        for (int i = 0; i < window.length; i++) {
            result[i] = (float)(window[i]);
        }
        return result;
    }
}