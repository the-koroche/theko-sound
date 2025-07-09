package org.theko.sound.backend;

/**
 * This exception is thrown to indicate that an audio backend could not be found.
 * It extends the {@link Exception} class and provides constructors for various use cases.
 * 
 * <p>Possible scenarios where this exception might be used include:
 * <ul>
 *   <li>When attempting to access an audio backend that is not available.</li>
 *   <li>When the system fails to detect a required audio backend.</li>
 * </ul>
 * 
 * @since v1.0.0
 * @author Theko
 */
public class AudioBackendNotFoundException extends Exception {
    
    public AudioBackendNotFoundException () {
        super();
    }

    public AudioBackendNotFoundException (String message) {
        super(message);
    }

    public AudioBackendNotFoundException (Throwable cause) {
        super(cause);
    }

    public AudioBackendNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
