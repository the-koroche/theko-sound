package org.theko.sound;

public class AudioData {
    private final byte[] bytes;
    private final AudioFormat audioFormat;

    public AudioData(byte[] bytes, AudioFormat audioFormat) {
        this.bytes = bytes;
        this.audioFormat = audioFormat;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public AudioFormat getFormat() {
        return audioFormat;
    }
}
