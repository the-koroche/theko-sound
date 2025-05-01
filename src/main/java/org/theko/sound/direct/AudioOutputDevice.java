package org.theko.sound.direct;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;

/**
 * The {@code AudioOutputDevice} interface represents an audio output device capable of
 * handling audio output operations. It extends {@link AudioDevice} and {@link AutoCloseable}
 * for initialization, control, and resource management.
 *
 * <p>Implementations of this interface should provide mechanisms to open and close the device,
 * manage audio data flow, and retrieve device-related information.</p>
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *   <li>Opening the device for audio output with specified port and format.</li>
 *   <li>Checking if the device is open and managing its state.</li>
 *   <li>Writing audio data into the device.</li>
 *   <li>Retrieving audio device information such as buffer size, latency, and frame position.</li>
 * </ul>
 *
 * @see AudioDevice
 * @see AutoCloseable
 * @see AudioDeviceException
 */
public interface AudioOutputDevice extends AudioDevice, AutoCloseable {
    /**
     * Opens the audio output device with the specified audio port, format, and buffer size.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data.
     * @throws AudioDeviceException If an error occurs while opening the device.
     */
    void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException;
    /**
     * Opens the audio output device with the specified audio port and format.
     *
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioDeviceException If an error occurs while opening the device.
     */
    void open(AudioPort port, AudioFormat audioFormat) throws AudioDeviceException;
    /**
     * Checks if the audio output device is currently open.
     *
     * @return {@code true} if the device is open, {@code false} otherwise.
     * @throws AudioDeviceException If an error occurs during the operation.
     */
    boolean isOpen() throws AudioDeviceException;
    /**
     * Closes the audio output device and releases resources.
     *
     * @throws AudioDeviceException If an error occurs while closing the device.
     */
    void close() throws AudioDeviceException;
    /**
     * Starts audio output operations on the device.
     *
     * @throws AudioDeviceException If an error occurs while starting the device.
     */
    void start() throws AudioDeviceException;
    /**
     * Stops audio output operations on the device.
     *
     * @throws AudioDeviceException If an error occurs while stopping the device.
     */
    void stop() throws AudioDeviceException;
    /**
     * Flushes the audio output buffer to force immediate output.
     *
     * @throws AudioDeviceException If an error occurs while flushing the buffer.
     */
    void flush() throws AudioDeviceException;
    /**
     * Drains the audio output buffer to force all buffered data to be output.
     *
     * @throws AudioDeviceException If an error occurs while draining the buffer.
     */
    void drain() throws AudioDeviceException;
    /**
     * Writes audio data into the device.
     *
     * @param data The audio data to be written.
     * @param offset The offset in the array from which to write.
     * @param length The number of bytes to write.
     * @return The number of bytes successfully written.
     * @throws AudioDeviceException If an error occurs while writing data.
     */
    int write(byte[] data, int offset, int length) throws AudioDeviceException;
    /**
     * Returns the amount of free space in the audio output buffer.
     *
     * @return The number of bytes available in the buffer.
     * @throws AudioDeviceException If an error occurs while retrieving the available space.
     */
    int available() throws AudioDeviceException;

    /**
     * Returns the size of the audio output buffer.
     *
     * @return The size of the buffer.
     * @throws AudioDeviceException If an error occurs while retrieving the buffer size.
     */
    int getBufferSize() throws AudioDeviceException;

    /**
     * Returns the current frame position in the audio output stream.
     *
     * @return The current frame position.
     * @throws AudioDeviceException If an error occurs while retrieving the frame position.
     */
    long getFramePosition() throws AudioDeviceException;
    /**
     * Returns the current position in the audio output stream in microseconds.
     *
     * @return The current position in microseconds.
     * @throws AudioDeviceException If an error occurs while retrieving the position.
     */
    long getMicrosecondPosition() throws AudioDeviceException;

    /**
     * Returns the current latency in the audio output stream in microseconds.
     *
     * @return The current latency in microseconds.
     * @throws AudioDeviceException If an error occurs while retrieving the latency.
     */
    long getMicrosecondLatency() throws AudioDeviceException;
    /**
     * Returns the currently active audio port.
     *
     * @return The currently active audio port.
     * @throws AudioDeviceException If an error occurs while retrieving the current port.
     */
    AudioPort getCurrentAudioPort() throws AudioDeviceException;
}

