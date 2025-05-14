package org.theko.sound.direct;

/**
 * Represents an exception that occurs in the context of audio device operations.
 * This is a runtime exception that can be used to signal issues related to
 * audio device functionality.
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioDeviceException extends RuntimeException {
    public AudioDeviceException () {
        super();
    }

    public AudioDeviceException (String message) {
        super(message);
    }

    public AudioDeviceException (Throwable cause) {
        super(cause);
    }

    public AudioDeviceException (String message, Throwable cause) {
        super(message, cause);
    }
}
