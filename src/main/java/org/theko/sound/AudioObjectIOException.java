package org.theko.sound;

import java.io.IOException;

/**
 * This class represents a custom exception that extends {@link IOException}.
 * It is used to indicate issues specific to audio object operations.
 * 
 * <p>AudioObjectIOException provides multiple constructors to allow for
 * detailed exception messages and chaining of underlying causes.</p>
 * 
 * <ul>
 *   <li>{@link #AudioObjectIOException()} - Constructs a new exception with no detail message or cause.</li>
 *   <li>{@link #AudioObjectIOException(String)} - Constructs a new exception with the specified detail message.</li>
 *   <li>{@link #AudioObjectIOException(Throwable)} - Constructs a new exception with the specified cause.</li>
 *   <li>{@link #AudioObjectIOException(String, Throwable)} - Constructs a new exception with the specified detail message and cause.</li>
 * </ul>
 * 
 * @author Alex Soloviov
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
