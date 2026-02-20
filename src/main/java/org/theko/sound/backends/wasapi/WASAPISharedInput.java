/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.backends.wasapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioUnitsConverter;
import org.theko.sound.backends.AudioBackendException;
import org.theko.sound.backends.AudioInputBackend;
import org.theko.sound.backends.BackendNotOpenException;

/**
 * {@code WASAPISharedInput} is an implementation of the {@link AudioInputBackend} interface
 * that provides audio input backend functionality using the Windows Audio Session API (WASAPI) in shared mode.
 * 
 * @see WASAPISharedBackend
 *
 * @author Theko
 * @since 0.2.3-beta
 */
public final class WASAPISharedInput extends WASAPISharedBackend implements AudioInputBackend {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedInput.class);

    private long inputContextPtr;

    private boolean isOpen = false;
    private boolean isStarted = false;
    private int bufferSize = -1;
    private AudioFormat audioFormat = null;
    private AudioPort port = null;

    @Override
    public synchronized AudioFormat open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        if (isOpen()) throw new AudioBackendException("Backend is already open.");
        if (port == null) {
            // Get default input port
            port = super.getDefaultPort(AudioFlow.IN).orElse(null);
            if (port == null) throw new IllegalArgumentException("Port is null.");
        }
        if (port.getFlow() != AudioFlow.IN) throw new IllegalArgumentException("Port is not an input port.");
        if (port.getLink() == null) throw new IllegalArgumentException("Port link is null.");
        if (audioFormat == null) throw new IllegalArgumentException("Audio format is null.");
        if (audioFormat.isBigEndian()) throw new AudioBackendException("Big endian audio format is not supported.");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size is less than or equal to zero.");

        if (!isInitialized()) {
            logger.debug("Initializing WASAPI.");
            initialize();
        }
        logger.debug("Opening input port: {}, audio format: {}, buffer size: {}", port, audioFormat, bufferSize);
        AudioFormat result = nOpen(port, audioFormat, bufferSize);

        this.bufferSize = bufferSize;
        this.audioFormat = audioFormat;
        this.port = port;
        isOpen = true;

        return result;
    }

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        return this.open(port, audioFormat, audioFormat.getByteRate() / 4 /* 0.25 seconds */);
    }

    @Override
    public boolean isOpen() throws AudioBackendException {
        return super.isInitialized() && isOpen && inputContextPtr != 0;
    }

    /**
     * Checks if the audio input backend is started.
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
        if (isOpen || inputContextPtr != 0) nClose();
        if (super.isInitialized()) super.shutdown();
        isOpen = false;
        isStarted = false;
        bufferSize = -1;
        audioFormat = null;
        port = null;
        inputContextPtr = 0;
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
    public int read(byte[] data, int offset, int length) throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot read. Backend is not open.");
        //return nRead(data, offset, length);
        int totalRead = 0;
        while (totalRead < length) {
            int read = nRead(data, offset + totalRead, length - totalRead);
            if (read == -1) break;
            totalRead += read;
        }
        return totalRead;
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
    
    private synchronized native AudioFormat nOpen(AudioPort port, AudioFormat audioFormat, int bufferSize);
    private synchronized native void nClose();
    private synchronized native void nStart();
    private synchronized native void nStop();
    private synchronized native void nFlush();
    private synchronized native void nDrain();
    private synchronized native int nRead(byte[] data, int offset, int length);
    private synchronized native int nAvailable();
    private synchronized native int nGetBufferSize();
    private synchronized native long nGetFramePosition();
    private synchronized native long nGetMicrosecondLatency();
    private synchronized native AudioPort nGetCurrentAudioPort();
}
