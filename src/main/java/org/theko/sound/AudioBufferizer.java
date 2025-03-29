package org.theko.sound;

import java.util.ArrayList;

/**
 * Utility class for buffering audio data into chunks of a specified size.
 */
public class AudioBufferizer {

    /**
     * Splits the input audio data into buffers of the specified size.
     *
     * @param data       The input byte array containing audio data.
     * @param format     The audio format (sample size in bits, channels, etc.).
     * @param bufferSize The size of each buffer in bytes (must take frame size into account).
     * @return A 2D byte array where each element is a chunk of audio data (buffer).
     * @throws IllegalArgumentException if data or format is null, or if bufferSize is less than or equal to zero.
     */
    public static byte[][] bufferize(byte[] data, AudioFormat format, int bufferSize) {
        // Check if the input data or format is null
        if (data == null || format == null) {
            throw new IllegalArgumentException("Data and format must not be null");
        }

        // Ensure that bufferSize is greater than zero
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        }

        // Calculate the size of one frame (in bytes)
        int frameSize = format.getFrameSize();
        if (frameSize <= 0) {
            throw new IllegalArgumentException("Invalid frame size: " + frameSize);
        }

        // Adjust the bufferSize to be a multiple of the frameSize
        if (bufferSize % frameSize != 0) {
            bufferSize = (bufferSize / frameSize) * frameSize;
        }

        // List to hold the resulting buffers (chunks)
        ArrayList<byte[]> buffers = new ArrayList<>();

        // Loop through the input data and split it into buffers of the specified size
        for (int i = 0; i < data.length; i += bufferSize) {
            int remaining = Math.min(bufferSize, data.length - i); // Remaining data to copy
            byte[] chunk = new byte[remaining]; // Create a new buffer for the chunk
            System.arraycopy(data, i, chunk, 0, remaining); // Copy data into the chunk
            buffers.add(chunk); // Add the chunk to the list of buffers
        }

        // Convert the list of buffers into a 2D byte array and return it
        return buffers.toArray(new byte[0][]);
    }
}
