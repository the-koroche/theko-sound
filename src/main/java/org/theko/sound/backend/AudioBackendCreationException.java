package org.theko.sound.backend;

/**
 * This exception is thrown to indicate that an error occurred during the creation
 * of an audio backend.
 * 
 * <p>
 * The {@code AudioBackendCreationException} class provides constructors to specify
 * an error message, a cause, or both. It extends the {@code Exception} class,
 * making it a checked exception that must be declared in a method or constructor's
 * {@code throws} clause if it can be thrown during execution.
 * </p>
 * 
 * <p>
 * Usage examples include handling errors when initializing or configuring audio
 * backends in an application.
 * </p>
 *
 * @since v1.0.0
 * @author Theko
 */
public class AudioBackendCreationException extends Exception {
    
    public AudioBackendCreationException () {
        super();
    }

    public AudioBackendCreationException (String message) {
        super(message);
    }

    public AudioBackendCreationException (Throwable cause) {
        super(cause);
    }

    public AudioBackendCreationException (String message, Throwable cause) {
        super(message, cause);
    }
}
