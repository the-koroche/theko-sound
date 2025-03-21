package org.theko.sound;

public class AudioPortsNotFoundException extends Exception {
    public AudioPortsNotFoundException () {
        super();
    }

    public AudioPortsNotFoundException (String message) {
        super(message);
    }

    public AudioPortsNotFoundException (Throwable cause) {
        super(cause);
    }

    public AudioPortsNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
