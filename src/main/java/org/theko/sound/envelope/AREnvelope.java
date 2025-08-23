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

import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;

/**
 * The AREnvelope class represents an Attack-Release envelope, which is a 
 * common component in sound synthesis. It allows control over the attack 
 * and release phases of a sound envelope.
 * 
 * <p>This class implements the {@link Controllable} interface, enabling 
 * external control of its parameters.</p>
 * 
 * <p>The attack and release parameters are represented as {@link FloatControl} 
 * objects, which provide a range of values and a descriptive name for each 
 * parameter.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 *     AREnvelope envelope = new AREnvelope(0.5f, 1.0f);
 *     FloatControl attackControl = envelope.getAttack();
 *     FloatControl releaseControl = envelope.getRelease();
 * </pre>
 * 
 * @see org.theko.sound.control.FloatControl
 * @see org.theko.sound.control.Controllable
 * @see org.theko.sound.envelope.ASREnvelope
 * 
 * @since 1.4.1
 * @author Theko
 */
public class AREnvelope implements Controllable {

    protected final FloatControl attack, release;

    public AREnvelope (float attack, float release) {
        this.attack = new FloatControl("Attack", 0, 10, attack);
        this.release = new FloatControl("Release", 0, 10, release);
    }

    /**
     * Returns the attack phase of the envelope as a FloatControl.
     * 
     * @return A FloatControl representing the attack phase of the envelope.
     */
    public FloatControl getAttack () {
        return attack;
    }

    /**
     * Returns the release phase of the envelope as a FloatControl.
     * 
     * @return A FloatControl representing the release phase of the envelope.
     */
    public FloatControl getRelease () {
        return release;
    }
}
