package org.theko.sound.envelope;

import org.theko.sound.control.FloatController;

public class ASDREnvelope extends ASREnvelope {
    protected final FloatController decay;

    public ASDREnvelope(float attack, float sustain, float decay, float release) {
        super(attack, sustain, release);
        this.decay = new FloatController("Decay", 0, 10, decay);
    }

    public FloatController getDecay() {
        return decay;
    }
}
