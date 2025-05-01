package org.theko.sound;

import org.theko.sound.direct.AudioDeviceException;

public interface AudioLine extends AutoCloseable {
    void open(AudioPort audioPort, AudioFormat audioFormat, int bufferSize);
    void open(AudioPort audioPort, AudioFormat audioFormat);
    void open(AudioFormat audioFormat) throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException;
    void close();
    void flush();
    void drain();
    void start();
    void stop();
    boolean isOpen();
    int available();
    int getBufferSize();
    long getFramePosition();
    long getMicrosecondPosition();
    long getMicrosecondLatency();
    AudioPort getCurrentAudioPort();
}
