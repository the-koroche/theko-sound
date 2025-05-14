package org.theko.sound.codec;

/**
 * This exception is thrown when an audio codec cannot be found.
 * It extends {@link AudioCodecException} to provide more specific
 * error handling for codec-related issues.
 * 
 * <p>Possible use cases include scenarios where the application
 * attempts to process audio data with a codec that is unavailable
 * or unsupported.</p>
 * 
 * @since v1.3.1
 * 
 * @author Theko
 */
public class AudioCodecNotFoundException extends AudioCodecException {
    public AudioCodecNotFoundException () {
        super();
    }

    public AudioCodecNotFoundException (String message) {
        super(message);
    }

    public AudioCodecNotFoundException (Throwable cause) {
        super(cause);
    }

    public AudioCodecNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
