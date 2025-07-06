package org.theko.sound.event;

import org.theko.sound.SoundSource;

public class SoundSourceEvent {
    
    private final SoundSource soundSource;

    public SoundSourceEvent (SoundSource soundSource) {
        this.soundSource = soundSource;
    }

    public SoundSource getSoundSource() {
        return soundSource;
    }
}
