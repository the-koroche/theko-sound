package org.theko.sound;

/**
 * This exception is thrown when there is a mismatch in the expected length of an audio sample array
 * and the actual length provided. It extends the Exception class to provide specific context for
 * length mismatches in audio processing operations.
 * 
 * @see ChannelsCountMismatchException
 * 
 * @since v2.0.0
 * @author Theko
 */
public class LengthMismatchException extends Exception {
    
    public LengthMismatchException () {
        super();
    }

    public LengthMismatchException (String message) {
        super(message);
    }

    public LengthMismatchException (Throwable cause) {
        super(cause);
    }

    public LengthMismatchException (String message, Throwable cause) {
        super(message, cause);
    }
}