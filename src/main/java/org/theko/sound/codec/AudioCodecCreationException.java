package org.theko.sound.codec;

public class AudioCodecCreationException extends AudioCodecException {
    public AudioCodecCreationException () {
        super();
    }

    public AudioCodecCreationException (String message) {
        super(message);
    }

    public AudioCodecCreationException (Throwable cause) {
        super(cause);
    }

    public AudioCodecCreationException (String message, Throwable cause) {
        super(message, cause);
    }
}
