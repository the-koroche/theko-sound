package org.theko.sound.codec;

public class MissingAudioCodecException extends RuntimeException {
    public MissingAudioCodecException () {
        super();
    }

    public MissingAudioCodecException (String message) {
        super(message);
    }

    public MissingAudioCodecException (Throwable cause) {
        super(cause);
    }

    public MissingAudioCodecException (String message, Throwable cause) {
        super(message, cause);
    }
}
