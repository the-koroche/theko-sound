package org.theko.sound;

/**
 * This exception is thrown when there is a mismatch in the number of channels
 * expected and the number of channels provided in an audio processing operation.
 * It extends the Exception class to provide specific context for channel count mismatches.
 * 
 * @see LengthMismatchException
 * 
 * @since v2.0.0
 * @author Theko
 */
public class ChannelsCountMismatchException extends Exception {
    
    public ChannelsCountMismatchException () {
        super();
    }

    public ChannelsCountMismatchException (String message) {
        super(message);
    }

    public ChannelsCountMismatchException (Throwable cause) {
        super(cause);
    }

    public ChannelsCountMismatchException (String message, Throwable cause) {
        super(message, cause);
    }
}