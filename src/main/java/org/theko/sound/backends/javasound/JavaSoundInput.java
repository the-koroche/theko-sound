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

package org.theko.sound.backends.javasound;

import javax.sound.sampled.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backends.AudioBackendException;
import org.theko.sound.backends.AudioInputBackend;
import org.theko.sound.backends.BackendNotOpenException;

/**
 * The {@code JavaSoundInput} class is an implementation of the {@link AudioInputBackend}
 * interface that uses the Java Sound API to handle audio input operations.
 * It extends the {@link JavaSoundBackend} class and provides functionality for
 * managing audio input backends, such as opening, closing, starting, stopping,
 * and reading audio data from a {@link TargetDataLine}.
 *
 * <p>This class supports operations such as:
 * <ul>
 *   <li>Opening an audio input backend with a specified {@link AudioPort}, {@link AudioFormat}, and buffer size.</li>
 *   <li>Starting and stopping the audio input backend.</li>
 *   <li>Reading audio data from the input backend.</li>
 *   <li>Flushing and draining the audio input buffer.</li>
 *   <li>Retrieving information such as buffer size, frame position, and latency.</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * JavaSoundInput input = new JavaSoundInput();
 * input.open(audioPort, audioFormat, bufferSize);
 * input.start();
 * byte[] buffer = new byte[1024];
 * int bytesRead = input.read(buffer, 0, buffer.length);
 * input.stop();
 * input.close();
 * }</pre>
 *
 * <p>Note: This class throws {@link AudioBackendException} for various error
 * conditions, such as attempting to operate on a backend that is not open or
 * when the requested audio format is unsupported.
 *
 * @see AudioInputBackend
 * @see JavaSoundBackend
 * @see TargetDataLine
 * @see AudioPort
 * @see AudioFormat
 * 
 * @since 0.1.2-beta
 * @author Theko
 */
public final class JavaSoundInput extends JavaSoundBackend implements AudioInputBackend {

    /**
     * The target data line used for capturing audio data.
     */
    private TargetDataLine targetDataLine;

    /**
     * Indicates whether the audio input backend is open.
     */
    private boolean open;

    /**
     * The currently active audio port.
     */
    private AudioPort currentPort;

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, getJavaSoundAudioFormat(audioFormat), bufferSize);
            Mixer mixer = getMixerForPort(port);

            if (mixer == null || !mixer.isLineSupported(info)) {
                throw new AudioBackendException("Unsupported audio port or format.");
            }

            targetDataLine = (TargetDataLine) mixer.getLine(info);
            targetDataLine.open(getJavaSoundAudioFormat(audioFormat), bufferSize);
            this.currentPort = port;
            open = true;

            return audioFormat;
        } catch (LineUnavailableException ex) {
            throw new AudioBackendException("Failed to open audio output line.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            throw new AudioBackendException("Unsupported audio format.", ex);
        }
    }

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        return open(port, audioFormat, audioFormat.getByteRate() / 2 /* 0.5 seconds */);
    }

    @Override
    public boolean isOpen() {
        return open && targetDataLine != null && targetDataLine.isOpen();
    }

    @Override
    public void close() throws AudioBackendException {
        if (targetDataLine != null) {
            targetDataLine.close();
            open = false;
        }
    }

    @Override
    public void start() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot start. Backend is not open.");
        }
        targetDataLine.start();
    }

    @Override
    public void stop() throws AudioBackendException {
        if (isOpen()) {
            targetDataLine.stop();
        }
    }

    @Override
    public void flush() throws AudioBackendException {
        if (isOpen()) {
            targetDataLine.flush();
        }
    }

    @Override
    public void drain() throws AudioBackendException {
        if (isOpen()) {
            targetDataLine.drain();
        }
    }

    @Override
    public int read(byte[] data, int offset, int length) throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot write. Backend is not open.");
        }
        return targetDataLine.read(data, offset, length);
    }

    @Override
    public int available() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot check availability. Backend is not open.");
        }
        return targetDataLine.available();
    }

    @Override
    public int getBufferSize() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get buffer size. Backend is not open.");
        }
        return targetDataLine.getBufferSize();
    }

    @Override
    public long getFramePosition() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get frame position. Backend is not open.");
        }
        return targetDataLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get microsecond position. Backend is not open.");
        }
        return targetDataLine.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get latency. Backend is not open.");
        }
        return (long)((targetDataLine.getBufferSize() * 1000000L) / targetDataLine.getFormat().getFrameRate());
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return currentPort;
    }
}
