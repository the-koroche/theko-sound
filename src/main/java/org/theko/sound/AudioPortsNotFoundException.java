package org.theko.sound;

/**
 * Exception thrown to indicate that the required audio ports could not be found.
 * This exception can be used to signal issues related to missing or unavailable
 * audio ports in the system.
 * 
 * @since v1.0.0
 * @author Theko
 */
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
