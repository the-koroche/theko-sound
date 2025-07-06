package org.theko.sound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The SampleConverter class provides utility methods for converting raw audio byte data
 * to normalized floating-point samples and vice versa. It supports various audio encodings
 * such as PCM (signed and unsigned), PCM_FLOAT, ULAW, and ALAW. The class also allows for
 * optional per-channel volume adjustments during the conversion process.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Convert raw byte data to normalized floating-point samples.</li>
 *   <li>Convert floating-point samples back to raw byte data.</li>
 *   <li>Support for multiple audio encodings and endianness.</li>
 *   <li>Optional per-channel volume multipliers for both conversions.</li>
 * </ul>
 *
 * <p>Supported Audio Encodings:</p>
 * <ul>
 *   <li>PCM_UNSIGNED</li>
 *   <li>PCM_SIGNED</li>
 *   <li>PCM_FLOAT</li>
 *   <li>ULAW</li>
 *   <li>ALAW</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * // Convert raw byte data to float samples
 * float[][] samples = SampleConverter.toSamples(byteData, audioFormat, 1.0f);
 *
 * // Convert float samples back to raw byte data
 * byte[] byteData = SampleConverter.fromSamples(samples, targetFormat, 0.8f);
 * </pre>
 *
 * <p>Note:</p>
 * <ul>
 *   <li>The volume multipliers array must have 0, 1, or the same number of elements as the number of channels.</li>
 *   <li>Unsupported audio encodings will result in an IllegalArgumentException.</li>
 * </ul>
 * 
 * @see AudioConverter
 * 
 * @since v1.2.0
 * 
 * @author Theko
 */
public class SampleConverter {

