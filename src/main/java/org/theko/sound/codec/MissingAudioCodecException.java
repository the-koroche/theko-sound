package org.theko.sound.codec;

/**
 * This exception is thrown to indicate that a required audio codec is missing.
 * It extends {@link RuntimeException}, allowing it to be used as an unchecked exception.
 *
 * <p>Possible use cases include scenarios where an application attempts to process
 * audio data but the necessary codec for decoding or encoding is unavailable.</p>
 * 
 * @since v1.3.0
 * @author Theko
 */
public class MissingAudioCodecException extends RuntimeException {
    
    public MissingAudioCodecException () {
        super();
    }

    public MissingAudioCodecException (String message) {
        super(message);
    }

    public MissingAudioCodecException (Throwable cause) {
        super(cause);
    }

    public MissingAudioCodecException (String message, Throwable cause) {
        super(message, cause);
    }
}
