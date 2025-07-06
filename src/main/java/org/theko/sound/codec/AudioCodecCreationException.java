package org.theko.sound.codec;

/**
 * This exception is thrown to indicate an error during the creation of an audio codec.
 * It extends {@link AudioCodecException} to provide more specific context for codec creation failures.
 * 
 * @since v1.3.1
 * 
 * @author Theko
 */
public class AudioCodecCreationException extends AudioCodecException {
    
    public AudioCodecCreationException () {
        super();
    }

    public AudioCodecCreationException (String message) {
        super(message);
    }

    public AudioCodecCreationException (Throwable cause) {
        super(cause);
    }

    public AudioCodecCreationException (String message, Throwable cause) {
        super(message, cause);
    }
}
