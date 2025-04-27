package org.theko.sound;

import java.io.IOException;

public class AudioObjectIOException extends IOException {
    public AudioObjectIOException () {
        super();
    }

    public AudioObjectIOException (String message) {
        super(message);
    }

    public AudioObjectIOException (Throwable cause) {
        super(cause);
    }

    public AudioObjectIOException (String message, Throwable cause) {
        super(message, cause);
    }
}
