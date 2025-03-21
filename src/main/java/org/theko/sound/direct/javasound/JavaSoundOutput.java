package org.theko.sound.direct.javasound;

import java.lang.ref.Cleaner;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioOutputDevice;

public class JavaSoundOutput extends JavaSoundDevice implements AudioOutputDevice {

    private SourceDataLine sourceDataLine;
    private boolean open;
    private AudioPort currentPort;
    
    private static final Cleaner cleaner = Cleaner.create();

    @Override
    public void initialize() {
        cleaner.register(this, this::close);
    }

    @Override
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, getJavaAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioDeviceException("Unsupported audio port or format.");
            }

            sourceDataLine = (SourceDataLine) mixer.getLine(info);
            sourceDataLine.open(getJavaAudioFormat(audioFormat), bufferSize);
            this.currentPort = port;
            open = true;
        } catch (LineUnavailableException | UnsupportedAudioFormatException e) {
            throw new AudioDeviceException("Failed to open audio output line.", e);
        }
    }

    @Override
    public void open(AudioPort port, AudioFormat audioFormat) throws AudioDeviceException {
        open(port, audioFormat, audioFormat.getByteRate());
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
