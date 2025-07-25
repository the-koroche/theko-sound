package org.theko.sound.samples;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import org.theko.sound.AudioConverter;
import org.theko.sound.AudioFormat;

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
 * @author Theko
 */
public class SampleConverter {

    /**
     * Converts raw audio byte data to normalized floating-point samples.
     * 
     * @param data The raw audio byte data.
     * @param audioFormat The audio format associated with the byte data.
     * @param volumes Optional volume multipliers for each channel. If not provided, the samples will be normalized to the range [-1, 1].
     * @return A 2D array of floating-point samples, where each row represents a channel and each column represents a sample.
     */
    public static float[][] toSamples(byte[] data, AudioFormat audioFormat, float... volumes) {
        int bytesPerSample = audioFormat.getSampleSizeInBytes();
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

        int sampleCount = data.length / bytesPerSample / channels;
        float[][] samples = new float[channels][sampleCount];
        ByteBuffer buffer = ByteBuffer.wrap(data).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // Выносим проверку кодировки из внутреннего цикла
        switch (audioFormat.getEncoding()) {
            case PCM_UNSIGNED:
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        samples[ch][i] = unsignedToFloat(buffer, bytesPerSample) * appliedVolumes[ch];
                    }
                }
                break;
                
            case PCM_SIGNED:
                // Оптимизация для распространенных размеров
                if (bytesPerSample == 2) {
                    ShortBuffer shortBuffer = buffer.asShortBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            float value = shortBuffer.get() / 32768.0f;
                            samples[ch][i] = value * appliedVolumes[ch];
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 2);
                } else if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            float value = floatBuffer.get();
                            samples[ch][i] = value * appliedVolumes[ch];
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 4);
                } else {
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            samples[ch][i] = signedToFloat(buffer, bytesPerSample) * appliedVolumes[ch];
                        }
                    }
                }
                break;
                
            case PCM_FLOAT:
                FloatBuffer floatBuffer = buffer.asFloatBuffer();
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float value = floatBuffer.get();
                        samples[ch][i] = value * appliedVolumes[ch];
                    }
                }
                buffer.position(buffer.position() + sampleCount * channels * 4);
                break;
                
            case ULAW:
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        samples[ch][i] = ulawToFloat(buffer) * appliedVolumes[ch];
                    }
                }
                break;
                
            case ALAW:
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        samples[ch][i] = alawToFloat(buffer) * appliedVolumes[ch];
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + audioFormat.getEncoding());
        }
        return samples;
    }

    /**
     * Convert floating-point samples to raw byte data.
     * 
     * @param samples A 2D array of floating-point samples, where each row represents a channel and each column represents a sample.
     * @param targetFormat The target audio format to convert to.
     * @param volumes Optional volume multipliers for each channel. If not provided, the samples will be normalized to the range [-1, 1].
     * @return A byte array containing the converted raw audio data.
     */
    public static byte[] fromSamples(float[][] samples, AudioFormat targetFormat, float... volumes) {
        int samplesLength = samples[0].length;
        int bytesPerSample = targetFormat.getSampleSizeInBytes();
        boolean isBigEndian = targetFormat.isBigEndian();
        int channels = targetFormat.getChannels();

        // Проверка согласованности каналов
        for (int i = 1; i < channels; i++) {
            if (samples[i].length != samplesLength) {
                throw new IllegalArgumentException("All channels must have the same length");
            }
        }

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

        byte[] data = new byte[samplesLength * bytesPerSample * channels];
        ByteBuffer buffer = ByteBuffer.wrap(data).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        // Выносим проверку кодировки из внутреннего цикла
        switch (targetFormat.getEncoding()) {
            case PCM_UNSIGNED:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = samples[ch][i] * appliedVolumes[ch];
                        floatToUnsigned(buffer, sample, bytesPerSample);
                    }
                }
                break;
                
            case PCM_SIGNED:
                // Оптимизация для распространенных размеров
                if (bytesPerSample == 2) {
                    ShortBuffer shortBuffer = buffer.asShortBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            float sample = samples[ch][i] * appliedVolumes[ch];
                            short value = (short) Math.max(Short.MIN_VALUE, 
                                    Math.min(Short.MAX_VALUE, Math.round(sample * 32768.0f)));
                            shortBuffer.put(value);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 2);
                } else if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            float sample = samples[ch][i] * appliedVolumes[ch];
                            floatBuffer.put(sample);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 4);
                } else {
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            float sample = samples[ch][i] * appliedVolumes[ch];
                            floatToSigned(buffer, sample, bytesPerSample);
                        }
                    }
                }
                break;
                
            case PCM_FLOAT:
                FloatBuffer floatBuffer = buffer.asFloatBuffer();
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = samples[ch][i] * appliedVolumes[ch];
                        floatBuffer.put(sample);
                    }
                }
                buffer.position(buffer.position() + samplesLength * channels * 4);
                break;
                
            case ULAW:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = samples[ch][i] * appliedVolumes[ch];
                        floatToUlaw(buffer, sample);
                    }
                }
                break;
                
            case ALAW:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        float sample = samples[ch][i] * appliedVolumes[ch];
                        floatToAlaw(buffer, sample);
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + targetFormat.getEncoding());
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

    private static float signedToFloat(ByteBuffer buffer, int bytesPerSample) {
        long value = 0;
        int bits = bytesPerSample * 8;
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
                    value = (b0 << 16) | ((b1 & 0xFF) << 8) | (b2 & 0xFF);
                    if ((b0 & 0x80) != 0) {
                        value |= 0xFF000000L;
                    }
                } else {
                    value = (b2 << 16) | ((b1 & 0xFF) << 8) | (b0 & 0xFF);
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
        long fullRange = 1L << (bits - 1);
        return (float) (value / (double) fullRange);
    }

    private static void floatToUnsigned (ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = Math.max(0f, Math.min(1f, sample));
        long value = (long) (sample * ((1L << (bytesPerSample * 8)) - 1));
        for (int i = bytesPerSample - 1; i >= 0; i--) {
            buffer.put((byte) ((value >> (i * 8)) & 0xFF));
        }
    }

    private static void floatToSigned(ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        int bits = bytesPerSample * 8;
        long fullRange = 1L << (bits - 1);
        long value = Math.round(sample * fullRange);
        long max = fullRange - 1;
        long min = -fullRange;
        value = Math.max(min, Math.min(max, value));

        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            for (int i = bytesPerSample - 1; i >= 0; i--) {
                buffer.put((byte) (value >>> (i * 8)));
            }
        } else {
            for (int i = 0; i < bytesPerSample; i++) {
                buffer.put((byte) (value >>> (i * 8)));
            }
        }
    }
}