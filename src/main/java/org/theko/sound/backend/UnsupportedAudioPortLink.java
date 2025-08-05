package org.theko.sound.backend;

/**
 * This exception is thrown to indicate that an audio port link type is not supported by the audio backend.
 * It extends the {@link RuntimeException} class and provides constructors for various use cases.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class UnsupportedAudioPortLink extends RuntimeException {

    public UnsupportedAudioPortLink() {
        super();
    }

    public UnsupportedAudioPortLink(String message) {
        super(message);
    }

    public UnsupportedAudioPortLink(Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioPortLink(String message, Throwable cause) {
        super(message, cause);
    }
}
