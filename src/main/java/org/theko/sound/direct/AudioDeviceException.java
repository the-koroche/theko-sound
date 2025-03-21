package org.theko.sound.direct;

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
