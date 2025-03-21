package org.theko.sound;

import java.util.ArrayList;

public class AudioBufferizer {

    /**
     * Разбивает входные аудиоданные на блоки (буферы) заданного размера.
     *
     * @param data       Входной массив байтов с аудиоданными.
     * @param format     Формат аудиоданных (sampleSizeInBits, channels и т.д.).
     * @param bufferSize Размер буфера в байтах (должен учитывать frameSize).
     * @return Двумерный массив, где каждый элемент - это буфер данных.
     */
    public static byte[][] bufferize(byte[] data, AudioFormat format, int bufferSize) {
        if (data == null || format == null) {
            throw new IllegalArgumentException("Data and format must not be null");
        }

        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        }

        // Вычисляем размер кадра (в байтах)
        int frameSize = format.getFrameSize();
        if (frameSize <= 0) {
            throw new IllegalArgumentException("Invalid frame size: " + frameSize);
        }

        // Округляем bufferSize до ближайшего кратного frameSize
        if (bufferSize % frameSize != 0) {
            bufferSize = (bufferSize / frameSize) * frameSize;
        }

        ArrayList<byte[]> buffers = new ArrayList<>();

        for (int i = 0; i < data.length; i += bufferSize) {
            int remaining = Math.min(bufferSize, data.length - i);
            byte[] chunk = new byte[remaining];
            System.arraycopy(data, i, chunk, 0, remaining);
            buffers.add(chunk);
        }

        return buffers.toArray(new byte[0][]);
    }
}
