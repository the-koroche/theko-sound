package org.theko.sound;

/**
 * This exception is thrown to indicate that an error occurred during the creation
 * of an audio device.
 * 
 * <p>
 * The {@code AudioDeviceCreationException} class provides constructors to specify
 * an error message, a cause, or both. It extends the {@code Exception} class,
 * making it a checked exception that must be declared in a method or constructor's
 * {@code throws} clause if it can be thrown during execution.
 * </p>
 * 
 * <p>
 * Usage examples include handling errors when initializing or configuring audio
 * devices in an application.
 * </p>
 *
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioDeviceCreationException extends Exception {
    public AudioDeviceCreationException () {
        super();
    }

    public AudioDeviceCreationException (String message) {
        super(message);
    }

    public AudioDeviceCreationException (Throwable cause) {
        super(cause);
    }

    public AudioDeviceCreationException (String message, Throwable cause) {
        super(message, cause);
    }
}
