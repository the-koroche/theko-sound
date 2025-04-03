package org.theko.sound;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioInputDevice;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;
import org.theko.sound.event.DataLineListener;

/**
 * Represents an audio output line that connects to an audio output device and manages audio data flow.
 * It allows for setting an output line, opening the audio device, writing data, and controlling playback.
 */
public class AudioInputLine implements AutoCloseable {

    // Audio output device associated with this line
    protected final AudioInputDevice aid;
    
    // Input data line for receiving data (can be null)
    protected DataLine output;

    // Lock for synchronizing access to the output line
    private final Lock outputLock = new ReentrantLock();

    /**
     * Creates a new AudioInputLine with the specified audio output device.
     * 
     * @param aid The audio output device associated with this output line.
     */
    public AudioInputLine(AudioInputDevice aid) {
        // Ensure the audio device is not null
        this.aid = Objects.requireNonNull(aid, "AudioInputDevice is null.");
    }

    /**
     * Creates a new AudioInputLine and retrieves the default output device from the platform.
     * 
     * @throws AudioDeviceNotFoundException If no audio device is found.
     * @throws AudioDeviceCreationException If there is an error creating the audio device.
     */
    public AudioInputLine() throws AudioDeviceNotFoundException, AudioDeviceCreationException {
        // Fetch the default output device for the platform
        this(AudioDevices.getInputDevice(AudioDevices.getPlatformDevice().getDeviceClass()));
    }

    /**
     * Sets the output data line for this output line. 
     * If an output is already set, it will remove the previous listener before adding the new one.
     * 
     * @param output The output data line to set (can be null).
     */
    public void setInput(DataLine output) {
        byte[] buffer = new byte[getBufferSize()];
        while (isOpen() && !Thread.currentThread().isInterrupted()) {
            dread(buffer, 0, buffer.length);
            try {
                output.send(buffer);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } 
    }

    /**
     * Gets the current output data line.
     * 
     * @return The current output data line (can be null).
     */
    public DataLine getInput() {
        outputLock.lock();
        try {
            return output;
        } finally {
            outputLock.unlock();
        }
    }

    /**
     * Opens the audio output line with the specified port, audio format, and buffer size.
     * If no port is provided, the default output port for the given audio format will be used.
     * 
     * @param port The audio port to use for output (can be null).
     * @param audioFormat The audio format for playback.
     * @param bufferSize The buffer size for the output.
     * 
     * @throws AudioDeviceException If the audio device cannot be opened.
     * @throws AudioPortsNotFoundException If the specified port cannot be found.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     */
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSize) 
            throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        aid.open(
            port == null ? 
            Objects.requireNonNull(aid.getDefaultPort(AudioFlow.IN, audioFormat)).get() :
            port, 
            audioFormat, 
            bufferSize
        );
    }

    /**
     * Opens the audio output line with the specified port, audio format, and buffer size.
     * If no port is provided, the default output port for the given audio format will be used.
     * 
     * @param port The audio port to use for output (can be null).
     * @param audioFormat The audio format for playback.
     * @param bufferSize The buffer size for the output.
     * 
     * @throws AudioDeviceException If the audio device cannot be opened.
     * @throws AudioPortsNotFoundException If the specified port cannot be found.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     */
    public void open(AudioFormat audioFormat, int bufferSize) 
            throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        aid.open(
            Objects.requireNonNull(aid.getDefaultPort(AudioFlow.IN, audioFormat)).get(),
            audioFormat, 
            bufferSize
        );
    }

    /**
     * Opens the audio output line with the specified port and audio format.
     * If no port is provided, the default output port for the given audio format will be used.
     * 
     * @param port The audio port to use for output (can be null).
     * @param audioFormat The audio format for playback.
     * 
     * @throws AudioDeviceException If the audio device cannot be opened.
     * @throws AudioPortsNotFoundException If the specified port cannot be found.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     */
    public void open(AudioPort port, AudioFormat audioFormat) 
            throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        aid.open(
            port == null ? 
            Objects.requireNonNull(aid.getDefaultPort(AudioFlow.IN, audioFormat)).get() :
            port, 
            audioFormat
        );
    }

    /**
     * Opens the audio output line with the specified audio format.
     * Uses the default output port for the given audio format.
     * 
     * @param audioFormat The audio format for playback.
     * 
     * @throws AudioDeviceException If the audio device cannot be opened.
     * @throws AudioPortsNotFoundException If the specified port cannot be found.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     */
    public void open(AudioFormat audioFormat) 
            throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        aid.open(Objects.requireNonNull(aid.getDefaultPort(AudioFlow.IN, audioFormat)).get(), audioFormat);
    }

    /**
     * Checks if the audio output line is currently open.
     * 
     * @return True if the output line is open, false otherwise.
     */
    public boolean isOpen() {
        return aid.isOpen();
    }

    /**
     * Closes the audio output line, releasing resources.
     */
    @Override
    public void close() {
        aid.close();
    }

    /**
     * Starts the audio output device.
     * 
     * @throws IllegalStateException If the audio output device is not open.
     */
    public void start() {
        if (!aid.isOpen()) throw new IllegalStateException("AudioInputDevice is not open.");
        aid.start();
    }

    /**
     * Stops the audio output device.
     */
    public void stop() {
        aid.stop();
    }

    /**
     * Flushes the audio output device, ensuring that all buffered data is processed.
     */
    public void flush() {
        aid.flush();
    }

    /**
     * Drains the audio output device, ensuring that all data is processed before stopping.
     */
    public void drain() {
        aid.drain();
    }

    /**
     * Writes data to the audio output device.
     * 
     * @param data The data to write.
     * @param offset The offset into the data.
     * @param length The length of the data to write.
     * 
     * @return The number of bytes written.
     * @throws IllegalStateException If the audio output device is not open.
     * @throws AudioDeviceException If the write operation fails.
     */
    protected int dread(byte[] data, int offset, int length) {
        if (!aid.isOpen()) {
            throw new IllegalStateException("Cannot write. AudioInputDevice is not open.");
        }

        int readed = aid.read(data, offset, length);
        if (readed < 0) {
            throw new AudioDeviceException("Write operation failed.");
        }

        return readed;
    }

    /**
     * Writes data to the audio output line.
     * If the output line is set, returns -1 (no write operation).
     * 
     * @param data The data to write.
     * @param offset The offset into the data.
     * @param length The length of the data to write.
     * 
     * @return The number of bytes written, or -1 if output is set.
     */
    public int read(byte[] data, int offset, int length) {
        if (output != null) {
            // If output is set, don't write data
            return -1;
        }
        return dread(data, offset, length);
    }

    /**
     * Returns the number of bytes available for writing.
     * 
     * @return The number of available bytes.
     */
    public int available() {
        return aid.available();
    }

    /**
     * Returns the buffer size of the audio output device.
     * 
     * @return The buffer size.
     */
    public int getBufferSize() {
        return aid.getBufferSize();
    }

    /**
     * Returns the current frame position of the audio output device.
     * 
     * @return The current frame position.
     */
    public long getFramePosition() {
        return aid.getFramePosition();
    }

    /**
     * Returns the current microsecond position of the audio output device.
     * 
     * @return The current microsecond position.
     */
    public long getMicrosecondPosition() {
        return aid.getMicrosecondPosition();
    }

    /**
     * Returns the current microsecond latency of the audio output device.
     * 
     * @return The current microsecond latency.
     */
    public long getMicrosecondLatency() {
        return aid.getMicrosecondLatency();
    }

    /**
     * Returns the current audio port in use by the output device.
     * 
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort() {
        return aid.getCurrentAudioPort();
    }
}
