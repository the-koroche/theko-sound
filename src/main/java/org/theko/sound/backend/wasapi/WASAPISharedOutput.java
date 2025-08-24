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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioConverter;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;

/**
 * {@code WASAPISharedOutput} is an implementation of the {@link AudioOutputBackend} interface
 * that provides audio output backend functionality using the Windows Audio Session API (WASAPI) in shared mode.
 * 
 * @see WASAPISharedBackend
 * @see WASAPIExclusiveOutput
 *
 * @author Theko
 * @since 2.3.2
 */
public sealed class WASAPISharedOutput extends WASAPISharedBackend implements AudioOutputBackend permits WASAPIExclusiveOutput {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedOutput.class);

    private long outputContextPtr;

    private boolean isOpen = false;
    private boolean isStarted = false;
    private int bufferSize = -1;
    private AudioFormat audioFormat = null;
    private AudioPort port = null;

    @Override
    public synchronized void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        if (isOpen()) throw new AudioBackendException("Backend is already open.");
        if (port == null) throw new IllegalArgumentException("Port is null.");
        if (port.getFlow() != AudioFlow.OUT) throw new IllegalArgumentException("Port is not an output port.");
        if (port.getLink() == null) throw new IllegalArgumentException("Port link is null.");
        if (audioFormat == null) throw new IllegalArgumentException("Audio format is null.");
        if (audioFormat.isBigEndian()) throw new AudioBackendException("Big endian audio format is not supported.");
        if (bufferSize <= 0) throw new IllegalArgumentException("Buffer size is less than or equal to zero.");

        if (!isInitialized()) {
            logger.debug("Initializing WASAPI.");
            initialize();
        }
        logger.debug("Opening output port: {} with audio format: {} and buffer size: {}", port, audioFormat, bufferSize);
        openOut(port, audioFormat, bufferSize);

        this.bufferSize = bufferSize;
        this.audioFormat = audioFormat;
        this.port = port;
        isOpen = true;
    }

    /**
     * Opens the output port with the specified audio format and buffer size.
     * Overriden by {@link WASAPIExclusiveOutput}, which uses exclusive mode.
     * 
     * @param port The output port to open.
     * @param audioFormat The audio format to use for the output.
     * @param bufferSize The buffer size to use for the output.
     * @throws AudioBackendException If an error occurs while opening the output port.
     */
    protected void openOut(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        openOut0(false, port, audioFormat, bufferSize);
    }

    @Override
    public void open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        this.open(port, audioFormat, audioFormat.getByteRate() / 4);
    }

    @Override
    public boolean isOpen() throws AudioBackendException {
        return super.isInitialized() && isOpen && outputContextPtr != 0;
    }

    @Override
    public void close() throws AudioBackendException {
        if (isOpen || outputContextPtr != 0) closeOut0();
        isOpen = false;
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
        startOut0();
        isStarted = true;
    }

    @Override
    public void stop() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot stop. Backend is not open.");
        if (!isStarted) return;
        stopOut0();
        isStarted = false;
    }

    @Override
    public void flush() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot flush. Backend is not open.");
        flushOut0();
    }

    @Override
    public void drain() throws AudioBackendException, BackendNotOpenException {
        if (!isOpen()) throw new BackendNotOpenException("Cannot drain. Backend is not open.");
        drainOut0();
    }

    @Override
    public int write(byte[] data, int offset, int length) throws AudioBackendException, BackendNotOpenException {
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
        int bufferSize = this.bufferSize;
        try {
            int nativeBufferSize = getBufferSizeOut0();
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
        long latency = (long) ((bufferSize / (float) audioFormat.getSampleRate()) * 1000000);
        try {
            long nativeLatency = getMicrosecondLatency0();
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
            AudioPort nativePort = getCurrentAudioPort0();
            if (nativePort == null) {
                return port;
            }
            return nativePort;
        } catch (AudioBackendException ex) {
            logger.error("Error getting current audio port", ex);
            return port;
        }
    }
    
    protected synchronized native void openOut0(boolean isExclusive, AudioPort port, AudioFormat audioFormat, int bufferSize);
    private synchronized native void closeOut0();
    private synchronized native void startOut0();
    private synchronized native void stopOut0();
    private synchronized native void flushOut0();
    private synchronized native void drainOut0();
    private synchronized native int writeOut0(byte[] data, int offset, int length);
    private synchronized native int availableOut0();
    private synchronized native int getBufferSizeOut0();
    private synchronized native long getFramePositionOut0();
    private synchronized native long getMicrosecondLatency0();
    private synchronized native AudioPort getCurrentAudioPort0();
}
