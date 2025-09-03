/*
 * Copyright 2025 Alex Soloviov (aka Theko)
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

package org.theko.sound.samples;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.theko.sound.AudioFormat;

/**
 * Utility class for converting between raw PCM/encoded audio data and normalized floating-point samples.
 *
 * <p>This class provides methods to:</p>
 * <ul>
 *   <li>Convert raw audio byte data into normalized {@code float} samples in the range [-1.0, 1.0].</li>
 *   <li>Convert normalized {@code float} samples back into raw audio bytes.</li>
 *   <li>Apply optional per-channel volume multipliers during conversion.</li>
 * </ul>
 *
 * <p>The conversion methods support the following audio encodings:</p>
 * <ul>
 *   <li>{@link org.theko.sound.AudioFormat.Encoding#PCM_UNSIGNED}</li>
 *   <li>{@link org.theko.sound.AudioFormat.Encoding#PCM_SIGNED}</li>
 *   <li>{@link org.theko.sound.AudioFormat.Encoding#PCM_FLOAT}</li>
 *   <li>{@link org.theko.sound.AudioFormat.Encoding#ULAW}</li>
 *   <li>{@link org.theko.sound.AudioFormat.Encoding#ALAW}</li>
 * </ul>
 *
 * <p><b>Volume Multipliers:</b></p>
 * <ul>
 *   <li>The {@code volumes} array is optional.</li>
 *   <li>If provided, it must have either 0, 1, or the same number of elements as the number of channels.</li>
 *   <li>If fewer multipliers than channels are provided, remaining channels default to 1.0 (no scaling).</li>
 * </ul>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>Unsupported audio encodings will throw {@link IllegalArgumentException}.</li>
 *   <li>Methods that return arrays will allocate new memory for the result; 
 *       methods with preallocated arrays reuse existing memory to reduce GC pressure.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * byte[] pcmData = ...;
 * float[][] samples = SamplesConverter.toSamples(pcmData, audioFormat, 0.8f, 0.9f);
 * byte[] outputData = SamplesConverter.fromSamples(samples, audioFormat);
 * </pre>
 *
 * @see AudioFormat
 * @see ByteBuffer
 * 
 * @since 1.2.0
 * @author Theko
 */
public final class SamplesConverter {

    private static final double INV_32768 = 1.0 / 32768.0;
    private static final double INV_2147483648 = 1.0 / 2147483648.0;

