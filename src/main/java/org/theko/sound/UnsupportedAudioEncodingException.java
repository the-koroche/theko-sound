package org.theko.sound;

public class UnsupportedAudioEncodingException extends Exception {
    public UnsupportedAudioEncodingException () {
        super();
    }

    public UnsupportedAudioEncodingException (String message) {
        super(message);
    }

    public UnsupportedAudioEncodingException (Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioEncodingException (String message, Throwable cause) {
        super(message, cause);
    }
}
