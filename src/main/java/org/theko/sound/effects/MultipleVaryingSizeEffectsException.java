package org.theko.sound.effects;

/**
 * This exception is thrown when multiple effects of varying sizes are added
 * to an audio mixer.
 * <p>It indicates that the effects cannot be processed together due to their
 * differing output sizes.
 * 
 * @since v2.0.0
 * @author Theko
 */
public class MultipleVaryingSizeEffectsException extends Exception {
    
    public MultipleVaryingSizeEffectsException () {
        super();
    }

    public MultipleVaryingSizeEffectsException (String message) {
        super(message);
    }

    public MultipleVaryingSizeEffectsException (Throwable cause) {
        super(cause);
    }

    public MultipleVaryingSizeEffectsException (String message, Throwable cause) {
        super(message, cause);
    }
}