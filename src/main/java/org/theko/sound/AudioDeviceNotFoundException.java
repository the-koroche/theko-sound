package org.theko.sound;

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
