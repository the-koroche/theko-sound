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
 * @since 1.4.1
 * @author Theko
 */
public class ASDREnvelope extends ASREnvelope {

    protected final FloatControl decay;

    public ASDREnvelope(float attack, float sustain, float decay, float release) {
        super(attack, sustain, release);
        this.decay = new FloatControl("Decay", 0, 10, decay);
    }

    public FloatControl getDecay() {
        return decay;
    }
}
