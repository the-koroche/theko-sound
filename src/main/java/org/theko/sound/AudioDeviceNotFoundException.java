package org.theko.sound;

/**
 * This exception is thrown to indicate that an audio device could not be found.
 * It extends the {@link Exception} class and provides constructors for various use cases.
 * 
 * <p>Possible scenarios where this exception might be used include:
 * <ul>
 *   <li>When attempting to access an audio device that is not available.</li>
 *   <li>When the system fails to detect a required audio device.</li>
 * </ul>
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioDeviceNotFoundException extends Exception {
    public AudioDeviceNotFoundException () {
        super();
    }

    public AudioDeviceNotFoundException (String message) {
        super(message);
    }

    public AudioDeviceNotFoundException (Throwable cause) {
        super(cause);
    }

    public AudioDeviceNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
