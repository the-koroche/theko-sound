package org.theko.sound.event;

import org.theko.sound.AudioLine;

/**
 * Represents an event related to an {@link AudioLine}.
 * This class encapsulates an {@link AudioLine} instance and provides
 * access to it through a getter method.
 * 
 * @since v1.4.1
 * @author Theko
 */
public class AudioLineEvent {

    private final AudioLine audioLine;
    
    public AudioLineEvent (AudioLine audioLine) {
        this.audioLine = audioLine;
    }

    public AudioLine getAudioLine () {
        return audioLine;
    }
}
