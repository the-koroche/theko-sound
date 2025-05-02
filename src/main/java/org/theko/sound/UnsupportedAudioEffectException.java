package org.theko.sound;

/**
 * This exception is thrown to indicate that a requested audio effect is not supported.
 * 
 * @author Alex Soloviov
 */
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
