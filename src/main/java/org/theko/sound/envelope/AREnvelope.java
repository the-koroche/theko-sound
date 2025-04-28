package org.theko.sound.envelope;

import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatController;

public class AREnvelope implements Controllable {
    protected final FloatController attack, release;

    public AREnvelope (float attack, float release) {
        this.attack = new FloatController("Attack", 0, 10, attack);
        this.release = new FloatController("Release", 0, 10, release);
    }

    public FloatController getAttack() {
        return attack;
    }

    public FloatController getRelease() {
        return release;
    }
}
