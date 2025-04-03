package org.theko.sound.direct.javasound;

import java.lang.ref.Cleaner;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioInputDevice;

public class JavaSoundInput extends JavaSoundDevice implements AudioInputDevice {

    private TargetDataLine targetDataLine;
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
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, getJavaAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioDeviceException("Unsupported audio port or format.");
            }

            targetDataLine = (TargetDataLine) mixer.getLine(info);
            targetDataLine.open(getJavaAudioFormat(audioFormat), bufferSize);
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
