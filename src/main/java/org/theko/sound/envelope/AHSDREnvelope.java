package org.theko.sound.envelope;

import org.theko.sound.control.FloatControl;

/**
 * The AHSDREnvelope class represents an Attack-Hold-Sustain-Decay-Release envelope.
 * It allows control over the attack, hold, 
 * sustain, decay, and release phases of a sound envelope.
 * 
 * <p>This class implements the {@link Controllable} interface, enabling 
 * external control of its parameters.</p>
 * 
 * <p>The attack, hold, sustain, decay and release parameters are represented as {@link FloatControl} 
 * objects, which provide a range of values and a descriptive name for each 
 * parameter.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 *     ASDREnvelope envelope = new ASDREnvelope(0.5f, 0.8f, 0,75f, 0.3f 1.0f);
 *     FloatControl attackControl = envelope.getAttack();
 *     FloatControl holdControl = envelope.getHold();
 *     FloatControl sustainControl = envelope.getSustain();
 *     FloatControl decayControl = envelope.getDecay();
 *     FloatControl releaseControl = envelope.getRelease();
 * </pre>
 * 
 * @see org.theko.sound.control.FloatControl
 * @see org.theko.sound.envelope.ASDREnvelope
 * @see org.theko.sound.envelope.AREnvelope
 * 
  * @since v1.4.1
* 
* @author Theko
 */
public class AHSDREnvelope extends ASDREnvelope {
    protected final FloatControl hold;

    public AHSDREnvelope(float attack, float hold, float sustain, float decay, float release) {
        super(attack, sustain, decay, release);
        this.hold = new FloatControl("Hold", -1, 10, hold); // -1 endless hold
    }

    public FloatControl getHold() {
        return hold;
    }
}
