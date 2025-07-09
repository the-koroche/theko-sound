package org.theko.sound;

/**
 * This exception is thrown when there is an error during the mixing of audio samples.
 * It extends RuntimeException to indicate that it is an unchecked exception.
 * 
 * @since v2.0.0
 * @author Theko
 */
public class MixingException extends RuntimeException {
    
    public MixingException () {
        super();
    }

    public MixingException (String message) {
        super(message);
    }

    public MixingException (Throwable cause) {
        super(cause);
    }

    public MixingException (String message, Throwable cause) {
        super(message, cause);
    }
}
