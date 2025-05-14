package org.theko.sound;

/**
 * Exception thrown to indicate that the required audio ports could not be found.
 * This exception can be used to signal issues related to missing or unavailable
 * audio ports in the system.
 *
 * <p>There are multiple constructors available to provide flexibility in specifying
 * the exception message and/or the underlying cause of the exception.</p>
 *
 * <ul>
 *   <li>{@link #AudioPortsNotFoundException()} - Constructs a new exception with no detail message or cause.</li>
 *   <li>{@link #AudioPortsNotFoundException(String)} - Constructs a new exception with the specified detail message.</li>
 *   <li>{@link #AudioPortsNotFoundException(Throwable)} - Constructs a new exception with the specified cause.</li>
 *   <li>{@link #AudioPortsNotFoundException(String, Throwable)} - Constructs a new exception with the specified detail message and cause.</li>
 * </ul>
 * 
 * @since v1.4.1
 * 
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
