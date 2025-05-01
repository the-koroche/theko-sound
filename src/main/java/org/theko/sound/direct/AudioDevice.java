package org.theko.sound.direct;

import java.util.Collection;
import java.util.Optional;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;

/**
 * The {@code AudioDevice} interface represents a generic audio device that can handle
 * audio input and output operations. It provides methods for initializing the device,
 * retrieving available audio ports, and checking port compatibility with specific audio formats.
 * 
 * <p>This interface is intended to be implemented by classes that interact with 
 * software audio devices, providing a standardized way to manage audio input and output.</p>
 * 
 * <p>Implementations of this interface should define the behavior for interacting with
 * audio input and output devices, as well as managing audio ports.</p>
 * 
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Initialization of the audio device.</li>
 *   <li>Retrieval of all available audio ports.</li>
 *   <li>Filtering ports based on audio flow and format compatibility.</li>
 *   <li>Determining the default port for specific audio flows and formats.</li>
 *   <li>Access to input and output device representations.</li>
 * </ul>
 * 
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 * @see AudioInputDevice
 * @see AudioOutputDevice
 * @see AudioDeviceException
 * @see AudioPortsNotFoundException
 * @see UnsupportedAudioFormatException
 * 
 * @author Alex Soloviov
 */
public interface AudioDevice {
    /**
     * Initializes the audio device. This default implementation does nothing,
     * and it is expected that implementers override this method if initialization
     * logic is required.
     *
     * @throws AudioDeviceException if an error occurs during initialization.
     */
    default void initialize() throws AudioDeviceException {
        // No default initialization logic provided.
    }

    /**
     * Retrieves all ports associated with this audio device, regardless of
     * availability or compatibility.
     *
     * @return a collection of all {@link AudioPort}s available on the device.
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
     * @throws UnsupportedAudioFormatException if the format is not supported by the device or ports.
     */
    Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat)
        throws AudioPortsNotFoundException, UnsupportedAudioFormatException;

    /**
     * Checks if a specific audio port supports the provided audio format.
     *
     * @param port the {@link AudioPort} to check.
     * @param audioFormat the audio format to test compatibility for.
     * @return {@code true} if the port supports the specified format; {@code false} otherwise.
     */
    boolean isPortSupporting(AudioPort port, AudioFormat audioFormat);

    /**
     * Retrieves the default audio port for the specified audio flow and format.
     *
     * @param flow the desired audio flow (input or output).
     * @param audioFormat the desired audio format.
     * @return an {@link Optional} containing the default port if found; empty otherwise.
     * @throws AudioPortsNotFoundException if no matching ports are available.
     * @throws UnsupportedAudioFormatException if the given format is not supported.
     */
    Optional<AudioPort> getDefaultPort(AudioFlow flow, AudioFormat audioFormat)
        throws AudioPortsNotFoundException, UnsupportedAudioFormatException;

    /**
     * Returns the input device associated with this audio device, allowing
     * for access to input-specific controls or metadata.
     *
     * @return an {@link AudioInputDevice} representing the input side of this device.
     */
    AudioInputDevice getInputDevice();

    /**
     * Returns the output device associated with this audio device, allowing
     * for access to output-specific controls or metadata.
     *
     * @return an {@link AudioOutputDevice} representing the output side of this device.
     */
    AudioOutputDevice getOutputDevice();
}
