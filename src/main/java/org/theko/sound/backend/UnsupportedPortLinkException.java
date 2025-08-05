package org.theko.sound.backend;

/**
 * This exception is thrown to indicate that an audio port link type is not supported by the audio backend.
 * It extends the {@link RuntimeException} class and provides constructors for various use cases.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class UnsupportedPortLinkException extends RuntimeException {

    public UnsupportedPortLinkException() {
        super();
    }

    public UnsupportedPortLinkException(String message) {
        super(message);
    }

    public UnsupportedPortLinkException(Throwable cause) {
        super(cause);
    }

    public UnsupportedPortLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
