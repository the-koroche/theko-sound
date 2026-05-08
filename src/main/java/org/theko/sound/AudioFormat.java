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

import java.io.Serializable;
import java.util.Objects;

import org.theko.sound.util.FormatUtilities;

/**
 * Represents an audio format with various properties such as sample rate, bit depth,
 * number of channels, encoding type, and endianness. This class provides utility
 * methods to analyze and manipulate audio format properties.
 *
 * <p>AudioFormat instances are immutable and include computed properties such as
 * frame size (bytes per frame) and byte rate (bytes per second).
 *
 * <p>Supported encodings are defined in the {@link Encoding} enum, which includes
 * PCM (signed, unsigned, and float), ULAW, and ALAW formats.
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
 * @since 0.1.0-beta
 * @author Theko
 */
public class AudioFormat implements Serializable {

    /**
     * Builder class for creating AudioFormat instances.
     * @since 0.3.1-beta
     */
    public static class Builder {

        private int sampleRate = 44100;
        private int bitsPerSample = 16;
        private int channels = 2;
        private Encoding encoding = Encoding.PCM_SIGNED;
        private boolean bigEndian = false;

        /**
         * Sets the sample rate for the audio format.
         * @param sampleRate Sample rate in Hz
         * @return This builder
         */
        public AudioFormat.Builder rate(int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        /**
         * Sets the number of bits per sample for the audio format.
         * @param bitsPerSample Number of bits
         * @return This builder
         */
        public AudioFormat.Builder bits(int bitsPerSample) {
            this.bitsPerSample = bitsPerSample;
            return this;
        }

        /**
         * Sets the number of audio channels for the audio format.
         * @param channels Number of channels
         * @return This builder
         */
        public AudioFormat.Builder channels(int channels) {
            this.channels = channels;
            return this;
        }

        /**
         * Sets the endianness for the audio format.
         * @param bigEndian True for big endian, false for little endian
         * @return This builder
         */
        public AudioFormat.Builder bigEndian(boolean bigEndian) {
            this.bigEndian = bigEndian;
            return this;
        }

        /**
         * Sets the encoding type for the audio format.
         * Changes the number of bits per sample based on the selected encoding.
         * @param encoding Encoding type
         * @return This builder
         */
        public AudioFormat.Builder encoding(Encoding encoding) {
            this.encoding = encoding;
            switch (encoding) {
                case PCM_SIGNED:    return this;
                case PCM_UNSIGNED:  return bits(8);
                case PCM_FLOAT:     return bits(Math.max(bitsPerSample, 32));
                case ULAW: case ALAW: return bits(8);
                default:
                    throw new IllegalArgumentException("Unsupported encoding: " + encoding);
            }
        }

        /**
         * Sets the audio format to mono, e.g. 1 channel.
         * @return This builder
         */
        public AudioFormat.Builder mono() {return channels(1); }

        /**
         * Sets the audio format to stereo, e.g. 2 channels.
         * @return This builder
         */
        public AudioFormat.Builder stereo() { return channels(2); }

        /**
         * Sets the encoding to unsigned PCM.
         * @return This builder
         */
        public AudioFormat.Builder unsigned() { return encoding(Encoding.PCM_UNSIGNED); }

        /**
         * Sets the encoding to signed PCM.
         * @return This builder
         */
        public AudioFormat.Builder signed() { return encoding(Encoding.PCM_SIGNED); }

        /**
         * Sets the encoding to float PCM.
         * @return This builder
         */
        public AudioFormat.Builder pcmFloat() { return encoding(Encoding.PCM_FLOAT); }

        /**
         * Sets the encoding to ULAW.
         * @return This builder
         */
        public AudioFormat.Builder ulaw() { return encoding(Encoding.ULAW); }

        /**
         * Sets the encoding to ALAW.
         * @return This builder
         */
        public AudioFormat.Builder alaw() { return encoding(Encoding.ALAW); }

        /**
         * Sets the endianness to little endian.
         * @return This builder
         */
        public AudioFormat.Builder littleEndian() { return bigEndian(false); }

        /**
         * Sets the endianness to big endian.
         * @return This builder
         */
        public AudioFormat.Builder bigEndian() { return bigEndian(true); }

        /**
         * Builds the audio format.
         * @return The audio format
         */
        public AudioFormat build() { return new AudioFormat(sampleRate, bitsPerSample, channels, encoding, bigEndian); }
    }

    /**
     * Returns a new builder for creating AudioFormat instances.
     * @return The builder
     */
    public static final AudioFormat.Builder builder() { return new AudioFormat.Builder(); }

    private static final int BITS_PER_SEC_PRECISION = 2; // Precision for formatting bits per second

    /** 8 kHz, 8-bit, mono, unsigned PCM (very low quality, small size). */
    public static final AudioFormat LOWEST_QUALITY_FORMAT =
            new AudioFormat(8000, 8, 1, Encoding.PCM_UNSIGNED, false);

    /** 22.05 kHz, 8-bit, mono, unsigned PCM (low quality, small size). */
    public static final AudioFormat LOW_QUALITY_FORMAT =
            new AudioFormat(22050, 8, 1, Encoding.PCM_UNSIGNED, false);

    /** 44.1 kHz, 16-bit, stereo, signed PCM (CD-quality audio). */
    public static final AudioFormat NORMAL_QUALITY_FORMAT =
            new AudioFormat(44100, 16, 2, Encoding.PCM_SIGNED, false);

