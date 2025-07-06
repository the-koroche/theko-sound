package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.FloatControl;
import org.theko.sound.control.AudioControl;

public class BitcrusherEffect extends AudioEffect {
    protected final FloatControl bitdepth = new FloatControl("Bit Depth", 1, 16, 4);
    protected final FloatControl sampleRateReduction = new FloatControl("Sample Rate Reduction", 50f, 22000f, 2000f);

    protected final List<AudioControl> bitcrusherControls = List.of(
        bitdepth,
        sampleRateReduction
    );

    public BitcrusherEffect() {
        super(Type.REALTIME);

        allControls.addAll(bitcrusherControls);
    }

    public FloatControl getBitdepth() {
        return bitdepth;
    }

    public FloatControl getSampleRateReduction() {
        return sampleRateReduction;
    }

    @Override
    public void render(float[][] samples, int sampleRate, int length) {
        int channels = samples.length;

        float targetRate = sampleRateReduction.getValue();
        float sampleStep = sampleRate / targetRate;

        int bitDepth = (int) bitdepth.getValue(); // from 1 to 16
        int levels = (1 << bitDepth) - 1;

        for (int ch = 0; ch < channels; ch++) {
            float[] channel = samples[ch];

            float heldSample = 0.0f;
            float sampleCounter = 0.0f;

            for (int i = 0; i < length; i++) {
                if (sampleCounter <= 0.0f) {
                    heldSample = Math.round(channel[i] * levels) / (float) levels;
                    sampleCounter += sampleStep;
                }

                channel[i] = heldSample;
                sampleCounter -= 1.0f;
            }
        }
    }
}
