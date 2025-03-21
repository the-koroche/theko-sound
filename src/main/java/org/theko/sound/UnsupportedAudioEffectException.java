package org.theko.sound;

public class UnsupportedAudioEffectException extends Exception {
    public UnsupportedAudioEffectException () {
        super();
    }

    public UnsupportedAudioEffectException (String message) {
        super(message);
    }

    public UnsupportedAudioEffectException (Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioEffectException (String message, Throwable cause) {
        super(message, cause);
    }
}
