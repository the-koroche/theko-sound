package org.theko.sound.codec;

public class AudioCodecException extends Exception {
    public AudioCodecException () {
        super();
    }

    public AudioCodecException (String message) {
        super(message);
    }

    public AudioCodecException (Throwable cause) {
        super(cause);
    }

    public AudioCodecException (String message, Throwable cause) {
        super(message, cause);
    }
}
