package org.theko.sound.effects;

public class IncompatibleEffectTypeException extends Exception {
    
    public IncompatibleEffectTypeException () {
        super();
    }

    public IncompatibleEffectTypeException (String message) {
        super(message);
    }

    public IncompatibleEffectTypeException (Throwable cause) {
        super(cause);
    }

    public IncompatibleEffectTypeException (String message, Throwable cause) {
        super(message, cause);
    }
}