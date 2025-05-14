package org.theko.sound;

import java.io.IOException;

/**
 * This class represents a custom exception that extends {@link IOException}.
 * It is used to indicate issues specific to audio object operations.
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
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
