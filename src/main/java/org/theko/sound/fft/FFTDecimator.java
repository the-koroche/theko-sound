package org.theko.sound.fft;

public class FFTDecimator {
    private FFTDecimator () {
    }

    public static float[] decimate(float[] signal, int factor) {
        int newSize = signal.length / factor;
        float[] decimatedSignal = new float[newSize];
    
        for (int i = 0; i < newSize; i++) {
            decimatedSignal[i] = signal[i * factor];
        }
    
        return decimatedSignal;
    }
}
