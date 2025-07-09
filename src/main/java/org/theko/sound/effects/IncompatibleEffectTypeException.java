package org.theko.sound.effects;

/**
 * This exception is thrown when an audio effect is incompatible with the expected type.
 * It extends the Exception class to provide a specific context for errors related to
 * audio effects that do not match the required type or format.
 * Usually this exception is used when an effect is offline processing,
 * but it is added to an AudioMixer that expects an real-time effect.
 * 
 * @since v2.0.0
 * @author Theko
 */
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