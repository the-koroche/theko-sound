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
import org.theko.sound.backends.AudioOutputBackend;
import org.theko.sound.backends.BackendNotOpenException;

/**
 * The {@code JavaSoundOutput} class is an implementation of the {@link AudioOutputBackend}
 * interface that uses Java Sound API to manage audio output. It extends the {@link JavaSoundBackend}
 * class and provides functionality for opening, closing, and controlling audio output lines.
 * 
 * <p>This class manages a {@link SourceDataLine} for audio playback and supports operations
 * such as starting, stopping, flushing, draining, and writing audio data. It also provides
 * methods to retrieve information about the audio line, such as buffer size, frame position,
 * and latency.
 * 
 * <p>Key features include:
 * <ul>
 *   <li>Opening and configuring audio output lines with specified {@link AudioPort} and {@link AudioFormat}.</li>
 *   <li>Support for querying the current audio port and checking the state of the audio line.</li>
 * </ul>
 * 
 * <p>Usage example:
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
 * such as unsupported audio formats or attempting to operate on a closed backend.
 * 
 * @see AudioOutputBackend
 * @see JavaSoundBackend
 * @see SourceDataLine
 * 
 * @since 0.1.0-beta
 * @author Theko
 */
public final class JavaSoundOutput extends JavaSoundBackend implements AudioOutputBackend {

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
    public AudioFormat open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
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
        } catch (LineUnavailableException ex) {
            throw new AudioBackendException("Failed to open audio output line.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            throw new AudioBackendException("Unsupported audio format.", ex);
        }
        return audioFormat;
    }

    @Override
    public AudioFormat open(AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        return open(port, audioFormat, audioFormat.getByteRate() / 4 /* 0.25 seconds */);
    }

    @Override
    public boolean isOpen() {
        return open && sourceDataLine != null && sourceDataLine.isOpen();
    }

    @Override
    public void close() throws AudioBackendException {
        if (sourceDataLine != null) {
            sourceDataLine.close();
            open = false;
        }
    }

    @Override
    public void start() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot start. Backend is not open.");
        }
        sourceDataLine.start();
    }

    @Override
    public void stop() throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.stop();
        }
    }

    @Override
    public void flush() throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.flush();
        }
    }

    @Override
    public void drain() throws AudioBackendException {
        if (isOpen()) {
            sourceDataLine.drain();
        }
    }

    @Override
    public int write(byte[] data, int offset, int length) throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot write. Backend is not open.");
        }
        return sourceDataLine.write(data, offset, length);
    }

    @Override
    public int available() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot check availability. Backend is not open.");
        }
        return sourceDataLine.available();
    }

    @Override
    public int getBufferSize() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get buffer size. Backend is not open.");
        }
        return sourceDataLine.getBufferSize();
    }

    @Override
    public long getFramePosition() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get frame position. Backend is not open.");
        }
        return sourceDataLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get microsecond position. Backend is not open.");
        }
        return sourceDataLine.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() throws BackendNotOpenException {
        if (!isOpen()) {
            throw new BackendNotOpenException("Cannot get latency. Backend is not open.");
        }
        return (long)((sourceDataLine.getBufferSize() * 1000000L) / sourceDataLine.getFormat().getFrameRate());
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return currentPort;
    }
}
