package org.theko.sound;

import org.theko.sound.direct.AudioDeviceException;

/**
 * The {@code AudioLine} interface represents an abstraction for an audio line,
 * providing methods to open, close, and control the audio line, as well as to
 * retrieve information about its state and configuration.
 * <p>
 * Implementations of this interface are expected to handle audio data
 * processing, including buffering, playback, and resource management.
 * </p>
 * <p>
 * The {@code AudioLine} interface extends {@link AutoCloseable}, allowing
 * instances to be used in try-with-resources statements for automatic resource
 * management.
 * </p>
 * <p>
 * Typical usage involves opening the audio line with a specific audio port and
 * format, starting and stopping playback, and querying its state or
 * configuration as needed.
 * </p>
 * <p>
 * Exceptions such as {@link AudioDeviceException},
 * {@link AudioPortsNotFoundException}, and {@link UnsupportedAudioFormatException}
 * are thrown to indicate errors during operations.
 * </p>
 * 
 * @see AudioInputLine
 * @see AudioOutputLine
 * @see org.theko.sound.event.AudioLineListener
 * 
 * @author Alex Soloviov
 */
public interface AudioLine extends AutoCloseable {
    /**
     * Opens the audio line using the specified audio port and format.
     * The audio line is created with the specified buffer size.
     * @param audioPort the audio port to use
     * @param audioFormat the audio format to use
     * @param bufferSize the buffer size to use
     * @throws AudioDeviceException if there's an error opening the audio line
     */
    void open(AudioPort audioPort, AudioFormat audioFormat, int bufferSize);

    /**
     * Opens the audio line using the specified audio port and format.
     * The audio line is created with a default buffer size.
     * @param audioPort the audio port to use
     * @param audioFormat the audio format to use
     * @throws AudioDeviceException if there's an error opening the audio line
     */
    void open(AudioPort audioPort, AudioFormat audioFormat);

    /**
     * Opens the audio line using the specified audio format.
     * The first available audio port that supports the specified format is used.
     * @param audioFormat the audio format to use
     * @throws AudioDeviceException if there's an error opening the audio line
     * @throws AudioPortsNotFoundException if there are no available audio ports
     * @throws UnsupportedAudioFormatException if the audio format is not supported
     */
    void open(AudioFormat audioFormat) throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException;

    /**
     * Closes the audio line and releases all associated resources.
     * @throws AudioDeviceException if there's an error closing the audio line
     */
    void close();

    /**
     * Flushes the audio line, discarding any buffered audio data.
     * @throws AudioDeviceException if there's an error flushing the audio line
     */
    void flush();

    /**
     * Drains the audio line, playing all buffered audio data.
     * @throws AudioDeviceException if there's an error draining the audio line
     */
    void drain();

    /**
     * Starts the audio line.
     * @throws AudioDeviceException if there's an error starting the audio line
     */
    void start();

    /**
     * Stops the audio line.
     * @throws AudioDeviceException if there's an error stopping the audio line
     */
    void stop();

    /**
     * Checks if the audio line is open.
     * @return true if the audio line is open, false otherwise
     */
    boolean isOpen();

    /**
     * Gets the number of available bytes in the audio line's buffer.
     * @return the number of available bytes
     */
    int available();

    /**
     * Gets the buffer size of the audio line.
     * @return the buffer size
     */
    int getBufferSize();

    /**
     * Gets the current frame position of the audio line.
     * @return the frame position
     */
    long getFramePosition();

    /**
     * Gets the current position of the audio line in microseconds.
     * @return the position in microseconds
     */
    long getMicrosecondPosition();

    /**
     * Gets the latency of the audio line in microseconds.
     * @return the latency in microseconds
     */
    long getMicrosecondLatency();

    /**
     * Gets the current audio port of the audio line.
     * @return the audio port
     */
    AudioPort getCurrentAudioPort();
}
