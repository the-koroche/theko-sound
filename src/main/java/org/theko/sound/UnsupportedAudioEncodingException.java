package org.theko.sound;

/**
 * This exception is thrown to indicate that an unsupported audio encoding
 * format has been encountered.
 * 
 * <p>
 * The {@code UnsupportedAudioEncodingException} class extends the
 * {@code Exception} class and provides constructors to create an exception
 * instance with a custom message, a cause, or both.
 * </p>
 * 
 * <p>
 * This exception can be used in scenarios where audio processing or decoding
 * encounters an encoding format that is not supported by the application.
 * </p>
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class UnsupportedAudioEncodingException extends Exception {
    public UnsupportedAudioEncodingException () {
        super();
    }

    public UnsupportedAudioEncodingException (String message) {
        super(message);
    }

    public UnsupportedAudioEncodingException (Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioEncodingException (String message, Throwable cause) {
        super(message, cause);
    }
}
