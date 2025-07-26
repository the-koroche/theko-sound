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

    public SoundPlayer (AudioOutputLayer outputLine) {
        super();
        this.outputLine = outputLine;
            this.outputLine.setRootNode(this);
    }

    public SoundPlayer () {
        super();
        try {
            this.outputLine = new AudioOutputLayer();
            this.outputLine.setRootNode(this);
        } catch (AudioBackendCreationException | AudioBackendNotFoundException ex) {
            throw new RuntimeException("Audio backend creation failed.", ex);
        }
    }

    public SoundPlayer (File file) {
        this();
        try {
            this.open(file);
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    public SoundPlayer (String file) {
        this();
        try {
            this.open(new File(file));
        } catch (FileNotFoundException | AudioCodecNotFoundException e) {
            throw new RuntimeException("Failed to open audio file: " + file, e);
        }
    }

    public void open (File file, AudioPort port, int bufferSize) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(port, AudioFormat.NORMAL_QUALITY_FORMAT, bufferSize);
        } catch (AudioBackendException e) {
            throw new AudioBackendException("Audio backend creation failed.", e);
        }
    }

    public void open (File file, AudioPort port) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(port, AudioFormat.NORMAL_QUALITY_FORMAT);
        } catch (AudioBackendException e) {
            throw new AudioBackendException("Audio backend creation failed.", e);
        }
    }

    @Override public void open (File file) throws FileNotFoundException, AudioCodecNotFoundException {
        super.open(file);
        try {
            this.outputLine.open(null, AudioFormat.NORMAL_QUALITY_FORMAT);
        } catch (AudioBackendException e) {
            throw new AudioBackendException("Audio backend creation failed.", e);
        }
    }

    @Override
    public void open (String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    @Override
    public void start () {
        outputLine.start();
        super.start();
    }

    public void startAndWait () throws InterruptedException {
        start();
        while (super.isPlaying()) {
            Thread.sleep(100);
        }
        stop();
    }

    @Override
    public void stop () {
        outputLine.stop();
        super.stop();
    }
    
    @Override
    public void close () {
        super.close();
        outputLine.close();
        logger.info("SoundPlayer closed.");
    }
}