package org.theko.sound.direct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.AudioFormat.Encoding;

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
     * Retrieves the audio format associated with the given audio port.
     *
     * This method generates a list of audio formats based on common sample rates, bit depths,
     * channel configurations, and encodings. It then checks each format against the specified
     * audio port to determine compatibility. The first compatible format found is returned.
     *
     * @param port the {@link AudioPort} whose format is requested.
     * @return the {@link AudioFormat} for the given port, or {@code null} if no compatible format is found.
     */
    default AudioFormat getFormatForPort(AudioPort port) {
        // Define common sample rates in Hz
        int[] sampleRates = {48000, 44100, 22500, 8000};
        
        // Define common bit depths in bits per sample
        int[] bitDepths = {32, 24, 16, 8};
        
        // Define common channel configurations (stereo or mono)
        int[] channels = {2, 1};
        
        // Define supported audio encodings
        Encoding[] encodings = {Encoding.PCM_FLOAT, Encoding.PCM_SIGNED, Encoding.PCM_UNSIGNED};

        // Create a list to store generated audio formats
        List<AudioFormat> sortedFormats = new ArrayList<>();
        
        // Generate combinations of audio format properties
        for (int ch : channels) {
            for (int bits : bitDepths) {
                for (int rate : sampleRates) {
                    for (Encoding enc : encodings) {
                        // Skip invalid combinations:
                        // 1. 8-bit FLOAT or stereo SIGNED PCM is not valid
                        // 2. FLOAT encoding must be 32-bit
                        if (bits == 8 && (enc == Encoding.PCM_FLOAT || (enc == Encoding.PCM_SIGNED && ch == 2))) continue;
                        if (enc == Encoding.PCM_FLOAT && bits != 32) continue;

                        // Add the valid audio format to the list
                        sortedFormats.add(new AudioFormat(rate, bits, ch, enc, false));
                    }
                }
            }
        }

        // Iterate over generated formats to find a compatible one
        for (AudioFormat current : sortedFormats) {
            // Check if the current format is supported by the port
            if (isPortSupporting(port, current)) {
                return current; // Return the first compatible format found
            }
        }

        // Return null if no compatible format is found
        return null;
    }

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
