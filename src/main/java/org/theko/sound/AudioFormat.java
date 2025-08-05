package org.theko.sound;

import java.io.Serializable;
import java.util.Objects;

import org.theko.sound.codec.AudioEncoding;
import org.theko.sound.utility.FormatUtilities;

/**
 * Represents an audio format with various properties such as sample rate, bit depth,
 * number of channels, encoding type, and endianness. This class provides utility 
 * methods to analyze and manipulate audio format properties.
 * 
 * <p>AudioFormat instances are immutable and include computed properties such as 
 * frame size (bytes per frame) and byte rate (bytes per second).</p>
 * 
 * <p>Supported encodings are defined in the {@link Encoding} enum, which includes 
 * PCM (signed, unsigned, and float), ULAW, and ALAW formats.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * AudioFormat format = new AudioFormat(44100, 16, 2, AudioFormat.Encoding.PCM_SIGNED, true);
 * System.out.println(format);
 * }</pre>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Validation of input parameters to ensure valid audio format configurations.</li>
 *   <li>Utility methods to check properties such as stereo, mono, high resolution, and lossless encoding.</li>
 *   <li>Support for creating new AudioFormat instances with modified encoding or endianness.</li>
 *   <li>Equality and hash code implementations for comparing audio formats.</li>
 * </ul>
 * 
 * @see Encoding
 * 
 * @since v1.0.0
 * @author Theko
 */
public class AudioFormat implements Serializable {
    
    public static final AudioFormat LOWEST_QUALITY_FORMAT = 
        new AudioFormat(8000, 8, 1, Encoding.PCM_UNSIGNED, false);
    public static final AudioFormat LOW_QUALITY_FORMAT = 
        new AudioFormat(22500, 8, 1, Encoding.PCM_UNSIGNED, false);
    public static final AudioFormat NORMAL_QUALITY_FORMAT = 
        new AudioFormat(44100, 16, 2, Encoding.PCM_SIGNED, false);
    public static final AudioFormat HIGH_QUALITY_FORMAT = 
        new AudioFormat(48000, 16, 2, Encoding.PCM_SIGNED, false);
    public static final AudioFormat ULTRA_QUALITY_FORMAT = 
        new AudioFormat(48000, 32, 2, Encoding.PCM_FLOAT, false);

    private final int sampleRate;
    private final int bitsPerSample;
    private final int channels;
    private final Encoding encoding;
    private final boolean bigEndian;
    
    // Computed properties
    private final int frameSize; // bytes per frame
    private final int byteRate;  // bytes per second

    /**
     * Supported audio encodings.
     */
    public enum Encoding {
        PCM_UNSIGNED,
        PCM_SIGNED,
        PCM_FLOAT,
        ULAW,
        ALAW
    }

    /**
     * Constructs an AudioFormat instance with computed frame size and byte rate.
     *
     * @param sampleRate    The sample rate in Hz (must be positive).
     * @param bitsPerSample Bits per sample (must be positive).
     * @param channels      Number of audio channels (must be positive).
     * @param encoding      The encoding type (must not be null).
     * @param bigEndian     True for big-endian byte order, false for little-endian.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public AudioFormat (int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian) {
        this(sampleRate, bitsPerSample, channels, encoding, bigEndian,
             (bitsPerSample / 8) * channels,
             sampleRate * ((bitsPerSample / 8) * channels));
    }

    /**
     * Constructs an AudioFormat instance with all parameters explicitly specified.
     *
     * @param sampleRate    The sample rate in Hz.
     * @param bitsPerSample Bits per sample.
     * @param channels      Number of audio channels.
     * @param encoding      The encoding type.
     * @param bigEndian     True for big-endian byte order.
     * @param frameSize     Bytes per frame.
     * @param byteRate      Bytes per second.
     * @throws IllegalArgumentException if any parameter is invalid.
     */
    public AudioFormat (int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian, int frameSize, int byteRate) {
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
    /** @return The sample rate in Hz. */
    public int getSampleRate () { return sampleRate; }

    /** @return Bits per sample. */
    public int getSampleSizeInBits () { return bitsPerSample; }

    /** @return Bytes per sample. */
    public int getSampleSizeInBytes () { return bitsPerSample / 8; }

    /** @return The number of audio channels. */
    public int getChannels () { return channels; }

    /** @return The encoding type. */
    public Encoding getEncoding () { return encoding; }

    /** @return True if big-endian, false if little-endian. */
    public boolean isBigEndian () { return bigEndian; }

    /** @return Bytes per frame. */
    public int getFrameSize () { return frameSize; }

    /** @return Bytes per second. */
    public int getByteRate () { return byteRate; }

    // Utility methods
    /** @return True if stereo (2 channels). */
    public boolean isStereo () { return channels == 2; }

    /** @return True if mono (1 channel). */
    public boolean isMono () { return channels == 1; }

    /** @return True if bit depth is greater than 16-bit (high resolution). */
    public boolean isHighResolution () { return bitsPerSample > 16; }

    /** @return True if the format is lossless (PCM-based encoding). */
    public boolean isLossless () {
        return encoding == Encoding.PCM_SIGNED || encoding == Encoding.PCM_UNSIGNED || encoding == Encoding.PCM_FLOAT;
    }

    /**
     * Checks if another AudioFormat instance has the same format.
     *
     * @param other Another AudioFormat instance.
     * @return True if the formats match, false otherwise.
     */
    public boolean isSameFormat (AudioFormat other) {
        return this.sampleRate == other.sampleRate &&
                this.bitsPerSample == other.bitsPerSample &&
                this.channels == other.channels &&
                this.encoding == other.encoding &&
                this.bigEndian == other.bigEndian &&
                this.frameSize == other.frameSize &&
                this.byteRate == other.byteRate;
    }

    /**
     * Returns a new AudioFormat with a different encoding.
     *
     * @param newEncoding The new encoding.
     * @return A new AudioFormat instance with the specified encoding.
     */
    public AudioFormat convertTo (Encoding newEncoding) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, newEncoding, bigEndian, frameSize, byteRate);
    }

    public AudioFormat convertTo(AudioEncoding newEncoding) {
        switch (newEncoding) {
            case PCM_UNSIGNED:
                return convertTo(Encoding.PCM_UNSIGNED);
            case PCM_SIGNED:
                return convertTo(Encoding.PCM_SIGNED);
            case PCM_FLOAT:
                return convertTo(Encoding.PCM_FLOAT);
            case ULAW:
                return convertTo(Encoding.ULAW);
            case ALAW:
                return convertTo(Encoding.ALAW);
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + newEncoding);
        }
    }

    /**
     * Returns a new AudioFormat with a different endianness.
     *
     * @param newEndian True for big-endian, false for little-endian.
     * @return A new AudioFormat instance with the specified endianness.
     */
    public AudioFormat withEndian (boolean newEndian) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, encoding, newEndian, frameSize, byteRate);
    }

    // Overridden methods
    @Override
    public String toString () {
        return String.format("AudioFormat{%d Hz, %d-bit, %d channels, %s, %s-endian, %s/s}",
                sampleRate, bitsPerSample, channels, encoding, bigEndian ? "big" : "little", FormatUtilities.formatBytes(byteRate, true));
    }

    @Override
    public boolean equals (Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AudioFormat other = (AudioFormat) obj;
        return isSameFormat(other);
    }

    @Override
    public int hashCode () {
        return Objects.hash(sampleRate, bitsPerSample, channels, encoding, bigEndian, frameSize, byteRate);
    }
}
