package org.theko.sound;

import java.util.Objects;

public class AudioFormat {
    private final int sampleRate;
    private final int bitsPerSample;
    private final int channels;
    private final Encoding encoding;
    private final boolean bigEndian;

    // Computed properties (now editable)
    private final int frameSize; // bytes per frame
    private final int byteRate;  // bytes per second

    public enum Encoding {
        PCM_UNSIGNED,
        PCM_SIGNED,
        PCM_FLOAT,
        ULAW,
        ALAW
    }

    /** Constructor with calculated frameSize and byteRate */
    public AudioFormat(int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian) {
        this(sampleRate, bitsPerSample, channels, encoding, bigEndian, (bitsPerSample / 8) * channels, sampleRate * ((bitsPerSample / 8) * channels));
    }

    /** Full constructor with all arguments */
    public AudioFormat(int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian, int frameSize, int byteRate) {
        if (sampleRate <= 0 || bitsPerSample <= 0 || channels <= 0 || frameSize <= 0 || byteRate <= 0) {
            throw new IllegalArgumentException("Invalid audio format parameters");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("Encoding cannot be null");
        }

        this.sampleRate = sampleRate;
        this.bitsPerSample = bitsPerSample;
        this.channels = channels;
        this.encoding = encoding;
        this.bigEndian = bigEndian;
        this.frameSize = frameSize;
        this.byteRate = byteRate;
    }

    // Getters
    public int getSampleRate() { return sampleRate; }
    public int getBitsPerSample() { return bitsPerSample; }
    public int getBytesPerSample() { return bitsPerSample / 8; }
    public int getChannels() { return channels; }
    public Encoding getEncoding() { return encoding; }
    public boolean isBigEndian() { return bigEndian; }
    public int getFrameSize() { return frameSize; }
    public int getByteRate() { return byteRate; }

    // Utility methods
    public boolean isStereo() { return channels == 2; }
    public boolean isMono() { return channels == 1; }
    public boolean isHighResolution() { return bitsPerSample > 16; }
    public boolean isLossless() {
        return encoding == Encoding.PCM_SIGNED || encoding == Encoding.PCM_UNSIGNED || encoding == Encoding.PCM_FLOAT;
    }

    public boolean isSameFormat(AudioFormat other) {
        return this.sampleRate == other.sampleRate &&
                this.bitsPerSample == other.bitsPerSample &&
                this.channels == other.channels &&
                this.encoding == other.encoding &&
                this.bigEndian == other.bigEndian &&
                this.frameSize == other.frameSize &&
                this.byteRate == other.byteRate;
    }

    public AudioFormat convertTo(Encoding newEncoding) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, newEncoding, bigEndian, frameSize, byteRate);
    }

    public AudioFormat withEndian(boolean newEndian) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, encoding, newEndian, frameSize, byteRate);
    }

    // Overridden methods
    @Override
    public String toString() {
        return String.format("AudioFormat[%d Hz, %d-bit, %d channels, %s, %s-endian, %.2f KB/s]", 
                sampleRate, bitsPerSample, channels, encoding, bigEndian ? "big" : "little", byteRate / 1024.0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AudioFormat other = (AudioFormat) obj;
        return isSameFormat(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleRate, bitsPerSample, channels, encoding, bigEndian, frameSize, byteRate);
    }
}
