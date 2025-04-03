package org.theko.sound.fft;

public class WindowFunction {
    public static float[] apply(float[] data, WindowType type) {
        double[] window = type.generate(data.length);
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (float)(data[i] * window[i]);
        }
        return result;
    }
}