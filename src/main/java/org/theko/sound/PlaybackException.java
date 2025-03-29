package org.theko.sound;

public class PlaybackException extends RuntimeException {
    public PlaybackException () {
        super();
    }

    public PlaybackException (String message) {
        super(message);
    }

    public PlaybackException (Throwable cause) {
        super(cause);
    }

    public PlaybackException (String message, Throwable cause) {
        super(message, cause);
    }
}
