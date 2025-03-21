package org.theko.sound;

public class MixingException extends RuntimeException {
    public MixingException () {
        super();
    }

    public MixingException (String message) {
        super(message);
    }

    public MixingException (Throwable cause) {
        super(cause);
    }

    public MixingException (String message, Throwable cause) {
        super(message, cause);
    }
}
