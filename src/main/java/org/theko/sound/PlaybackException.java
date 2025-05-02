package org.theko.sound;

/**
 * PlaybackException is a custom unchecked exception that represents errors
 * occurring during audio playback operations.
 * 
 * <p>This exception extends {@link RuntimeException}, allowing it to be thrown
 * without being explicitly declared in method signatures. It provides multiple
 * constructors to support different use cases, such as specifying an error
 * message, a cause, or both.</p>
 * 
 * <p>Usage examples:</p>
 * <ul>
 *   <li>Throwing with a custom message: {@code throw new PlaybackException("Playback failed");}</li>
 *   <li>Wrapping another exception: {@code throw new PlaybackException(e);}</li>
 *   <li>Providing both a message and a cause: {@code throw new PlaybackException("Playback failed", e);}</li>
 * </ul>
 * 
 * @author Alex Soloviov
 */
public class PlaybackException extends RuntimeException {
    public PlaybackException () {
        super();
    }

    public PlaybackException (String message) {
        super(message);
    }

    public PlaybackException (Throwable cause) {
        super(cause);
    }

    public PlaybackException (String message, Throwable cause) {
        super(message, cause);
    }
}
