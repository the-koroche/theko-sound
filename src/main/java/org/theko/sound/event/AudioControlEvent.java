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

package org.theko.sound.event;

import org.theko.sound.control.AudioControl;

/**
 * Event for {@link AudioControl} events.
 * It contains the audio control that triggered the event.
 * 
 * @since 2.4.0
 * @author Theko
 */
public class AudioControlEvent extends Event {
    
    private final AudioControl audioControl;

    /**
     * Constructs an {@link AudioControlEvent} with the given audio control.
     * 
     * @param audioControl the audio control
     */
    public AudioControlEvent(AudioControl audioControl) {
        this.audioControl = audioControl;
    }

    /**
     * Returns the audio control that triggered the event.
     * @return The audio control
     */
    public AudioControl getAudioControl() {
        return audioControl;
    }
}
