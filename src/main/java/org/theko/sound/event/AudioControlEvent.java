package org.theko.sound.event;

import org.theko.sound.control.AudioControl;

public class AudioControlEvent {
    private final AudioControl audioControl;

    public AudioControlEvent (AudioControl audioControl) {
        this.audioControl = audioControl;
    }

    public AudioControl getAudioControl() {
        return audioControl;
    }
}
