package org.theko.sound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SampleConverter {

    public static float[][] toSamples(byte[] data, AudioFormat audioFormat, float... volumes) {
        int bytesPerSample = audioFormat.getBytesPerSample();
        boolean isBigEndian = audioFormat.isBigEndian();
        int channels = audioFormat.getChannels();

        float[][] samples = new float[channels][data.length / bytesPerSample / channels];
        ByteOrder order = isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

        if (volumes.length != 0 && volumes.length != 1 && volumes.length != channels) {
            throw new IllegalArgumentException("Volumes array must have 0, 1, or 'channels' elements.");
        }

        float[] appliedVolumes = new float[channels];
        if (volumes.length == 0) {
            Arrays.fill(appliedVolumes, 1.0f);
        } else if (volumes.length == 1) {
            Arrays.fill(appliedVolumes, volumes[0]);
        } else {
            System.arraycopy(volumes, 0, appliedVolumes, 0, channels);
        }

        for (int i = 0, offset = 0; i < samples[0].length; i++, offset += bytesPerSample * channels) {
            for (int channelIndex = 0; channelIndex < channels; channelIndex++) {
                ByteBuffer buffer = ByteBuffer.wrap(data, offset + channelIndex * bytesPerSample, bytesPerSample).order(order);
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

    public static byte[] fromSamples(float[][] samples, AudioFormat targetFormat, float... volumes) {
        int bytesPerSample = targetFormat.getBytesPerSample();
        boolean isBigEndian = targetFormat.isBigEndian();
        byte[] data = new byte[samples[0].length * bytesPerSample * targetFormat.getChannels()];
        ByteOrder order = isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

        if (volumes.length != 0 && volumes.length != 1 && volumes.length != targetFormat.getChannels()) {
            throw new IllegalArgumentException("Volumes array must have 0, 1, or 'channels' elements.");
        }

        float[] appliedVolumes = new float[targetFormat.getChannels()];
        if (volumes.length == 0) {
            Arrays.fill(appliedVolumes, 1.0f);
        } else if (volumes.length == 1) {
            Arrays.fill(appliedVolumes, volumes[0]);
        } else {
            System.arraycopy(volumes, 0, appliedVolumes, 0, appliedVolumes.length);
        }

        for (int i = 0, offset = 0; i < samples[0].length; i++, offset += bytesPerSample * targetFormat.getChannels()) {
            for (int channelIndex = 0; channelIndex < targetFormat.getChannels(); channelIndex++) {
                ByteBuffer buffer = ByteBuffer.wrap(data, offset + channelIndex * bytesPerSample, bytesPerSample).order(order);
                float volume = appliedVolumes[channelIndex];
                float sample = samples[channelIndex][i] * volume;

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
    
    // Преобразование ULAW в плавающую точку
    private static float ulawToFloat(ByteBuffer buffer) {
        int ulawByte = buffer.get() & 0xFF;
        int sign = (ulawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (ulawByte >> 4) & 0x07;
        int mantissa = ulawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (0x70 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * sample / 32768.0f));  // Возвращаем нормализованное значение
    }

    // Преобразование ALAW в плавающую точку
    private static float alawToFloat(ByteBuffer buffer) {
        int alawByte = buffer.get() & 0xFF;
        int sign = (alawByte & 0x80) != 0 ? -1 : 1;
        int exponent = (alawByte >> 4) & 0x07;
        int mantissa = alawByte & 0x0F;
        int sample = (mantissa << (exponent + 3)) + (0x70 << exponent);

        return Math.max(-1.0f, Math.min(1.0f, sign * sample / 32768.0f));  // Возвращаем нормализованное значение
    }

    // Преобразование плавающей точки в ULAW
    private static void floatToUlaw(ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));  // Ограничение в пределах [-1, 1]
        sample *= 32768.0f;  // Масштабируем в диапазон

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample / 2) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));  // Записываем ULAW байт
    }

    // Преобразование плавающей точки в ALAW
    private static void floatToAlaw(ByteBuffer buffer, float sample) {
        sample = Math.max(-1.0f, Math.min(1.0f, sample));  // Ограничение в пределах [-1, 1]
        sample *= 32768.0f;  // Масштабируем в диапазон

        int sign = sample < 0 ? 0x80 : 0x00;
        sample = Math.abs(sample);

        int exponent = 7;
        while (sample < 2048.0f && exponent > 0) {
            sample *= 2;
            exponent--;
        }

        int mantissa = (int) (sample / 2) & 0x0F;
        buffer.put((byte) (sign | (exponent << 4) | mantissa));  // Записываем ALAW байт
    }

    private static float unsignedToFloat(ByteBuffer buffer, int bytesPerSample) {
        long value = 0;
        for (int i = 0; i < bytesPerSample; i++) {
            value = (value << 8) | (buffer.get() & 0xFF);
        }
        return (float) value / ((1L << (bytesPerSample * 8)) - 1);
    }

    private static float signedToFloat(ByteBuffer buffer, int bytesPerSample) {
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

    private static void floatToUnsigned(ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = Math.max(0f, Math.min(1f, sample));
        long value = (long) (sample * ((1L << (bytesPerSample * 8)) - 1));
        for (int i = bytesPerSample - 1; i >= 0; i--) {
            buffer.put((byte) ((value >> (i * 8)) & 0xFF));
        }
    }

    private static void floatToSigned(ByteBuffer buffer, float sample, int bytesPerSample) {
        // Нормализация в диапазоне [-1.0, 1.0]
        sample = Math.max(-1.0f, Math.min(1.0f, sample));
        
        // Вычисляем максимальное значение в зависимости от разрядности
        long max = (1L << (bytesPerSample * 8 - 1)) - 1; // 32767 для 16 бит
        long min = -(1L << (bytesPerSample * 8 - 1));   // -32768 для 16 бит
    
        // Масштабирование с учетом смещения для отрицательных значений
        long value = Math.round(sample * max);
        if (value > max) value = max;
        if (value < min) value = min;
    
        // Запись в буфер с учетом порядка байтов
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