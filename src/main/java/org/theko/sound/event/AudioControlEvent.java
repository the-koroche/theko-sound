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
