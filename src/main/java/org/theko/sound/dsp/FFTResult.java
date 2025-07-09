package org.theko.sound.dsp;

public class FFTResult {
    public final float frequency;
    public final float amplitude;
    public final float phase;

    public FFTResult(float frequency, float amplitude, float phase) {
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.phase = phase;
    }

    @Override
    public String toString() {
        return "FFTResult{" +
                "frequency=" + frequency +
                ", amplitude=" + amplitude +
                ", phase=" + phase +
                '}';
    }
}
