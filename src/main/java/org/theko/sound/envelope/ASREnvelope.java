package org.theko.sound.envelope;

import org.theko.sound.control.FloatControl;

/**
 * The ASREnvelope class represents an Attack-Sustain-Release envelope.
 * It allows control over the attack, 
 * sustain, and release phases of a sound envelope.
 * 
 * <p>This class implements the {@link Controllable} interface, enabling 
 * external control of its parameters.</p>
 * 
 * <p>The attack, sustain, and release parameters are represented as {@link FloatControl} 
 * objects, which provide a range of values and a descriptive name for each 
 * parameter.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 *     ASREnvelope envelope = new ASREnvelope(0.5f, 0,75f, 1.0f);
 *     FloatControl attackControl = envelope.getAttack();
 *     FloatControl sustainControl = envelope.getSustain();
 *     FloatControl releaseControl = envelope.getRelease();
 * </pre>
 * 
 * @see org.theko.sound.control.FloatControl
 * @see org.theko.sound.envelope.AREnvelope
 * @see org.theko.sound.envelope.ASDREnvelope
 * 
  * @since v1.4.1
* 
* @author Theko
 */
public class ASREnvelope extends AREnvelope {
    protected final FloatControl sustain;

    public ASREnvelope(float attack, float sustain, float release) {
        super(attack, release);
        this.sustain = new FloatControl("Sustaiun", 0, 10, sustain);
    }

    /**
     * Returns the sustain phase of the envelope as a FloatControl.
     * 
     * @return A FloatControl representing the sustain phase of the envelope.
     */
    public FloatControl getSustain() {
        return sustain;
    }
}
