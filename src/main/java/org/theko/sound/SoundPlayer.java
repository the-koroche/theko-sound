package org.theko.sound;

import java.io.File;
import java.io.FileNotFoundException;

import org.theko.sound.direct.AudioDeviceException;

public class SoundPlayer extends SoundSource {
    private final AudioOutputLine aol;
    protected AudioPort audioPort;

    public SoundPlayer() {
        try {
            aol = new AudioOutputLine();
        } catch (AudioDeviceNotFoundException | AudioDeviceCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public void open(File file, int bufferSize, AudioPort port) throws FileNotFoundException {
        this.audioPort = port;
        super.open(file, bufferSize);
    }

    @Override
    protected void initializeAudioPipeline() throws UnsupportedAudioFormatException {
        super.initializeAudioPipeline();
        try {
            aol.open(audioPort, audioFormat, bufferSize);
            aol.setInput(getOutputLine());
        } catch (AudioDeviceException | AudioPortsNotFoundException | UnsupportedAudioFormatException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        super.start();
        aol.start();
    }

    @Override
    public void stop() {
        super.stop();
        aol.stop();
    }

    @Override
    public void setFramePosition(long frame) {
        if (!isOpen) return;

        frame = frame / audioFormat.getFrameSize() * audioFormat.getFrameSize();
    
        if (frame < 0) frame = 0;
        if (frame >= length) frame = length - 1;
    
        int buffer = (int) (frame / bufferSize);
        int remaining = (int) (frame % bufferSize);
    
        //stop();
        pendingFrameSeek = true;
        aol.flush();

        played = Math.min(buffer, audioData.length - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        pendingFrameSeek = false;
        //start();
    }

    @Override
    public long getFramePosition() {
        if (!isOpen) {
            return -1;
        }
        return (bufferSize * played + aol.available() + offset) / audioFormat.getFrameSize();
    }

    @Override
    public void close() {
        super.close();
        aol.close();
    }
}
