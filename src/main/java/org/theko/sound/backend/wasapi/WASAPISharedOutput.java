package org.theko.sound.backend.wasapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioConverter;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;

public class WASAPISharedOutput extends WASAPISharedBackend implements AudioOutputBackend {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedOutput.class);

    protected long nativeHandle;

    protected boolean isOpen = false;
    protected int bufferSize = -1;
    protected AudioFormat audioFormat = null;
    protected AudioPort port = null;

    @Override
    public synchronized void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        if (isOpen()) throw new AudioBackendException("Backend is already open.");
        if (port == null) throw new IllegalArgumentException("Port is null.");
        if (port.getFlow() != AudioFlow.OUT) throw new IllegalArgumentException("Port is not an output port.");
        if (port.getLink() == null) throw new IllegalArgumentException("Port link is null.");
        if (audioFormat == null) throw new IllegalArgumentException("Audio format is null.");
        if (audioFormat.isBigEndian()) throw new AudioBackendException("Big endian audio format is not supported.");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size is less than or equal to zero.");

        if (!isInitialized()) initialize();
        openOut(port, audioFormat, bufferSize);

        this.bufferSize = bufferSize;
        this.audioFormat = audioFormat;
        this.port = port;
        isOpen = true;
    }

    protected void openOut(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        openOut0(false, port, audioFormat, bufferSize);
        logger.debug("Native handle is {}", nativeHandle);
    }

    @Override
    public void open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        this.open(port, audioFormat, audioFormat.getByteRate() / 4);
    }

    @Override
    public boolean isOpen() throws AudioBackendException {
        return isOpen;
    }

    @Override
    public synchronized void close() throws AudioBackendException {
        if (isOpen()) closeOut0();
        isOpen = false;
        bufferSize = -1;
        audioFormat = null;
        port = null;
    }

    @Override
    public synchronized void start() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot start. Backend is not open.");
        startOut0();
    }

    @Override
    public synchronized void stop() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot stop. Backend is not open.");
        stopOut0();
    }

    @Override
    public synchronized void flush() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot flush. Backend is not open.");
        flushOut0();
    }

    @Override
    public synchronized void drain() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot drain. Backend is not open.");
        drainOut0();
    }

    @Override
    public synchronized int write(byte[] data, int offset, int length) throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot write. Backend is not open.");
        return writeOut0(data, offset, length);
    }

    @Override
    public int available() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot write. Backend is not open.");
        return availableOut0();
    }

    @Override
    public int getBufferSize() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get buffer size. Backend is not open.");
        try {
            return getBufferSizeOut0();
        } catch (AudioBackendException e) {
            return bufferSize;
        }
    }

    @Override
    public long getFramePosition() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get frame position. Backend is not open.");
        return getFramePositionOut0();
    }

    @Override
    public long getMicrosecondPosition() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get microsecond position. Backend is not open.");
        return AudioConverter.framesToMicroseconds(getFramePosition(), audioFormat.getSampleRate());
    }

    @Override
    public long getMicrosecondLatency() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get latency. Backend is not open.");
        return getMicrosecondLatency0();
    }

    @Override
    public AudioPort getCurrentAudioPort() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get current audio port. Backend is not open.");
        try {
            return getCurrentAudioPort0();
        } catch (AudioBackendException e) {
            return port;
        }
    }
    
    protected native void openOut0(boolean isExclusive, AudioPort port, AudioFormat audioFormat, int bufferSize);
    private native void closeOut0();
    private native void startOut0();
    private native void stopOut0();
    private native void flushOut0();
    private native void drainOut0();
    private native int writeOut0(byte[] data, int offset, int length);
    private native int availableOut0();
    private native int getBufferSizeOut0();
    private native long getFramePositionOut0();
    private native long getMicrosecondLatency0();
    private native AudioPort getCurrentAudioPort0();
}
