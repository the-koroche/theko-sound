package org.theko.sound.codec;

/**
 * Represents an exception that occurs during audio codec operations.
 * This exception can be used to indicate various issues related to
 * encoding, decoding, or processing audio data.
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public class AudioCodecException extends Exception {
    
    public AudioCodecException () {
        super();
    }

    public AudioCodecException (String message) {
        super(message);
    }

    public AudioCodecException (Throwable cause) {
        super(cause);
    }

    public AudioCodecException (String message, Throwable cause) {
        super(message, cause);
    }
}