    /**
     * Converts raw byte data to normalized floating-point samples.
     *
     * @param data        The raw audio byte array.
     * @param audioFormat The format of the audio data.
     * @param volumes     Optional per-channel volume multipliers.
     * @return A 2D float array where each row represents a channel.
     * @throws IllegalArgumentException If volume array length is invalid.
     */
    public static float[][] toSamples (byte[] data, AudioFormat audioFormat, float... volumes) {
        int bytesPerSample = audioFormat.getBytesPerSample();
        boolean isBigEndian = audioFormat.isBigEndian();
        int channels = audioFormat.getChannels();

        // Validate volume multipliers
        if (volumes.length != 0 && volumes.length != 1 && volumes.length != channels) {
            throw new IllegalArgumentException("Volumes array must have 0, 1, or 'channels' elements.");
        }

        // Apply volume multipliers
        float[] appliedVolumes = new float[channels];
        if (volumes.length == 0) {
            Arrays.fill(appliedVolumes, 1.0f);
        } else if (volumes.length == 1) {
            Arrays.fill(appliedVolumes, volumes[0]);
        } else {
            System.arraycopy(volumes, 0, appliedVolumes, 0, channels);
        }

        float[][] samples = new float[channels][data.length / bytesPerSample / channels];
        ByteBuffer buffer = ByteBuffer.wrap(data).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // Read audio data and convert to float samples
        for (int i = 0; i < samples[0].length; i++) {
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                int pos = i * bytesPerSample * channels + channelIndex * bytesPerSample;
                buffer.position(pos);
                float volume = appliedVolumes[channelIndex];

                switch (audioFormat.getEncoding()) {
                    case PCM_UNSIGNED:
                        samples[channelIndex][i] = unsignedToFloat(buffer, bytesPerSample) * volume;
                        break;
                    case PCM_SIGNED:
                        samples[channelIndex][i] = signedToFloat(buffer, bytesPerSample) * volume;
                        break;
                    case PCM_FLOAT:
                        samples[channelIndex][i] = buffer.getFloat() * volume;
                        break;
                    case ULAW:
                        samples[channelIndex][i] = ulawToFloat(buffer) * volume;
                        break;
                    case ALAW:
                        samples[channelIndex][i] = alawToFloat(buffer) * volume;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported encoding: " + audioFormat.getEncoding());
                }
            }
        }
        return samples;
    }

    /**
     * Converts floating-point samples to raw byte data.
     *
     * @param samples      A 2D float array of audio samples.
     * @param targetFormat The target audio format.
     * @param volumes      Optional per-channel volume multipliers.
     * @return The encoded byte array.
     * @throws IllegalArgumentException If volume array length is invalid.
     */
    public static byte[] fromSamples (float[][] samples, AudioFormat targetFormat, float... volumes) {
        int samplesLength = samples[0].length;
        int bytesPerSample = targetFormat.getBytesPerSample();
        boolean isBigEndian = targetFormat.isBigEndian();
        int channels = targetFormat.getChannels();
        byte[] data = new byte[samplesLength * bytesPerSample * channels];

        // Validate volume multipliers
        if (volumes.length != 0 && volumes.length != 1 && volumes.length != channels) {
            throw new IllegalArgumentException("Volumes array must have 0, 1, or 'channels' elements.");
        }

        // Apply volume multipliers
        float[] appliedVolumes = new float[channels];
        if (volumes.length == 0) {
            Arrays.fill(appliedVolumes, 1.0f);
        } else if (volumes.length == 1) {
            Arrays.fill(appliedVolumes, volumes[0]);
        } else {
            System.arraycopy(volumes, 0, appliedVolumes, 0, channels);
        }

        // Convert samples to bytes
        ByteBuffer buffer = ByteBuffer.wrap(data).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < samplesLength; i++) {
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                float sample = samples[channelIndex][i] * appliedVolumes[channelIndex];

                switch (targetFormat.getEncoding()) {
                    case PCM_UNSIGNED:
                        floatToUnsigned(buffer, sample, bytesPerSample);
                        break;
                    case PCM_SIGNED:
                        floatToSigned(buffer, sample, bytesPerSample);
                        break;
                    case PCM_FLOAT:
                        buffer.putFloat(sample);
                        break;
                    case ULAW:
                        floatToUlaw(buffer, sample);
                        break;
                    case ALAW:
                        floatToAlaw(buffer, sample);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported encoding: " + targetFormat.getEncoding());
                }
            }
        }
        return data;
    }
    
    private static float ulawToFloat (ByteBuffer buffer) {
        int ulawByte = buffer.get() & 0xFF;
        int sign = (ulawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (ulawByte >> 4) & 0x07;
        int mantissa = ulawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (1 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * sample / 2147483648.0f));  // Return normalized value
    }

    private static float alawToFloat (ByteBuffer buffer) {
        int alawByte = buffer.get() & 0xFF;
        int sign = (alawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (alawByte >> 4) & 0x07;
        int mantissa = alawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (exponent == 0 ? 0 : 0x80 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * sample / 32768.0f));  // Return normalized value
    }

    private static void floatToUlaw (ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        sample *= 32768.0f;

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample / 2) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));
    }

    private static void floatToAlaw (ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        sample *= 32768.0f;

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample / 2) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));
    }

    private static float unsignedToFloat (ByteBuffer buffer, int bytesPerSample) {
        long value = 0;
        for (int i = 0; i < bytesPerSample; i++) {
            value = (value << 8) | (buffer.get() & 0xFF);
        }
        return (float) value / ((1L << (bytesPerSample * 8)) - 1);
    }

    private static float signedToFloat (ByteBuffer buffer, int bytesPerSample) {
        long value = 0;
        switch (bytesPerSample) {
            case 1:
                value = buffer.get();
                break;
            case 2:
                value = buffer.getShort();
                break;
            case 3:
                byte b0 = buffer.get();
                byte b1 = buffer.get();
                byte b2 = buffer.get();
                if (buffer.order() == ByteOrder.BIG_ENDIAN) {
                    value = (b0 << 16) | (b1 & 0xFF) << 8 | (b2 & 0xFF);
                    if ((b0 & 0x80) != 0) {
                        value |= 0xFF000000L;
                    }
                } else {
                    value = (b2 << 16) | (b1 & 0xFF) << 8 | (b0 & 0xFF);
                    if ((b2 & 0x80) != 0) {
                        value |= 0xFF000000L;
                    }
                }
                break;
            case 4:
                value = buffer.getInt();
                break;
            default:
                throw new IllegalArgumentException("Unsupported sample size: " + bytesPerSample);
        }
        int bits = bytesPerSample * 8;
        long max = (1L << (bits - 1)) - 1;
        return (float) value / max;
    }

    private static void floatToUnsigned (ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = Math.max(0f, Math.min(1f, sample));
        long value = (long) (sample * ((1L << (bytesPerSample * 8)) - 1));
        for (int i = bytesPerSample - 1; i >= 0; i--) {
            buffer.put((byte) ((value >> (i * 8)) & 0xFF));
        }
    }

    private static void floatToSigned (ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        
        // Calculate max value based on sample size
        long max = (1L << (bytesPerSample * 8 - 1)) - 1; // 32767 for 16 бит
        long min = -(1L << (bytesPerSample * 8 - 1));   // -32768 for 16 бит
    
        long value = Math.round(sample * max);
        if (value > max) value = max;
        if (value < min) value = min;
    
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            for (int i = bytesPerSample - 1; i >= 0; i--) {
                buffer.put((byte) ((value >> (i * 8)) & 0xFF));
            }
        } else {
            for (int i = 0; i < bytesPerSample; i++) {
                buffer.put((byte) ((value >> (i * 8)) & 0xFF));
            }
        }
    }
}