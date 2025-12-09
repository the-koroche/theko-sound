/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.backend.dummy;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.AudioUnitsConverter;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.utility.TimeUtilities;

/**
 * A dummy audio input backend.
 * 
 * This class is used to simulate an audio input backend for testing purposes.
 * It implements the {@link AudioInputBackend} interface and provides all the necessary methods.
 * 
 * @author Theko
 * @since 2.4.1
 */
public class DummyInputBackend extends DummyAudioBackend implements AudioInputBackend {

    private boolean isOpen = false;
    private boolean isStarted = false;
    private int bufferSize = -1;
    private AudioFormat format;
    private AudioPort port;

    private long framePosition = 0;

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        if (port == null) {
            // Using default output port
            try {
                port = getDefaultPort(AudioFlow.IN).orElse(null);
            } catch (AudioPortsNotFoundException e) {
                throw new AudioBackendException("Default input port not found.", e);
            }
        }
        if (port.getFlow() != AudioFlow.IN) throw new IllegalArgumentException("Port is not an input port.");
        if (port.getLink() == null) throw new IllegalArgumentException("Port link is null.");
        if (audioFormat == null) throw new IllegalArgumentException("Audio format is null.");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size is less than or equal to zero.");

        if (!isFormatSupported(port, audioFormat)) {
            throw new IllegalArgumentException("Audio format is not supported.");
        }

        this.isOpen = true;
        this.isStarted = false;
        this.bufferSize = bufferSize;
        this.format = audioFormat;
        this.port = port;
        
        return audioFormat;
    }

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        return this.open(port, audioFormat, audioFormat.getByteRate() / 4 /* 0.25 seconds */);
    }

    @Override
    public boolean isOpen() throws AudioBackendException {
        return this.isOpen;
    }

    @Override
    public void close() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot close. Backend is not open.");
        this.isOpen = false;
        this.isStarted = false;
        this.bufferSize = -1;
        this.format = null;
        this.port = null;
    }

    @Override
    public void start() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot start. Backend is not open.");
        if (isStarted) return;
        isStarted = true;
    }

    @Override
    public void stop() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot stop. Backend is not open.");
        if (!isStarted) return;
        isStarted = false;
    }

    @Override
    public void flush() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot flush. Backend is not open.");
    }

    @Override
    public void drain() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot drain. Backend is not open.");
        sleepRead(available());
        framePosition += available() / format.getFrameSize();
    }

    @Override
    public int read(byte[] data, int offset, int length) throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot read. Backend is not open.");
        if (data == null) throw new NullPointerException("Data buffer is null.");
        if (offset < 0 || length < 0 || offset + length > data.length) 
            throw new IndexOutOfBoundsException("Invalid offset or length.");


        int bytesRead = 0;
        while (bytesRead < length) {
            int bytesToRead = Math.min(bufferSize - (int)(framePosition % bufferSize), length - bytesRead);
            framePosition += bytesToRead / format.getFrameSize();
            bytesRead += bytesToRead;
            sleepRead(bytesToRead);
        }

        return bytesRead;
    }

    private void sleepRead(int dataLength) {
        long frames = dataLength / format.getFrameSize();
        long mcs = AudioUnitsConverter.framesToMicroseconds(frames, format.getSampleRate());
        TimeUtilities.waitMicrosPrecise(mcs);
    }

    @Override
    public int available() throws AudioBackendException {
        return bufferSize - (int)(framePosition % bufferSize);
    }

    @Override
    public int getBufferSize() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get buffer size. Backend is not open.");
        return this.bufferSize;
    }

    @Override
    public long getFramePosition() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get frame position. Backend is not open.");
        return this.framePosition;
    }

    @Override
    public long getMicrosecondPosition() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get microsecond position. Backend is not open.");
        return AudioUnitsConverter.framesToMicroseconds(getFramePosition(), format.getSampleRate());
    }

    @Override
    public long getMicrosecondLatency() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get latency. Backend is not open.");
        return (long) ((bufferSize / (float) format.getSampleRate()) * 1000000);
    }

    @Override
    public AudioPort getCurrentAudioPort() throws AudioBackendException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get current audio port. Backend is not open.");
        return port;
    }
}