    /** 48 kHz, 16-bit, stereo, signed PCM (studio-quality audio). */
    public static final AudioFormat HIGH_QUALITY_FORMAT =
            new AudioFormat(48000, 16, 2, Encoding.PCM_SIGNED, false);

    /** 48 kHz, 32-bit, stereo, float PCM (high-fidelity, suitable for DSP processing). */
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
     * <p>This enum defines the basic encoding formats for audio samples.
     *
     * <ul>
     *     <li>{@link #PCM_UNSIGNED} Pulse-code modulation, unsigned integer samples.</li>
     *     <li>{@link #PCM_SIGNED} Pulse-code modulation, signed integer samples.</li>
     *     <li>{@link #PCM_FLOAT} Pulse-code modulation, floating-point samples.</li>
     *     <li>{@link #ULAW} 8-bit logarithmic μ-law encoding (commonly used in telephony).</li>
     *     <li>{@link #ALAW} 8-bit logarithmic A-law encoding (commonly used in telephony).</li>
     * </ul>
     */
    public enum Encoding {
        /** PCM unsigned integer samples. */
        PCM_UNSIGNED,

        /** PCM signed integer samples. */
        PCM_SIGNED,

        /** PCM floating-point samples. */
        PCM_FLOAT,

        /** 8-bit μ-law encoded samples. */
        ULAW,

        /** 8-bit A-law encoded samples. */
        ALAW
    }

    /**
     * Constructs an AudioFormat instance with computed frame size and byte rate.
     *
     * @param sampleRate    The sample rate in Hz (must be positive)
     * @param bitsPerSample Bits per sample (must be positive)
     * @param channels      Number of audio channels (must be positive)
     * @param encoding      The encoding type (must not be null)
     * @param bigEndian     True for big-endian byte order, false for little-endian
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public AudioFormat(int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian) {
        this(sampleRate, bitsPerSample, channels, encoding, bigEndian,
             (bitsPerSample / 8) * channels,
             sampleRate * ((bitsPerSample / 8) * channels));
    }

    /**
     * Constructs an AudioFormat instance with all parameters explicitly specified.
     *
     * @param sampleRate    The sample rate in Hz
     * @param bitsPerSample Bits per sample
     * @param channels      Number of audio channels
     * @param encoding      The encoding type
     * @param bigEndian     True for big-endian byte order
     * @param frameSize     Bytes per frame
     * @param byteRate      Bytes per second
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public AudioFormat(int sampleRate, int bitsPerSample, int channels, Encoding encoding, boolean bigEndian, int frameSize, int byteRate) {
        if (sampleRate <= 0) throw new IllegalArgumentException("Sample rate must be positive.");
        if (bitsPerSample <= 0) throw new IllegalArgumentException("Bits per sample must be positive.");
        if (channels <= 0) throw new IllegalArgumentException("Number of channels must be positive.");
        if (frameSize <= 0) throw new IllegalArgumentException("Frame size must be positive.");
        if (byteRate <= 0) throw new IllegalArgumentException("Byte rate must be positive.");

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
    public int getSampleRate() { return sampleRate; }

    /** @return Bits per sample. */
    public int getBitsPerSample() { return bitsPerSample; }

    /** @return Bytes per sample. */
    public int getBytesPerSample() { return bitsPerSample / 8; }

    /** @return The number of audio channels. */
    public int getChannels() { return channels; }

    /** @return The encoding type. */
    public Encoding getEncoding() { return encoding; }

    /** @return True if big-endian, false if little-endian. */
    public boolean isBigEndian() { return bigEndian; }

    /** @return Bytes per frame. */
    public int getFrameSize() { return frameSize; }

    /** @return Bytes per second. */
    public int getByteRate() { return byteRate; }

    // Utility methods
    /** @return True if stereo (2 channels). */
    public boolean isStereo() { return channels == 2; }

    /** @return True if mono (1 channel). */
    public boolean isMono() { return channels == 1; }

    /** @return True if bit depth is greater than 16-bit (high resolution). */
    public boolean isHighResolution() { return bitsPerSample > 16; }

    /** @return True if the format is lossless (PCM-based encoding). */
    public boolean isLossless() {
        return encoding == Encoding.PCM_SIGNED || encoding == Encoding.PCM_UNSIGNED || encoding == Encoding.PCM_FLOAT;
    }

    /**
     * Checks if another AudioFormat instance has the same format.
     *
     * @param other Another AudioFormat instance
     * @return True if the formats match, false otherwise
     */
    public boolean isSameFormat(AudioFormat other) {
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
     * @param newEncoding The new encoding
     * @return A new AudioFormat instance with the specified encoding
     */
    public AudioFormat convertTo(Encoding newEncoding) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, newEncoding, bigEndian, frameSize, byteRate);
    }

    /**
     * Returns a new AudioFormat with a different endianness.
     *
     * @param newEndian True for big-endian, false for little-endian
     * @return A new AudioFormat instance with the specified endianness
     */
    public AudioFormat withEndian(boolean newEndian) {
        return new AudioFormat(sampleRate, bitsPerSample, channels, encoding, newEndian, frameSize, byteRate);
    }

    // Overridden methods
    @Override
    public String toString() {
        return String.format("AudioFormat{%d Hz, %d-bit, %d channels, %s, %s-endian, %s}",
                sampleRate, bitsPerSample, channels,
                (encoding != null ? encoding.name() : "N/A"),
                bigEndian ? "big" : "little",
                FormatUtilities.formatBits(byteRate * 8, BITS_PER_SEC_PRECISION) + "ps"
            );
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
