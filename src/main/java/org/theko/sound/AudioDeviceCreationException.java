package org.theko.sound;

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
