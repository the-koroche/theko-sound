package org.theko.sound.backend;

/**
 * Represents an exception that occurs in the context of audio backend operations.
 * This is a runtime exception that can be used to signal issues related to
 * audio backend functionality.
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioBackendException extends RuntimeException {
    
    public AudioBackendException () {
        super();
    }

    public AudioBackendException (String message) {
        super(message);
    }

    public AudioBackendException (Throwable cause) {
        super(cause);
    }

    public AudioBackendException (String message, Throwable cause) {
        super(message, cause);
    }
}
