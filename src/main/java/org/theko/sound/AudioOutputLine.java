package org.theko.sound;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioOutputDevice;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;
import org.theko.sound.event.DataLineListener;

/**
 * Represents an audio output line that connects to an audio output device and manages audio data flow.
 * It allows for setting an input line, opening the audio device, writing data, and controlling playback.
 */
public class AudioOutputLine implements AutoCloseable {

    // Audio output device associated with this line
    protected final AudioOutputDevice aod;
    
    // Input data line for receiving data (can be null)
    protected DataLine input;

    protected Runnable onWritedRunnable;

    // Lock for synchronizing access to the input line
    private final Lock inputLock = new ReentrantLock();

    // Data line listener for handling data events
    private final DataLineListener dataLineAdapter = new DataLineAdapter() {
        @Override
        public void onSend(DataLineEvent e) {
            // When data is sent, force receive the data and write it
            byte[] bytes = e.getDataLine().forceReceive();
            dwrite(bytes, 0, bytes.length);
            if (onWritedRunnable != null) {
                onWritedRunnable.run();
            }
        }
    };

    /**
     * Creates a new AudioOutputLine with the specified audio output device.
     * 
     * @param aod The audio output device associated with this output line.
     */
    public AudioOutputLine(AudioOutputDevice aod) {
        // Ensure the audio device is not null
        this.aod = Objects.requireNonNull(aod, "AudioOutputDevice is null.");
    }

    /**
     * Creates a new AudioOutputLine and retrieves the default output device from the platform.
     * 
     * @throws AudioDeviceNotFoundException If no audio device is found.
     * @throws AudioDeviceCreationException If there is an error creating the audio device.
     */
    public AudioOutputLine() throws AudioDeviceNotFoundException, AudioDeviceCreationException {
        // Fetch the default output device for the platform
        this(AudioDevices.getOutputDevice(AudioDevices.getPlatformDevice().getDeviceClass()));
    }

    /**
     * Sets the input data line for this output line. 
     * If an input is already set, it will remove the previous listener before adding the new one.
     * 
     * @param input The input data line to set (can be null).
     */
    public void setInput(DataLine input) {
        inputLock.lock();
        try {
            if (this.input != null) {
                // Remove the existing listener if input is already set
                this.input.removeDataLineListener(dataLineAdapter);
            }
            this.input = input;
            if (input != null) {
                // Add the listener if input is not null
                input.addDataLineListener(dataLineAdapter);
            }
        } finally {
            inputLock.unlock();
        }
    }

    /**
     * Gets the current input data line.
     * 
     * @return The current input data line (can be null).
     */
    public DataLine getInput() {
        inputLock.lock();
        try {
            return input;
        } finally {
            inputLock.unlock();
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
        aod.open(
            port == null ? 
            Objects.requireNonNull(aod.getDefaultPort(AudioFlow.OUT, audioFormat)).get() :
            port, 
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
        aod.open(
            port == null ? 
            Objects.requireNonNull(aod.getDefaultPort(AudioFlow.OUT, audioFormat)).get() :
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
        aod.open(Objects.requireNonNull(aod.getDefaultPort(AudioFlow.OUT, audioFormat)).get(), audioFormat);
    }

    /**
     * Checks if the audio output line is currently open.
     * 
     * @return True if the output line is open, false otherwise.
     */
    public boolean isOpen() {
        return aod.isOpen();
    }

    /**
     * Closes the audio output line, releasing resources.
     */
    @Override
    public void close() {
        inputLock.lock();
        try {
            if (input != null) {
                // Remove the listener from the input data line before closing
                input.removeDataLineListener(dataLineAdapter);
            }
            aod.close();
        } finally {
            inputLock.unlock();
        }
    }

    /**
     * Starts the audio output device.
     * 
     * @throws IllegalStateException If the audio output device is not open.
     */
    public void start() {
        if (!aod.isOpen()) throw new IllegalStateException("AudioOutputDevice is not open.");
        aod.start();
    }

    /**
     * Stops the audio output device.
     */
    public void stop() {
        aod.stop();
    }

    /**
     * Flushes the audio output device, ensuring that all buffered data is processed.
     */
    public void flush() {
        aod.flush();
    }

    /**
     * Drains the audio output device, ensuring that all data is processed before stopping.
     */
    public void drain() {
        aod.drain();
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
    protected int dwrite(byte[] data, int offset, int length) {
        if (!aod.isOpen()) {
            throw new IllegalStateException("Cannot write. AudioOutputDevice is not open.");
        }

        int written = aod.write(data, offset, length);
        if (written < 0) {
            throw new AudioDeviceException("Write operation failed.");
        }

        return written;
    }

    /**
     * Writes data to the audio output line.
     * If the input line is set, returns -1 (no write operation).
     * 
     * @param data The data to write.
     * @param offset The offset into the data.
     * @param length The length of the data to write.
     * 
     * @return The number of bytes written, or -1 if input is set.
     */
    public int write(byte[] data, int offset, int length) {
        if (input != null) {
            // If input is set, don't write data
            return -1;
        }
        return dwrite(data, offset, length);
    }

    /**
     * Returns the number of bytes available for writing.
     * 
     * @return The number of available bytes.
     */
    public int available() {
        return aod.available();
    }

    /**
     * Returns the buffer size of the audio output device.
     * 
     * @return The buffer size.
     */
    public int getBufferSize() {
        return aod.getBufferSize();
    }

    /**
     * Returns the current frame position of the audio output device.
     * 
     * @return The current frame position.
     */
    public long getFramePosition() {
        return aod.getFramePosition();
    }

    /**
     * Returns the current microsecond position of the audio output device.
     * 
     * @return The current microsecond position.
     */
    public long getMicrosecondPosition() {
        return aod.getMicrosecondPosition();
    }

    /**
     * Returns the current microsecond latency of the audio output device.
     * 
     * @return The current microsecond latency.
     */
    public long getMicrosecondLatency() {
        return aod.getMicrosecondLatency();
    }

    /**
     * Returns the current audio port in use by the output device.
     * 
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort() {
        return aod.getCurrentAudioPort();
    }

    public void setOnWritedAction(Runnable runnable) {
        this.onWritedRunnable = runnable;
    }
}
