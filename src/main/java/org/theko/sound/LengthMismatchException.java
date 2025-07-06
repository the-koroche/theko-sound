package org.theko.sound;

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