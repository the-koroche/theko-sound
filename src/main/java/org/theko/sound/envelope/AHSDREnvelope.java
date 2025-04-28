package org.theko.sound.envelope;

import org.theko.sound.control.FloatController;

public class AHSDREnvelope extends ASDREnvelope {
    protected final FloatController hold;

    public AHSDREnvelope(float attack, float hold, float sustain, float decay, float release) {
        super(attack, sustain, decay, release);
        this.hold = new FloatController("Hold", -1, 10, hold); // -1 endless hold
    }

    public FloatController getHold() {
        return hold;
    }
}
