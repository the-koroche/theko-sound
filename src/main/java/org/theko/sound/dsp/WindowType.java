package org.theko.sound.dsp;

public enum WindowType {
    RECTANGULAR, HAMMING, HANN, BLACKMAN, BLACKMAN_HARRIS, FLAT_TOP, TRIANGULAR, WELCH, COSINE;

    public double[] generate(int size) {
        double[] window = new double[size];
        switch (this) {
            case RECTANGULAR:
                for (int i = 0; i < size; i++) window[i] = 1.0;
                break;
            case HANN:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (size - 1)));
                break;
            case HAMMING:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (size - 1));
                break;
            case BLACKMAN:
                for (int i = 0; i < size; i++) 
                    window[i] = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1)) 
                                + 0.08 * Math.cos(4 * Math.PI * i / (size - 1));
                break;
            case BLACKMAN_HARRIS:
                for (int i = 0; i < size; i++)
                    window[i] = 0.35875 - 0.48829 * Math.cos(2 * Math.PI * i / (size - 1))
                                + 0.14128 * Math.cos(4 * Math.PI * i / (size - 1))
                                - 0.01168 * Math.cos(6 * Math.PI * i / (size - 1));
                break;
            case FLAT_TOP:
                for (int i = 0; i < size; i++)
                    window[i] = 1 - 1.93 * Math.cos(2 * Math.PI * i / (size - 1))
                                + 1.29 * Math.cos(4 * Math.PI * i / (size - 1))
                                - 0.388 * Math.cos(6 * Math.PI * i / (size - 1))
                                + 0.032 * Math.cos(8 * Math.PI * i / (size - 1));
                break;
            case TRIANGULAR:
                for (int i = 0; i < size; i++) 
                    window[i] = 1 - Math.abs((i - (size - 1) / 2.0) / ((size - 1) / 2.0));
                break;
            case WELCH:
                for (int i = 0; i < size; i++) 
                    window[i] = 1 - Math.pow((i - (size - 1) / 2.0) / ((size - 1) / 2.0), 2);
                break;
            case COSINE:
                for (int i = 0; i < size; i++) 
                    window[i] = Math.sin(Math.PI * i / (size - 1));
                break;
        }
        return window;
    }
}
