package org.theko.sound.envelope;

import org.theko.sound.control.FloatControl;

/**
 * The ASDREnvelope class represents an Attack-Sustain-Decay-Release envelope.
 * It allows control over the attack, 
 * sustain, decay, and release phases of a sound envelope.
 * 
 * <p>This class implements the {@link Controllable} interface, enabling 
 * external control of its parameters.</p>
 * 
 * <p>The attack, sustain, decay and release parameters are represented as {@link FloatControl} 
 * objects, which provide a range of values and a descriptive name for each 
 * parameter.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 *     ASDREnvelope envelope = new ASDREnvelope(0.5f, 0,75f, 0.3f 1.0f);
 *     FloatControl attackControl = envelope.getAttack();
 *     FloatControl sustainControl = envelope.getSustain();
 *     FloatControl decayControl = envelope.getDecay();
 *     FloatControl releaseControl = envelope.getRelease();
 * </pre>
 * 
 * @see org.theko.sound.control.FloatControl
 * @see org.theko.sound.envelope.ASREnvelope
 * @see org.theko.sound.envelope.AHSDREnvelope
 * 
 * @since v1.4.1
 * @author Theko
 */
public class ASDREnvelope extends ASREnvelope {

    protected final FloatControl decay;

    public ASDREnvelope (float attack, float sustain, float decay, float release) {
        super(attack, sustain, release);
        this.decay = new FloatControl("Decay", 0, 10, decay);
    }

    public FloatControl getDecay () {
        return decay;
    }
}
