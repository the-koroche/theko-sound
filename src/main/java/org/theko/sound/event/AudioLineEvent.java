package org.theko.sound.event;

import org.theko.sound.AudioLine;

public class AudioLineEvent {
    private AudioLine audioLine;
    
    public AudioLineEvent(AudioLine audioLine) {
        this.audioLine = audioLine;
    }

    public AudioLine getAudioLine() {
        return audioLine;
    }
}
