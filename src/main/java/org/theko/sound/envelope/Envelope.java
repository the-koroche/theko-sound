/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.envelope;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.utility.MathUtilities;

/**
 * Represents an envelope generator used to shape the amplitude of an audio signal over time.
 * It provides configurable attack, decay, sustain, and release stages, along with tension controls
 * for the attack and decay curves.
 *
 * <p>Attack, hold, decay, and release times are specified in seconds. Sustain defines a steady-state
 * level between 0.0 and 1.0. Curve tensions range from -1.0 to 1.0: negative values create concave
 * curves, positive values create convex curves, and 0.0 results in a linear transition.
 *
 * @since 2.4.1
 * @author Theko
 */
public class Envelope implements Controllable {

    protected final FloatControl attack = new FloatControl("Attack", 0.00f, 10.0f, 0.25f);
    protected final FloatControl attackTension = new FloatControl("Attack Tension", -1.0f, 1.0f, 0.0f);

    protected final FloatControl hold = new FloatControl("Hold", 0.00f, 10.0f, 1.0f);

    protected final FloatControl decay = new FloatControl("Decay", 0.00f, 10.0f, 0.1f);
    protected final FloatControl decayTension = new FloatControl("Decay Tension", -1.0f, 1.0f, 0.0f);

    protected final FloatControl sustain = new FloatControl("Sustain", 0.0f, 1.0f, 0.75f);

    protected final FloatControl release = new FloatControl("Release", 0.00f, 10.0f, 0.1f);
    protected final FloatControl releaseTension = new FloatControl("Release Tension", -1.0f, 1.0f, 0.0f);

    protected final List<AudioControl> envelopeControls = List.of(
        attack, attackTension,
        hold,
        decay, decayTension,
        sustain,
        release, releaseTension
    );

    /**
     * Creates an envelope with the specified parameters.
     * @param attack the attack time in seconds
     * @param attackTension the attack tension (-1.0 to 1.0)
     * @param hold the hold time in seconds
     * @param decay the decay time in seconds
     * @param decayTension the decay tension (-1.0 to 1.0)
     * @param sustain the sustain level (0.0 to 1.0)
     * @param release the release time in seconds
     * @param releaseTension the release tension (-1.0 to 1.0)
     */
    public Envelope(float attack, float attackTension, float hold, float decay, float decayTension, float sustain, float release, float releaseTension) {
        this.attack.setValue(attack);
        this.attackTension.setValue(attackTension);
        this.hold.setValue(hold);
        this.decay.setValue(decay);
        this.decayTension.setValue(decayTension);
        this.sustain.setValue(sustain);
        this.release.setValue(release);
        this.releaseTension.setValue(releaseTension);
    }

    /**
     * Creates an envelope with the specified parameters, using default tensions of 0.0.
     * @param attack the attack time in seconds
     * @param hold the hold time in seconds
     * @param decay the decay time in seconds
     * @param sustain the sustain level (0.0 to 1.0)
     * @param release the release time in seconds
     */
    public Envelope(float attack, float hold, float decay, float sustain, float release) {
        this(attack, 0.0f, hold, decay, 0.0f, sustain, release, 0.0f);
    }

    /**
     * Creates an envelope with default parameters.
     */
    public Envelope() { }

    /** @return the attack control */
    public FloatControl getAttack() { return attack; }

    /** @return the attack tension control */
    public FloatControl getAttackTension() { return attackTension; }

    /** @return the hold control */
    public FloatControl getHold() { return hold; }

    /** @return the decay control */
    public FloatControl getDecay() { return decay; }

    /** @return the decay tension control */
    public FloatControl getDecayTension() { return decayTension; }

    /** @return the sustain control */
    public FloatControl getSustain() { return sustain; }

    /** @return the release control */
    public FloatControl getRelease() { return release; }

    /** @return the release tension control */
    public FloatControl getReleaseTension() { return releaseTension; }

    
    /**
     * Calculates the envelope value at a given time.
     * 
     * <p>This method applies the envelope's attack, hold, decay, sustain, and release stages
     * to calculate the envelope value at the given time.
     * 
     * @param time the time at which to calculate the envelope value
     * @return the envelope value at the given time (0.0 to 1.0)
     */
    public float getValue(float time) {
        if (time < 0f)
            return 0f;

        float A = attack.getValue();
        float H = hold.getValue();
        float D = decay.getValue();
        float S = sustain.getValue();
        float R = release.getValue();

        float TA = attackTension.getValue();
        float TD = decayTension.getValue();
        float TR = releaseTension.getValue();

        float powA = MathUtilities.mapTension(TA);
        float powD = MathUtilities.mapTension(TD);
        float powR = MathUtilities.mapTension(TR);

        float t = time;

        // Attack
        if (t < A) {
            if (A <= 0f) return 1f;
            float x = t / A;
            return (float)Math.pow(x, powA);
        }

        // Hold
        t -= A;
        if (t < H) {
            return 1f;
        }

        // Decay
        t -= H;
        if (t < D) {
            if (D <= 0f) return S;
            float x = t / D;
            return S + (1f - S) * (1f - (float)Math.pow(x, powD));
        }

        // Sustain
        t -= D;
        if (t < 0) {
            return S;
        }

        // Release
        if (t < R) {
            if (R <= 0f) return 0f;
            float x = t / R;
            return S * (1f - (float)Math.pow(x, powR));
        }

        return 0f;
    }

    @Override
    public List<AudioControl> getAllControls() {
        return envelopeControls;
    }

    @Override
    public String toString() {
        return "Envelope [attack=" + attack + ", attackTension=" + attackTension + ", hold=" + hold + ", decay="
                + decay + ", decayTension=" + decayTension + ", sustain=" + sustain + ", release=" + release
                + ", releaseTension=" + releaseTension + "]";
    }
}
