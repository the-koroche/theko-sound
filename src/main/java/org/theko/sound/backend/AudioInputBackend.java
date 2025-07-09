package org.theko.sound.backend;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
/**
 * The {@code AudioInputBackend} interface represents an audio input backend capable of
 * handling audio data input operations. It extends {@link AudioBackend} and {@link AutoCloseable}
 * for initialization, control, and resource management.
 * 
 * <p>Implementations of this interface should provide mechanisms to open and close the backend,
 * manage audio data flow, and retrieve backend-related information.</p>
 * 
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Opening the backend for audio input with specified port and format.</li>
 *   <li>Checking if the backend is open and managing its state.</li>
 *   <li>Reading audio data into a buffer.</li>
 *   <li>Retrieving audio backend information such as buffer size and latency.</li>
 * </ul>
 * 
 * @see AudioBackend
 * @see AutoCloseable
 * @see AudioBackendException
 * @see AudioPort
 * @see AudioFormat
 * @see AudioOutputBackend
 * 
 * @since v1.0.0
 * @author Theko
 */
public interface AudioInputBackend extends AudioBackend, AutoCloseable {

    /**
     * Opens the audio input backend with the specified audio port, format, and buffer size.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    void open (AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException;

    /**
     * Opens the audio input backend with the specified audio port and format.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException;

    /**
     * Checks if the audio input backend is currently open.
     *
     * @return {@code true} if the backend is open, {@code false} otherwise.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    boolean isOpen () throws AudioBackendException;

    /**
     * Closes the audio input backend and releases resources.
     *
     * @throws AudioBackendException If an error occurs while closing the backend.
     */
    void close () throws AudioBackendException;

    /**
     * Starts audio input operations on the backend.
     *
     * @throws AudioBackendException If an error occurs while starting the backend.
     */
    void start () throws AudioBackendException;

    /**
     * Stops audio input operations on the backend.
     *
     * @throws AudioBackendException If an error occurs while stopping the backend.
     */
    void stop () throws AudioBackendException;

    /**
     * Flushes the audio input buffer, discarding any buffered data.
     *
     * @throws AudioBackendException If an error occurs during the operation.
     */
    void flush () throws AudioBackendException;

    /**
     * Drains the audio input buffer, ensuring all buffered data is processed.
     *
     * @throws AudioBackendException If an error occurs during the operation.
     */
    void drain () throws AudioBackendException;

    /**
     * Reads audio data into the specified buffer.
     *
     * @param buffer The buffer to store audio data.
     * @param offset The start offset in the buffer.
     * @param length The maximum number of bytes to read.
     * @return The number of bytes actually read.
     * @throws AudioBackendException If an error occurs during reading.
     */
    int read (byte[] buffer, int offset, int length) throws AudioBackendException;

    /**
     * Returns the number of bytes available for reading.
     *
     * @return The number of available bytes.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    int available () throws AudioBackendException;

    /**
     * Retrieves the size of the audio input buffer.
     *
     * @return The buffer size in bytes.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    int getBufferSize () throws AudioBackendException;

    /**
     * Retrieves the current frame position of the audio input backend.
     *
     * @return The frame position.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    long getFramePosition () throws AudioBackendException;

    /**
     * Retrieves the current microsecond position of audio input.
     *
     * @return The microsecond position.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    long getMicrosecondPosition () throws AudioBackendException;

    /**
     * Retrieves the latency of the audio input backend in microseconds.
     *
     * @return The latency in microseconds.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    long getMicrosecondLatency () throws AudioBackendException;

    /**
     * Retrieves the current audio port being used by the backend.
     *
     * @return The current {@link AudioPort}.
     * @throws AudioBackendException If an error occurs during the operation.
     */
    AudioPort getCurrentAudioPort () throws AudioBackendException;
}

