package org.theko.sound;

/**
 * This exception is thrown to indicate that an unsupported audio format
 * has been encountered during audio processing.
 * 
 * <p>It provides constructors to specify an error message, a cause, or both,
 * allowing for detailed exception handling and debugging.</p>
 * 
 * <p>Usage examples include scenarios where an audio file format is not
 * recognized or supported by the application.</p>
 * 
 * @author Alex Soloviov
 */
public class UnsupportedAudioFormatException extends Exception {
    public UnsupportedAudioFormatException () {
        super();
    }

    public UnsupportedAudioFormatException (String message) {
        super(message);
    }

    public UnsupportedAudioFormatException (Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioFormatException (String message, Throwable cause) {
        super(message, cause);
    }
}
