package org.theko.sound;

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