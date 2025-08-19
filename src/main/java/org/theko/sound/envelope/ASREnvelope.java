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
