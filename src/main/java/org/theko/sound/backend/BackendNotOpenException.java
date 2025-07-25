package org.theko.sound.backend;

/**
 * This exception is thrown to indicate that an audio backend is not open.<br>
 * It extends the {@link AudioBackendException} class and provides constructors for various use cases.<br>
 * 
 * @since v2.2.0
 * @author Theko
 */
public class BackendNotOpenException extends AudioBackendException {
    public BackendNotOpenException (String message) {
        super(message);
    }

    public BackendNotOpenException (Throwable cause) {
        super(cause);
    }

    public BackendNotOpenException (String message, Throwable cause) {
        super(message, cause);
    }

    public BackendNotOpenException () {
        super();
    }
}
