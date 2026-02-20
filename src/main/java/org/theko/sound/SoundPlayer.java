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

package org.theko.sound;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.ListenersManager;
import org.theko.sound.backends.AudioBackendCreationException;
import org.theko.sound.backends.AudioBackendException;
import org.theko.sound.backends.AudioBackendNotFoundException;
import org.theko.sound.codecs.AudioCodecNotFoundException;
import org.theko.sound.events.OutputLayerEvent;
import org.theko.sound.events.OutputLayerEventType;
import org.theko.sound.events.OutputLayerListener;

public class SoundPlayer extends SoundSource {

    private static final Logger logger = LoggerFactory.getLogger(SoundPlayer.class);

    private final AudioOutputLayer outputLayer;

    /**
     * Creates a new SoundPlayer with the specified audio output layer.
     * 
     * @param outputLine The audio output layer to use.
     */
    public SoundPlayer(AudioOutputLayer outputLine) {
        super();
        if (outputLine == null)
            throw new IllegalArgumentException("Audio output line cannot be null.");
        this.outputLayer = outputLine;
        this.outputLayer.setRootNode(this);
    }

    /**
     * Creates a new SoundPlayer with the default audio output layer.
     */
    public SoundPlayer() {
        super();
        try {
            this.outputLayer = new AudioOutputLayer();
            this.outputLayer.setRootNode(this);
        } catch (AudioBackendCreationException | AudioBackendNotFoundException ex) {
            throw new RuntimeException("Audio backend creation failed.", ex);
        }
    }

    /**
     * Creates a new SoundPlayer with the default audio output layer and opens the specified audio file.
     * 
     * @param file The audio file to open.
     */
    public SoundPlayer(File file) {
        this();
        try {
            this.open(file);
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    /**
     * Creates a new SoundPlayer with the default audio output layer and opens the specified audio file path.
     * 
     * @param file The audio file to open.
     */
    public SoundPlayer(String file) {
        this();
        try {
            this.open(new File(file));
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    /**
     * Opens the specified audio file and configures the audio output layer to use the specified audio port and buffer size.
     * 
     * @param file The audio file to open.
     * @param port The audio port to use.
     * @param bufferSize The buffer size to use.
     * @throws FileNotFoundException If the specified audio file is not found.
     * @throws AudioCodecNotFoundException If the specified audio file is not supported.
     */
    public void open(File file, AudioPort port, AudioMeasure bufferSize) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLayer.open(port, getAudioFormat(), bufferSize);
        } catch (AudioBackendException e) {
            throw new RuntimeException("Audio backend creation failed.", e);
        } catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(ex);
        } catch (AudioPortsNotFoundException ex) {
            logger.error("Default output audio port not found.", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Opens the specified audio file and configures the audio output layer to use the specified audio port.
     * 
     * @param file The audio file to open.
     * @param port The audio port to use.
     * @throws FileNotFoundException If the specified audio file is not found.
     * @throws AudioCodecNotFoundException If the specified audio file is not supported.
     */
    public void open(File file, AudioPort port) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLayer.open(port, getAudioFormat());
        } catch (AudioBackendException e) {
            throw new RuntimeException("Audio backend creation failed.", e);
        } catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(ex);
        } catch (AudioPortsNotFoundException ex) {
            logger.error("Default output audio port not found.", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Reopens the audio output layer with the same port and format as before.
     * 
     * @throws AudioBackendException If an error occurs while reopening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the buffer size is less than or equal to zero.
     */
    public void reopen() throws AudioBackendException, AudioPortsNotFoundException, UnsupportedAudioFormatException, IllegalArgumentException {
        this.outputLayer.reopen();
    }

    /**
     * Opens an audio file and decodes it into samples data.
     * Opens the audio output layer with the specified format.
     * 
     * @param file The audio file to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    @Override public void open(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLayer.open(null, getAudioFormat());
        } catch (AudioBackendException e) {
            throw new RuntimeException("Audio backend creation failed.", e);
        } catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(ex);
        } catch (AudioPortsNotFoundException ex) {
            logger.error("Default output audio port not found.", ex);
            throw new RuntimeException(ex);
        }
    }
    /**
     * Opens an audio file and decodes it into samples data.
     * Opens the audio output layer with the specified format.
     * 
     * @param file The audio file path to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    @Override
    public void open(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    /**
     * Checks if the audio output layer is open.
     * 
     * @return True if the audio output layer is open, false otherwise.
     */
    public boolean isOpen() {
        return outputLayer.isOpen();
    }

    @Override
    public void start() {
        outputLayer.start();
        super.start();
    }

    /**
     * Starts the playback of the sound source and waits for it to finish.
     */
    public void startAndWait() throws InterruptedException {
        start();
        waitUntilEnded();
    }

    /**
     * Waits for the playback of the sound source to finish.
     */
    public void waitUntilEnded() throws InterruptedException {
        while (super.isPlaying()) {
            Thread.sleep(100);
        }
    }

    @Override
    public void stop() {
        super.stop();
        try {
            outputLayer.stop();
        } catch (AudioBackendException | InterruptedException e) {
            throw new RuntimeException("Failed to stop audio output layer.", e);
        }
    }
    
    @Override
    public void close() {
        super.close();
        try {
            outputLayer.close();
        } catch (AudioBackendException | InterruptedException e) {
            throw new RuntimeException("Failed to close audio output layer.", e);
        }
        logger.info("SoundPlayer closed.");
    }

    public ListenersManager<OutputLayerEvent, OutputLayerListener, OutputLayerEventType> getOutputLayerListenersManager() {
        return outputLayer.getListenersManager();
    }
}