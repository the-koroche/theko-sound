package org.theko.sound;

/**
 * MixingException is a custom unchecked exception that is thrown to indicate
 * issues or errors encountered during the mixing process in the application.
 * 
 * <p>This exception extends {@link RuntimeException}, allowing it to be used
 * without requiring explicit handling in the code. It provides multiple
 * constructors to support different use cases, such as specifying an error
 * message, a cause, or both.
 * 
 * <p>Usage examples:
 * <pre>
 * throw new MixingException("An error occurred during mixing");
 * throw new MixingException(new IOException("File not found"));
 * throw new MixingException("Error with file", new IOException("File not found"));
 * </pre>
 * 
 * @see RuntimeException
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class MixingException extends RuntimeException {
    public MixingException () {
        super();
    }

    public MixingException (String message) {
        super(message);
    }

    public MixingException (Throwable cause) {
        super(cause);
    }

    public MixingException (String message, Throwable cause) {
        super(message, cause);
    }
}
