package org.theko.sound.backend;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;

/**
 * The {@code AudioOutputBackend} interface represents an audio output backend capable of
 * handling audio output operations. It extends {@link AudioBackend} and {@link AutoCloseable}
 * for initialization, control, and resource management.
 *
 * <p>Implementations of this interface should provide mechanisms to open and close the backend,
 * manage audio data flow, and retrieve backend-related information.</p>
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Opening the backend for audio output with specified port and format.</li>
 *   <li>Checking if the backend is open and managing its state.</li>
 *   <li>Writing audio data into the backend.</li>
 *   <li>Retrieving audio backend information such as buffer size, latency, and frame position.</li>
 * </ul>
 *
 * @see AudioBackend
 * @see AutoCloseable
 * @see AudioBackendException
 * 
 * @since v1.0.0
 * @author Theko
 */
public interface AudioOutputBackend extends AudioBackend, AutoCloseable {

    /**
     * Opens the audio output backend with the specified audio port, format, and buffer size.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    void open (AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException;
    /**
     * Opens the audio output backend with the specified audio port and format.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException;
    /**
     * Checks if the audio output backend is currently open.
     *
     * @return {@code true} if the backend is open, {@code false} otherwise.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    boolean isOpen () throws AudioBackendException;
    /**
     * Closes the audio output backend and releases resources.
     *
     * @throws AudioBackendException If an error occurs while closing the backend.
     */
    void close () throws AudioBackendException;
    /**
     * Starts audio output operations on the backend.
     *
     * @throws AudioBackendException If an error occurs while starting the backend.
     */
    void start () throws AudioBackendException;
    /**
     * Stops audio output operations on the backend.
     *
     * @throws AudioBackendException If an error occurs while stopping the backend.
     */
    void stop () throws AudioBackendException;
    /**
     * Flushes the audio output buffer to force immediate output.
     *
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    void flush () throws AudioBackendException;
    /**
     * Drains the audio output buffer to force all buffered data to be output.
     *
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    void drain () throws AudioBackendException;
    /**
     * Writes audio data into the backend.
     *
     * @param data The audio data to be written.
     * @param offset The offset in the array from which to write.
     * @param length The number of bytes to write.
     * @return The number of bytes successfully written.
     * @throws AudioBackendException If an error occurs while writing data.
     */
    int write (byte[] data, int offset, int length) throws AudioBackendException;
    /**
     * Returns the amount of free space in the audio output buffer.
     *
     * @return The number of bytes available in the buffer.
     * @throws AudioBackendException If an error occurs while retrieving the available space.
     */
    int available () throws AudioBackendException;

    /**
     * Returns the size of the audio output buffer.
     *
     * @return The size of the buffer.
     * @throws AudioBackendException If an error occurs while retrieving the buffer size.
     */
    int getBufferSize () throws AudioBackendException;

    /**
     * Returns the current frame position in the audio output stream.
     *
     * @return The current frame position.
     * @throws AudioBackendException If an error occurs while retrieving the frame position.
     */
    long getFramePosition () throws AudioBackendException;
    /**
     * Returns the current position in the audio output stream in microseconds.
     *
     * @return The current position in microseconds.
     * @throws AudioBackendException If an error occurs while retrieving the position.
     */
    long getMicrosecondPosition () throws AudioBackendException;

    /**
     * Returns the current latency in the audio output stream in microseconds.
     *
     * @return The current latency in microseconds.
     * @throws AudioBackendException If an error occurs while retrieving the latency.
     */
    long getMicrosecondLatency () throws AudioBackendException;
    /**
     * Returns the currently active audio port.
     *
     * @return The currently active audio port.
     * @throws AudioBackendException If an error occurs while retrieving the current port.
     */
    AudioPort getCurrentAudioPort () throws AudioBackendException;
}

