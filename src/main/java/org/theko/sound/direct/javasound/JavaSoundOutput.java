package org.theko.sound.direct.javasound;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioOutputDevice;

/**
 * The {@code JavaSoundOutput} class is an implementation of the {@link AudioOutputDevice}
 * interface that uses Java Sound API to manage audio output. It extends the {@link JavaSoundDevice}
 * class and provides functionality for opening, closing, and controlling audio output lines.
 * 
 * <p>This class manages a {@link SourceDataLine} for audio playback and supports operations
 * such as starting, stopping, flushing, draining, and writing audio data. It also provides
 * methods to retrieve information about the audio line, such as buffer size, frame position,
 * and latency.</p>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Opening and configuring audio output lines with specified {@link AudioPort} and {@link AudioFormat}.</li>
 *   <li>Support for querying the current audio port and checking the state of the audio line.</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * JavaSoundOutput output = new JavaSoundOutput();
 * output.open(audioPort, audioFormat);
 * output.start();
 * output.write(audioData, 0, audioData.length);
 * output.stop();
 * output.close();
 * }</pre>
 * 
 * <p>Note: This class throws {@link AudioDeviceException} for various error conditions,
 * such as unsupported audio formats or attempting to operate on a closed device.</p>
 * 
 * @see AudioOutputDevice
 * @see JavaSoundDevice
 * @see SourceDataLine
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class JavaSoundOutput extends JavaSoundDevice implements AudioOutputDevice {

    /**
     * The {@link SourceDataLine} instance used for audio playback.
     */
    private SourceDataLine sourceDataLine;

    /**
     * A flag indicating whether the audio output line is open.
     */
    private boolean open;

    /**
     * The current {@link AudioPort} used for audio output.
     */
    private AudioPort currentPort;

    @Override
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, getJavaSoundAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioDeviceException("Unsupported audio port or format.");
            }

            sourceDataLine = (SourceDataLine) mixer.getLine(info);
            sourceDataLine.open(getJavaSoundAudioFormat(audioFormat), bufferSize);
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
        return open && sourceDataLine != null && sourceDataLine.isOpen();
    }

    @Override
    public void close() throws AudioDeviceException {
        if (sourceDataLine != null) {
            sourceDataLine.close();
            open = false;
        }
    }

    @Override
    public void start() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot start. Device is not open.");
        }
        sourceDataLine.start();
    }

    @Override
    public void stop() throws AudioDeviceException {
        if (isOpen()) {
            sourceDataLine.stop();
        }
    }

    @Override
    public void flush() throws AudioDeviceException {
        if (isOpen()) {
            sourceDataLine.flush();
        }
    }

    @Override
    public void drain() throws AudioDeviceException {
        if (isOpen()) {
            sourceDataLine.drain();
        }
    }

    @Override
    public int write(byte[] data, int offset, int length) throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot write. Device is not open.");
        }
        return sourceDataLine.write(data, offset, length);
    }

    @Override
    public int available() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot check availability. Device is not open.");
        }
        return sourceDataLine.available();
    }

    @Override
    public int getBufferSize() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get buffer size. Device is not open.");
        }
        return sourceDataLine.getBufferSize();
    }

    @Override
    public long getFramePosition() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get frame position. Device is not open.");
        }
        return sourceDataLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get microsecond position. Device is not open.");
        }
        return sourceDataLine.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() throws AudioDeviceException {
        if (!isOpen()) {
            throw new AudioDeviceException("Cannot get latency. Device is not open.");
        }
        return (long)((sourceDataLine.getBufferSize() * 1000000L) / sourceDataLine.getFormat().getFrameRate());
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return currentPort;
    }
}