    private SamplesConverter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Converts raw PCM byte data into a newly allocated 2D array of normalized floating-point samples.
     *
     * <p>This is a convenience wrapper around {@link #toSamples(byte[], float[][], AudioFormat, float...)},
     * which allocates the output array automatically.</p>
     *
     * <p>The returned array is organized as {@code [channels][frames]}, where the first
     * dimension is the channel index and the second is the frame index. Input data must
     * match the specified {@link AudioFormat}.</p>
     *
     * @param data the raw PCM audio data (must not be null).
     * @param audioFormat the format of the input data (must not be null).
     * @param volumes optional per-channel volume multipliers (defaults to 1.0).
     * @return a newly allocated 2D array of normalized floating-point samples [channels][frames].
     * @throws IllegalArgumentException if data or audioFormat is null, or data length is inconsistent with channels/sample size.
     */
    public static float[][] toSamples(byte[] data, AudioFormat audioFormat) {
        if (data == null || audioFormat == null) {
            throw new IllegalArgumentException("Data and audio format must not be null.");
        }

        int channels = audioFormat.getChannels();
        int samplesLength = data.length / audioFormat.getBytesPerSample() / channels;
        float[][] samples = new float[channels][samplesLength];
        toSamples(data, samples, audioFormat);

        return samples;
    }

    /**
     * Converts a 2D array of normalized floating-point samples into a newly allocated PCM byte array.
     *
     * <p>This is a convenience wrapper around {@link #fromSamples(float[][], byte[], AudioFormat, float...)},
     * which allocates the output array automatically.</p>
     *
     * <p>The input samples are expected in the range [-1.0, 1.0] and organized as [channels][frames].
     * Optional {@code volumes} act as per-channel multipliers applied before quantization.</p>
     *
     * @param samples 2D array of normalized floating-point samples [channels][frames] (must not be null).
     * @param audioFormat target PCM format (must not be null).
     * @return a newly allocated byte array containing PCM audio data.
     * @throws IllegalArgumentException if samples or audioFormat is null, or array dimensions are inconsistent.
     */
    public static byte[] fromSamples(float[][] samples, AudioFormat audioFormat) {
        if (samples == null || audioFormat == null) {
            throw new IllegalArgumentException("Samples and audio format must not be null.");
        }

        int channels = audioFormat.getChannels();
        int samplesLength = samples[0].length;
        byte[] data = new byte[samplesLength * channels * audioFormat.getBytesPerSample()];
        fromSamples(samples, data, audioFormat);

        return data;
    }

    /**
     * Converts raw PCM byte data into normalized floating-point audio samples.
     *
     * <p>Input data must contain a whole number of audio frames. Each frame
     * consists of {@code channels * bytesPerSample} bytes.</p>
     *
     * <p>Output samples are written into {@code outputSamples} as a 2D array,
     * where the first dimension is the channel index and the second dimension
     * is the frame index.</p>
     *
     * <pre>
     * outputSamples.length == channels
     * outputSamples[ch].length == data.length / bytesPerSample / channels
     * </pre>
     *
     * @param data the raw PCM audio data (not null).
     * @param outputSamples preallocated array of shape [channels][frames],
     *                      where frames = data.length / bytesPerSample / channels.
     * @param audioFormat format describing the PCM data (sample size, byte order, etc.).
     *
     * @throws IllegalArgumentException if data, audio format, or output samples are null, or if array dimensions are inconsistent with the input data.
     */
    public static void toSamples(byte[] data, float[][] outputSamples, AudioFormat audioFormat) {
        if (data == null || audioFormat == null || outputSamples == null) {
            throw new IllegalArgumentException("Data, audio format, and output samples must not be null.");
        }
        if (data.length == 0) {
            return;
        }

        int bytesPerSample = audioFormat.getBytesPerSample();
        boolean isBigEndian = audioFormat.isBigEndian();
        int channels = audioFormat.getChannels();

        int sampleCount = data.length / bytesPerSample / channels;
        if (outputSamples.length != channels || outputSamples[0] == null || outputSamples[0].length != sampleCount) {
            throw new IllegalArgumentException("Output samples array must have 'channels' rows and 'sampleCount' columns.");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(data).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        switch (audioFormat.getEncoding()) {
            case PCM_UNSIGNED:
                double invMax = 1.0 / ((1L << (bytesPerSample * 8)) - 1);
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        outputSamples[ch][i] = unsignedToFloat(buffer, bytesPerSample, invMax);
                    }
                }
                break;
                
            case PCM_SIGNED:
                if (bytesPerSample == 2) {
                    ShortBuffer shortBuffer = buffer.asShortBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            outputSamples[ch][i] = (float) (shortBuffer.get() * INV_32768);
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 2);
                } else if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            outputSamples[ch][i] = floatBuffer.get();
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 4);
                } else {
                    int bits = bytesPerSample * 8;
                    long fullRange = 1L << (bits - 1);
                    double invFullRange = 1.0 / fullRange;
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            outputSamples[ch][i] = signedToFloat(buffer, bytesPerSample, invFullRange);
                        }
                    }
                }
                break;
                
            case PCM_FLOAT:
                if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            outputSamples[ch][i] = floatBuffer.get();
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 4);
                } else if (bytesPerSample == 8) {
                    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
                    for (int i = 0; i < sampleCount; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            outputSamples[ch][i] = (float) doubleBuffer.get();
                        }
                    }
                    buffer.position(buffer.position() + sampleCount * channels * 8);
                }
                break;
                
            case ULAW:
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        outputSamples[ch][i] = ulawToFloat(buffer);
                    }
                }
                break;
                
            case ALAW:
                for (int i = 0; i < sampleCount; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        outputSamples[ch][i] = alawToFloat(buffer);
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + audioFormat.getEncoding());
        }
    }

    /**
     * Converts normalized floating-point samples into raw PCM byte data.
     *
     * <p>The input samples are expected in the range [-1.0, 1.0]. Values outside
     * this range may clip depending on the target encoding.</p>
     *
     * <p>The {@code samples} array must be organized as:</p>
     * <pre>
     * samples[channel][frame]
     * </pre>
     * where {@code channel} is the channel index (0-based) and {@code frame} is the sample index.
     *
     * <p>The {@code outputBytes} array must be preallocated by the caller with
     * the following length:</p>
     * <pre>
     * outputBytes.length == frames * channels * bytesPerSample
     * </pre>
     *
     * <p>Conversion respects the target {@link AudioFormat}'s encoding, sample
     * size, byte order, and channel count.</p>
     *
     * @param samples 2D array of floating-point audio data, organized as [channels][frames].
     * @param outputBytes preallocated byte array to store converted PCM data.
     * @param targetFormat target PCM format (encoding, sample size, endian, channels).
     *
     * @throws IllegalArgumentException if {@code outputBytes} length is inconsistent with
     *                                  {@code samples.length}, frame count, or {@code targetFormat}.
     */
    public static void fromSamples(float[][] samples, byte[] outputBytes, AudioFormat targetFormat) {
        if (samples == null || targetFormat == null || outputBytes == null) {
            throw new IllegalArgumentException("Samples, target format, and output bytes must not be null.");
        }
        if (samples.length == 0 || samples[0] == null || samples[0].length == 0) {
            return;
        }

        int samplesLength = samples[0].length;
        int bytesPerSample = targetFormat.getBytesPerSample();
        boolean isBigEndian = targetFormat.isBigEndian();
        int channels = targetFormat.getChannels();

        for (int i = 1; i < channels; i++) {
            if (samples[i].length != samplesLength) {
                throw new IllegalArgumentException("All channels must have the same length");
            }
        }

        int dataLength = samplesLength * channels * bytesPerSample;
        if (outputBytes.length != dataLength) {
            throw new IllegalArgumentException("Output byte array cannot be null and must be of length " + dataLength);
        }

        ByteBuffer buffer = ByteBuffer.wrap(outputBytes).order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        switch (targetFormat.getEncoding()) {
            case PCM_UNSIGNED:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        floatToUnsigned(buffer, samples[ch][i], bytesPerSample);
                    }
                }
                break;
                
            case PCM_SIGNED:
                if (bytesPerSample == 2) {
                    ShortBuffer shortBuffer = buffer.asShortBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            short value = (short) Math.max(Short.MIN_VALUE, 
                                    Math.min(Short.MAX_VALUE, Math.round(samples[ch][i] * 32768.0f)));
                            shortBuffer.put(value);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 2);
                } else if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            floatBuffer.put(samples[ch][i]);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 4);
                } else {
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            floatToSigned(buffer, samples[ch][i], bytesPerSample);
                        }
                    }
                }
                break;
                
            case PCM_FLOAT:
                if (bytesPerSample == 4) {
                    FloatBuffer floatBuffer = buffer.asFloatBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            floatBuffer.put(samples[ch][i]);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 4);
                } else if (bytesPerSample == 8) {
                    DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
                    for (int i = 0; i < samplesLength; i++) {
                        for (int ch = 0; ch < channels; ch++) {
                            doubleBuffer.put(samples[ch][i]);
                        }
                    }
                    buffer.position(buffer.position() + samplesLength * channels * 8);
                }
                break;
                
            case ULAW:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        floatToUlaw(buffer, samples[ch][i]);
                    }
                }
                break;
                
            case ALAW:
                for (int i = 0; i < samplesLength; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        floatToAlaw(buffer, samples[ch][i]);
                    }
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported encoding: " + targetFormat.getEncoding());
        }
    }
    
    private static float ulawToFloat(ByteBuffer buffer) {
        int ulawByte = buffer.get() & 0xFF;
        int sign = (ulawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (ulawByte >> 4) & 0x07;
        int mantissa = ulawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (1 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * (float) (sample * INV_2147483648)));  // Return normalized value
    }

    private static float alawToFloat(ByteBuffer buffer) {
        int alawByte = buffer.get() & 0xFF;
        int sign = (alawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (alawByte >> 4) & 0x07;
        int mantissa = alawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (exponent == 0 ? 0 : 0x80 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * (float) (sample * INV_32768)));  // Return normalized value
    }

    private static void floatToUlaw(ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        sample *= 32768.0f;

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample * 0.5f) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));
    }

    private static void floatToAlaw(ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        sample *= 32768.0f;

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample * 0.5f) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));
    }

    private static float unsignedToFloat(ByteBuffer buffer, int bytesPerSample, double invMax) {
        long value = 0;
        for (int i = 0; i < bytesPerSample; i++) {
            value = (value << 8) | (buffer.get() & 0xFF);
        }
        return (float) (value * invMax);
    }

    private static float signedToFloat(ByteBuffer buffer, int bytesPerSample, double invFullRange) {
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
        return (float) (value * invFullRange);
    }

    private static void floatToUnsigned(ByteBuffer buffer, float sample, int bytesPerSample) {
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