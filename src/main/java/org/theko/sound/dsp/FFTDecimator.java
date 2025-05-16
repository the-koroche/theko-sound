package org.theko.sound.dsp;

public class FFTDecimator {
    private FFTDecimator () {
    }

    /**
     * Decimates the input signal by a given factor.
     *
     * @param signal input signal
     * @param factor decimation factor
     * @return decimated signal
     */
    public static float[] decimate(float[] signal, int factor) {
        int newSize = signal.length / factor;
        float[] decimatedSignal = new float[newSize];
    
        for (int i = 0; i < newSize; i++) {
            decimatedSignal[i] = signal[i * factor];
        }
    
        return decimatedSignal;
    }
}
