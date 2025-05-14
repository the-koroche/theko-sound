package org.theko.sound.direct.javasound;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioInputDevice;

/**
 * The {@code JavaSoundInput} class is an implementation of the {@link AudioInputDevice}
 * interface that uses the Java Sound API to handle audio input operations.
 * It extends the {@link JavaSoundDevice} class and provides functionality for
 * managing audio input devices, such as opening, closing, starting, stopping,
 * and reading audio data from a {@link TargetDataLine}.
 *
 * <p>This class supports operations such as:
 * <ul>
 *   <li>Opening an audio input device with a specified {@link AudioPort}, {@link AudioFormat}, and buffer size.</li>
 *   <li>Starting and stopping the audio input device.</li>
 *   <li>Reading audio data from the input device.</li>
 *   <li>Flushing and draining the audio input buffer.</li>
 *   <li>Retrieving information such as buffer size, frame position, and latency.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * JavaSoundInput input = new JavaSoundInput();
 * input.open(audioPort, audioFormat, bufferSize);
 * input.start();
 * byte[] buffer = new byte[1024];
 * int bytesRead = input.read(buffer, 0, buffer.length);
 * input.stop();
 * input.close();
 * }</pre>
 *
 * <p>Note: This class throws {@link AudioDeviceException} for various error
 * conditions, such as attempting to operate on a device that is not open or
 * when the requested audio format is unsupported.
 *
 * @see AudioInputDevice
 * @see JavaSoundDevice
 * @see TargetDataLine
 * @see AudioPort
 * @see AudioFormat
 * 
 * @since v1.2.0
 * 
 * @author Theko
 */
public class JavaSoundInput extends JavaSoundDevice implements AudioInputDevice {

    /**
     * The target data line used for capturing audio data.
     */
    private TargetDataLine targetDataLine;

    /**
     * Indicates whether the audio input device is open.
     */
    private boolean open;

    /**
     * The currently active audio port.
     */
    private AudioPort currentPort;

    @Override
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, getJavaSoundAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioDeviceException("Unsupported audio port or format.");
            }

            targetDataLine = (TargetDataLine) mixer.getLine(info);
            targetDataLine.open(getJavaSoundAudioFormat(audioFormat), bufferSize);
            this.currentPort = port;
            open = true;
        } catch (LineUnavailableException | UnsupportedAudioFormatException e) {
            throw new AudioDeviceException("Failed to open audio output line.", e);
        }
    }

    @Override
    public void open(AudioPort port, AudioFormat audioFormat) throws AudioDeviceException {
        open(port, audioFormat, audioFormat.getByteRate() / 5);
    }

    @Override
    public boolean isOpen() {
        return open && targetDataLine != null && targetDataLine.isOpen();
    }

    @Override
    public void close() throws AudioDeviceException {
        if (targetDataLine != null) {
            targetDataLine.close();
            open = false;
        }
    }

    @Override
    public void start() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot start. Device is not open.");
        }
        targetDataLine.start();
    }

    @Override
    public void stop() throws AudioDeviceException {
        if (isOpen()) {
            targetDataLine.stop();
        }
    }

    @Override
    public void flush() throws AudioDeviceException {
        if (isOpen()) {
            targetDataLine.flush();
        }
    }

    @Override
    public void drain() throws AudioDeviceException {
        if (isOpen()) {
            targetDataLine.drain();
        }
    }

    @Override
    public int read(byte[] data, int offset, int length) throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot write. Device is not open.");
        }
        return targetDataLine.read(data, offset, length);
    }

    @Override
    public int available() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot check availability. Device is not open.");
        }
        return targetDataLine.available();
    }

    @Override
    public int getBufferSize() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get buffer size. Device is not open.");
        }
        return targetDataLine.getBufferSize();
    }

    @Override
    public long getFramePosition() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get frame position. Device is not open.");
        }
        return targetDataLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get microsecond position. Device is not open.");
        }
        return targetDataLine.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get latency. Device is not open.");
        }
        return (long)((targetDataLine.getBufferSize() * 1000000L) / targetDataLine.getFormat().getFrameRate());
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return currentPort;
    }
}
