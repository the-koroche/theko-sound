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

package org.theko.sound.backend.wasapi;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioUnitsConverter;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;

/**
 * {@code WASAPISharedOutput} is an implementation of the {@link AudioOutputBackend} interface
 * that provides audio output backend functionality using the Windows Audio Session API (WASAPI) in shared mode.
 * 
 * @see WASAPISharedBackend
 *
 * @author Theko
 * @since 2.3.2
 */
public final class WASAPISharedOutput extends WASAPISharedBackend implements AudioOutputBackend {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedOutput.class);

    private long outputContextPtr;

    private boolean isOpen = false;
    private boolean isStarted = false;
    private int bufferSize = -1;
    private AudioFormat audioFormat = null;
    private AudioPort port = null;

    @Override
    public synchronized AudioFormat open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        if (isOpen()) throw new AudioBackendException("Backend is already open.");
        if (port == null) {
            // Get default output port
            port = super.getDefaultPort(AudioFlow.OUT).orElse(null);
            if (port == null) throw new IllegalArgumentException("Port is null.");
        }
        if (port.getFlow() != AudioFlow.OUT) throw new IllegalArgumentException("Port is not an output port.");
        if (port.getLink() == null) throw new IllegalArgumentException("Port link is null.");
        if (audioFormat == null) throw new IllegalArgumentException("Audio format is null.");
        if (audioFormat.isBigEndian()) throw new AudioBackendException("Big endian audio format is not supported.");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size is less than or equal to zero.");

        if (!isInitialized()) {
            logger.debug("Initializing WASAPI.");
            initialize();
        }
        AtomicReference<AudioFormat> openedFormat = new AtomicReference<>();
        logger.debug("Opening output, port: {}, audio format: {}, buffer size: {}", port, audioFormat, bufferSize);
        this.outputContextPtr = nOpen(port, audioFormat, bufferSize, openedFormat);
        if (this.outputContextPtr == 0) throw new AudioBackendException("Failed to open output.");

        this.bufferSize = bufferSize;
        this.audioFormat = audioFormat;
        this.port = port;
        isOpen = true;

        return openedFormat.get();
    }

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        return this.open(port, audioFormat, audioFormat.getByteRate() / 4 /* 0.25 seconds */);
    }

    @Override
    public boolean isOpen() throws AudioBackendException {
        return super.isInitialized() && isOpen && outputContextPtr != 0;
    }

    /**
     * Checks if the audio output backend is started.
     * 
     * @return True if the backend is started and opened, false otherwise
     * @throws AudioBackendException If an error occurs during the operation.
     */
    public boolean isStarted() throws AudioBackendException {
        return isOpen() && isStarted;
    }

    @Override
    public void close() throws AudioBackendException {
        if (!isOpen()) {
            logger.debug("Cannot close. Backend is not open.");
            return;
        }
        if (isStarted) stop();
        if (isOpen || outputContextPtr != 0) nClose();
        if (super.isInitialized()) super.shutdown();
        isOpen = false;
        isStarted = false;
        bufferSize = -1;
        audioFormat = null;
        port = null;
        outputContextPtr = 0;
        logger.debug("Closed.");
    }

    @Override
    public void start() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot start. Backend is not open.");
        if (isStarted) return;
        nStart();
        isStarted = true;
    }

    @Override
    public void stop() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot stop. Backend is not open.");
        if (!isStarted) return;
        nStop();
        isStarted = false;
    }

    @Override
    public void flush() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot flush. Backend is not open.");
        nFlush();
    }

    @Override
    public void drain() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot drain. Backend is not open.");
        nDrain();
    }

    @Override
    public int write(byte[] data, int offset, int length) throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot write. Backend is not open.");
        return nWrite(data, offset, length);
    }

    @Override
    public int available() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get available. Backend is not open.");
        return nAvailable();
    }

    @Override
    public int getBufferSize() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get buffer size. Backend is not open.");
        int bufferSize = this.bufferSize;
        try {
            int nativeBufferSize = nGetBufferSize();
            if (nativeBufferSize == -1) {
                return bufferSize;
            }
            return nativeBufferSize;
        } catch (AudioBackendException e) {
            logger.error("Failed to get native buffer size.", e);
            return bufferSize;
        }
    }

    @Override
    public long getFramePosition() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get frame position. Backend is not open.");
        return nGetFramePosition();
    }

    @Override
    public long getMicrosecondPosition() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get microsecond position. Backend is not open.");
        return AudioUnitsConverter.framesToMicroseconds(getFramePosition(), audioFormat.getSampleRate());
    }

    @Override
    public long getMicrosecondLatency() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get latency. Backend is not open.");
        long latency = (long) ((bufferSize / (float) audioFormat.getSampleRate()) * 1000000);
        try {
            long nativeLatency = nGetMicrosecondLatency();
            if (nativeLatency == -1) {
                return latency;
            }
            return nativeLatency;
        } catch (AudioBackendException ex) {
            logger.error("Error getting latency", ex);
            return latency;
        }
    }

    @Override
    public AudioPort getCurrentAudioPort() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot get current audio port. Backend is not open.");
        AudioPort port = this.port;
        try {
            AudioPort nativePort = nGetCurrentAudioPort();
            if (nativePort == null) {
                return port;
            }
            return nativePort;
        } catch (AudioBackendException ex) {
            logger.error("Error getting current audio port", ex);
            return port;
        }
    }
    
    private synchronized native long nOpen(AudioPort port, AudioFormat audioFormat, int bufferSize, AtomicReference<AudioFormat> audioFormatRef);
    private synchronized native void nClose();
    private synchronized native void nStart();
    private synchronized native void nStop();
    private synchronized native void nFlush();
    private synchronized native void nDrain();
    private synchronized native int nWrite(byte[] data, int offset, int length);
    private synchronized native int nAvailable();
    private synchronized native int nGetBufferSize();
    private synchronized native long nGetFramePosition();
    private synchronized native long nGetMicrosecondLatency();
    private synchronized native AudioPort nGetCurrentAudioPort();
}
