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

package org.theko.sound;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.codec.AudioCodecNotFoundException;

public class SoundPlayer extends SoundSource {

    private static final Logger logger = LoggerFactory.getLogger(SoundPlayer.class);

    private final AudioOutputLayer outputLine;

    public SoundPlayer(AudioOutputLayer outputLine) {
        super();
        this.outputLine = outputLine;
        this.outputLine.setRootNode(this);
    }

    public SoundPlayer() {
        super();
        try {
            this.outputLine = new AudioOutputLayer();
            this.outputLine.setRootNode(this);
        } catch (AudioBackendCreationException | AudioBackendNotFoundException ex) {
            throw new RuntimeException("Audio backend creation failed.", ex);
        }
    }

    public SoundPlayer(File file) {
        this();
        try {
            this.open(file);
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    public SoundPlayer(String file) {
        this();
        try {
            this.open(new File(file));
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    public void open(File file, AudioPort port, int bufferSize) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(port, getAudioFormat(), bufferSize);
        } catch (AudioBackendException ex) {
            throw new AudioBackendException("Audio backend creation failed.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(new UnsupportedAudioFormatException("Unsupported audio format.", ex));
        }
    }

    public void open(File file, AudioPort port) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(port, getAudioFormat());
        } catch (AudioBackendException e) {
            throw new AudioBackendException("Audio backend creation failed.", e);
        }  catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(new UnsupportedAudioFormatException("Unsupported audio format.", ex));
        }
    }

    /**
     * Opens an audio file and decodes it into samples data.
     * Opens the audio output line with the specified format.
     * 
     * @param file The audio file to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    @Override public void open(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(null, getAudioFormat());
        } catch (AudioBackendException e) {
            throw new AudioBackendException("Audio backend creation failed.", e);
        } catch (UnsupportedAudioFormatException ex) {
            logger.error("Unsupported audio format.", ex);
            throw new RuntimeException(new UnsupportedAudioFormatException("Unsupported audio format.", ex));
        }
    }
    /**
     * Opens an audio file and decodes it into samples data.
     * Opens the audio output line with the specified format.
     * 
     * @param file The audio file path to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    @Override
    public void open(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    public boolean isOpen() {
        return outputLine.isOpen();
    }

    @Override
    public void start() {
        outputLine.start();
        super.start();
    }

    /**
     * Starts the playback of the sound source and waits for it to finish.
     */
    public void startAndWait() throws InterruptedException {
        start();
        while (super.isPlaying()) {
            Thread.sleep(100);
        }
        stop();
    }

    @Override
    public void stop() {
        outputLine.stop();
        super.stop();
    }
    
    @Override
    public void close() {
        super.close();
        outputLine.close();
        logger.info("SoundPlayer closed.");
    }
}