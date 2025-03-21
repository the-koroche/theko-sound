package org.theko.sound;

public class UnsupportedAudioFormatException extends Exception {
    public UnsupportedAudioFormatException () {
        super();
    }

    public UnsupportedAudioFormatException (String message) {
        super(message);
    }

    public UnsupportedAudioFormatException (Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioFormatException (String message, Throwable cause) {
        super(message, cause);
    }
}
