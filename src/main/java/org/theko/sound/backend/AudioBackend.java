package org.theko.sound.backend;

import java.util.Collection;
import java.util.Optional;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;

/**
 * The {@code AudioBackend} interface represents a generic audio backend that can handle
 * audio input and output operations. It provides methods for initializing the backend,
 * retrieving available audio ports, and checking port compatibility with specific audio formats.
 * 
 * <p>This interface is intended to be implemented by classes that interact with 
 * software audio backends, providing a standardized way to manage audio input and output.</p>
 * 
 * <p>Implementations of this interface should define the behavior for interacting with
 * audio input and output backends, as well as managing audio ports.</p>
 * 
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 * @see AudioInputBackend
 * @see AudioOutputBackend
 * @see AudioBackendException
 * @see AudioPortsNotFoundException
 * @see UnsupportedAudioFormatException
 * 
 * @since v1.0.0
 * @author Theko
 */
public interface AudioBackend {

    /**
     * Initializes the audio backend. This default implementation does nothing,
     * and it is expected that implementers override this method if initialization
     * logic is required.
     *
     * @throws AudioBackendException if an error occurs during initialization.
     */
    default void initialize() throws AudioBackendException {
        // No default initialization logic provided.
    }

    /**
     * Shuts down the audio backend and releases any allocated resources.
     * This default implementation does nothing, and it is expected that
     * implementers override this method if shutdown logic is required.
     *
     * @throws AudioBackendException if an error occurs during shutdown.
     */
    default void shutdown() throws AudioBackendException {
        // No default shutdown logic provided.
    }

    /**
     * Retrieves all ports associated with this audio backend, regardless of
     * availability or compatibility.
     *
     * @return a collection of all {@link AudioPort}s available on the backend.
     */
    Collection<AudioPort> getAllPorts();

    /**
     * Returns a collection of audio ports that are compatible with the specified
     * audio flow and audio format.
     *
     * @param flow the direction of the audio flow (input or output).
     * @param audioFormat the desired audio format for compatibility.
     * @return a collection of {@link AudioPort}s matching the given criteria.
     * @throws AudioPortsNotFoundException if no ports are found for the given flow.
     * @throws UnsupportedAudioFormatException if the format is not supported by the backend or ports.
     */
    Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat)
        throws AudioPortsNotFoundException, UnsupportedAudioFormatException;

    /**
     * Returns a collection of audio ports that are compatible with the specified
     * audio flow.
     *
     * @param flow the direction of the audio flow (input or output).
     * @return a collection of {@link AudioPort}s matching the given criteria.
     * @throws AudioPortsNotFoundException if no ports are found for the given flow.
     */
    Collection<AudioPort> getAvailablePorts(AudioFlow flow)
        throws AudioPortsNotFoundException;

    /**
     * Checks if a specific audio port supports the provided audio format.
     *
     * @param port the {@link AudioPort} to check.
     * @param audioFormat the audio format to test compatibility for.
     * @return {@code true} if the port supports the specified format; {@code false} otherwise.
     */
    boolean isFormatSupported(AudioPort port, AudioFormat audioFormat);

    /**
     * Retrieves the default audio port for the specified audio flow.
     *
     * @param flow the desired audio flow (input or output).
     * @return an {@link Optional} containing the default port if found; empty otherwise.
     * @throws AudioPortsNotFoundException if no matching ports are available.
     */
    Optional<AudioPort> getDefaultPort(AudioFlow flow)
        throws AudioPortsNotFoundException;
        
    /**
     * Retrieves the audio port that supports the specified audio flow and audio format.
     *
     * @param flow the desired audio flow (input or output).
     * @param audioFormat the desired audio format.
     * @return an {@link Optional} containing the supporting port if found; empty otherwise.
     * @throws AudioPortsNotFoundException if no matching ports are available.
     * @throws UnsupportedAudioFormatException if the format is not supported by the backend or ports.
     */
    Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat) 
        throws AudioPortsNotFoundException, UnsupportedAudioFormatException;
    
    /**
     * Returns the input backend associated with this audio backend, allowing
     * for access to input-specific controls or metadata.
     *
     * @return an {@link AudioInputBackend} representing the input side of this backend.
     */
    AudioInputBackend getInputBackend();

    /**
     * Returns the output backend associated with this audio backend, allowing
     * for access to output-specific controls or metadata.
     *
     * @return an {@link AudioOutputBackend} representing the output side of this backend.
     */
    AudioOutputBackend getOutputBackend();

    /**
     * Checks if the audio backend is initialized.
     *
     * @return {@code true} if the audio backend is initialized; {@code false} otherwise.
     */
    boolean isInitialized();
}
