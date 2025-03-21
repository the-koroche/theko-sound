package org.theko.sound;

import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioOutputDevice;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;
import org.theko.sound.event.DataLineListener;

public class AudioOutputLine implements AutoCloseable {
    protected final AudioOutputDevice aod;
    protected DataLine input;
    private final Lock inputLock = new ReentrantLock();

    private static final Cleaner cleaner = Cleaner.create();

    private final DataLineListener dataLineAdapter = new DataLineAdapter() {
        @Override
        public void onSend(DataLineEvent e) {
            byte[] bytes = e.getDataLine().forceReceive();
            write0(bytes, 0, bytes.length);
        }
    };

    public AudioOutputLine(AudioOutputDevice aod) {
        this.aod = Objects.requireNonNull(aod, "AudioOutputDevice is null.");
        cleaner.register(this, this::close);
    }

    public AudioOutputLine() throws AudioDeviceNotFoundException, AudioDeviceCreationException {
        this(AudioDevices.getOutputDevice(AudioDevices.getPlatformDevice().getDeviceClass()));
    }

    public void setInput(DataLine input) {
        inputLock.lock();
        try {
            if (this.input != null) {
                this.input.removeDataLineListener(dataLineAdapter);
            }
            this.input = input;
            if (input != null) {
                input.addDataLineListener(dataLineAdapter);
            }
        } finally {
            inputLock.unlock();
        }
    }

    public DataLine getInput() {
        inputLock.lock();
        try {
            return input;
        } finally {
            inputLock.unlock();
        }
    }

    public void open(AudioPort port, AudioFormat audioFormat, int bufferSize) {
        aod.open(port, audioFormat, bufferSize);
    }

    public void open(AudioPort port, AudioFormat audioFormat) {
        aod.open(port, audioFormat);
    }

    public void open(AudioFormat audioFormat) throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        aod.open(Objects.requireNonNull(aod.getDefaultPort(AudioFlow.OUT, audioFormat)).get(), audioFormat);
    }

    public boolean isOpen() {
        return aod.isOpen();
    }

    @Override
    public void close() {
        inputLock.lock();
        try {
            if (input != null) {
                input.removeDataLineListener(dataLineAdapter);
            }
            aod.close();
        } finally {
            inputLock.unlock();
        }
    }

    public void start() {
        if (!aod.isOpen()) throw new IllegalStateException("AudioOutputDevice is not open.");
        aod.start();
    }

    public void stop() {
        aod.stop();
    }

    public void flush() {
        aod.flush();
    }

    public void drain() {
        aod.drain();
    }

    protected int write0(byte[] data, int offset, int length) {
        if (!aod.isOpen()) {
            throw new IllegalStateException("Cannot write. AudioOutputDevice is not open.");
        }

        int written = aod.write(data, offset, length);
        if (written < 0) {
            throw new AudioDeviceException("Write operation failed.");
        }

        return written;
    }

    public int write(byte[] data, int offset, int length) {
        if (input != null) {
            return -1;
        }
        return write0(data, offset, length);
    }

    public int available() {
        return aod.available();
    }

    public int getBufferSize() {
        return aod.getBufferSize();
    }

    public long getFramePosition() {
        return aod.getFramePosition();
    }

    public long getMicrosecondPosition() {
        return aod.getMicrosecondPosition();
    }

    public long getMicrosecondLatency() {
        return aod.getMicrosecondLatency();
    }

    public AudioPort getCurrentAudioPort() {
        return aod.getCurrentAudioPort();
    }
}
