package org.theko.sound.direct;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;

public interface AudioInputDevice extends AudioDevice, AutoCloseable {
    void open(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioDeviceException;
    void open(AudioPort port, AudioFormat audioFormat) throws AudioDeviceException;
    boolean isOpen() throws AudioDeviceException;
    void close() throws AudioDeviceException;
    void start() throws AudioDeviceException;
    void stop() throws AudioDeviceException;
    void flush() throws AudioDeviceException;
    void drain() throws AudioDeviceException;
    int read(byte[] buffer, int offset, int length) throws AudioDeviceException;
    int available() throws AudioDeviceException;

    int getBufferSize() throws AudioDeviceException;

    long getFramePosition() throws AudioDeviceException;
    long getMicrosecondPosition() throws AudioDeviceException;

    long getMicrosecondLatency() throws AudioDeviceException;
    AudioPort getCurrentAudioPort() throws AudioDeviceException;
}
