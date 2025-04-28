package org.theko.sound.envelope;

import org.theko.sound.control.FloatController;

public class ASREnvelope extends AREnvelope {
    protected final FloatController sustain;

    public ASREnvelope(float attack, float sustain, float release) {
        super(attack, release);
        this.sustain = new FloatController("Sustaiun", 0, 10, sustain);
    }

    public FloatController getSustain() {
        return sustain;
    }
}
