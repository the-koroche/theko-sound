package org.theko.sound.backend.javasound;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioOutputBackend;

/**
 * The {@code JavaSoundOutput} class is an implementation of the {@link AudioOutputBackend}
 * interface that uses Java Sound API to manage audio output. It extends the {@link JavaSoundBackend}
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
 * <p>Note: This class throws {@link AudioBackendException} for various error conditions,
 * such as unsupported audio formats or attempting to operate on a closed backend.</p>
 * 
 * @see AudioOutputBackend
 * @see JavaSoundBackend
 * @see SourceDataLine
 * 
 * @since v1.0.0
 * @author Theko
 */
public class JavaSoundOutput extends JavaSoundBackend implements AudioOutputBackend {

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
    public void open (AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, getJavaSoundAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioBackendException("Unsupported audio port or format.");
            }

            sourceDataLine = (SourceDataLine) mixer.getLine(info);
            sourceDataLine.open(getJavaSoundAudioFormat(audioFormat), bufferSize);
            this.currentPort = port;
            open = true;
        } catch (LineUnavailableException | UnsupportedAudioFormatException ex) {
            throw new AudioBackendException("Failed to open audio output line.", ex);
        }
    }

    @Override
    public void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        open(port, audioFormat, audioFormat.getByteRate() / 5);
    }

    @Override
    public boolean isOpen () {
        return open && sourceDataLine != null && sourceDataLine.isOpen();
    }

    @Override
    public void close () throws AudioBackendException {
        if (sourceDataLine != null) {
            sourceDataLine.close();
            open = false;
        }
    }

    @Override
    public void start () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot start. Backend is not open.");
        }
        sourceDataLine.start();
    }

    @Override
    public void stop () throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.stop();
        }
    }

    @Override
    public void flush () throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.flush();
        }
    }

    @Override
    public void drain () throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.drain();
        }
    }

    @Override
    public int write (byte[] data, int offset, int length) throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot write. Backend is not open.");
        }
        return sourceDataLine.write(data, offset, length);
    }

    @Override
    public int available () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot check availability. Backend is not open.");
        }
        return sourceDataLine.available();
    }

    @Override
    public int getBufferSize () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot get buffer size. Backend is not open.");
        }
        return sourceDataLine.getBufferSize();
    }

    @Override
    public long getFramePosition () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot get frame position. Backend is not open.");
        }
        return sourceDataLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot get microsecond position. Backend is not open.");
        }
        return sourceDataLine.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency () throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot get latency. Backend is not open.");
        }
        return (long)((sourceDataLine.getBufferSize() * 1000000L) / sourceDataLine.getFormat().getFrameRate());
    }

    @Override
    public AudioPort getCurrentAudioPort () {
        return currentPort;
    }
}
