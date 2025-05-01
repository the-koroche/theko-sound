package org.theko.sound.direct;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
/**
 * The {@code AudioInputDevice} interface represents an audio input device capable of
 * handling audio data input operations. It extends {@link AudioDevice} and {@link AutoCloseable}
 * for initialization, control, and resource management.
 * 
 * <p>Implementations of this interface should provide mechanisms to open and close the device,
 * manage audio data flow, and retrieve device-related information.</p>
 * 
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Opening the device for audio input with specified port and format.</li>
 *   <li>Checking if the device is open and managing its state.</li>
 *   <li>Reading audio data into a buffer.</li>
 *   <li>Retrieving audio device information such as buffer size and latency.</li>
 * </ul>
 * 
 * @see AudioDevice
 * @see AutoCloseable
 * @see AudioDeviceException
 * @see AudioPort
 * @see AudioFormat
 * @see AudioOutputDevice
 * 
 * @author Alex Soloviov
 */
public interface AudioInputDevice extends AudioDevice, AutoCloseable {

    /**
     * Opens the audio input device with the specified audio port, format, and buffer size.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data.
     * @throws AudioDeviceException If an error occurs while opening the device.
     */
    void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException;

    /**
     * Opens the audio input device with the specified audio port and format.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioDeviceException If an error occurs while opening the device.
     */
    void open(AudioPort port, AudioFormat audioFormat) throws AudioDeviceException;

    /**
     * Checks if the audio input device is currently open.
     *
     * @return {@code true} if the device is open, {@code false} otherwise.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    boolean isOpen() throws AudioDeviceException;

    /**
     * Closes the audio input device and releases resources.
     *
     * @throws AudioDeviceException If an error occurs while closing the device.
     */
    void close() throws AudioDeviceException;

    /**
     * Starts audio input operations on the device.
     *
     * @throws AudioDeviceException If an error occurs while starting the device.
     */
    void start() throws AudioDeviceException;

    /**
     * Stops audio input operations on the device.
     *
     * @throws AudioDeviceException If an error occurs while stopping the device.
     */
    void stop() throws AudioDeviceException;

    /**
     * Flushes the audio input buffer, discarding any buffered data.
     *
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    void flush() throws AudioDeviceException;

    /**
     * Drains the audio input buffer, ensuring all buffered data is processed.
     *
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    void drain() throws AudioDeviceException;

    /**
     * Reads audio data into the specified buffer.
     *
     * @param buffer The buffer to store audio data.
     * @param offset The start offset in the buffer.
     * @param length The maximum number of bytes to read.
     * @return The number of bytes actually read.
     * @throws AudioDeviceException If an error occurs during reading.
     */
    int read(byte[] buffer, int offset, int length) throws AudioDeviceException;

    /**
     * Returns the number of bytes available for reading.
     *
     * @return The number of available bytes.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    int available() throws AudioDeviceException;

    /**
     * Retrieves the size of the audio input buffer.
     *
     * @return The buffer size in bytes.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    int getBufferSize() throws AudioDeviceException;

    /**
     * Retrieves the current frame position of the audio input device.
     *
     * @return The frame position.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    long getFramePosition() throws AudioDeviceException;

    /**
     * Retrieves the current microsecond position of audio input.
     *
     * @return The microsecond position.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    long getMicrosecondPosition() throws AudioDeviceException;

    /**
     * Retrieves the latency of the audio input device in microseconds.
     *
     * @return The latency in microseconds.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    long getMicrosecondLatency() throws AudioDeviceException;

    /**
     * Retrieves the current audio port being used by the device.
     *
     * @return The current {@link AudioPort}.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    AudioPort getCurrentAudioPort() throws AudioDeviceException;
}

