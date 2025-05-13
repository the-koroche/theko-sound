package org.theko.sound;

import java.util.ArrayList;

/**
 * The AudioBufferizer class provides a utility method to split audio data into smaller buffers
 * of a specified size, ensuring that the buffers align with the audio frame size.
 * 
 * <p>This class is designed to handle audio data in byte arrays and requires an AudioFormat
 * object to determine the frame size and other audio properties. The buffer size is adjusted
 * to be a multiple of the frame size to maintain audio data integrity.</p>
 * 
 * <h2>Usage:</h2>
 * <pre>
 * byte[] audioData = ...; // Input audio data
 * AudioFormat format = ...; // Audio format
 * int bufferSize = 1024; // Desired buffer size
 * byte[][] buffers = AudioBufferizer.bufferize(audioData, format, bufferSize);
 * </pre>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Splits audio data into chunks of a specified size.</li>
 *   <li>Ensures buffer size is a multiple of the audio frame size.</li>
 *   <li>Handles invalid input by throwing appropriate exceptions.</li>
 * </ul>
 * 
 * <h2>Exceptions:</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} if the input data or format is null.</li>
 *   <li>{@link IllegalArgumentException} if the buffer size is less than or equal to zero.</li>
 *   <li>{@link IllegalArgumentException} if the frame size in the audio format is invalid.</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe as it does not maintain any state and only provides a static method.</p>
 * 
 * @author Alex Soloviov
 */
public class AudioBufferizer {
    private AudioBufferizer () {
    }

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

    /**
     * Splits a 2D array of float samples into a 3D array of buffers (chunks) of the specified size.
     *
     * <p>This method is useful for processing audio data in smaller chunks for tasks like
     * streaming or applying audio effects.</p>
     *
     * @param samples     A 2D array of float samples, where each row represents a channel.
     * @param bufferSize  The size of each buffer (chunk) in samples.
     * @return A 3D array of float buffers, where each element is a chunk of audio data.
     * @throws IllegalArgumentException if samples is null, or if bufferSize is less than or equal to zero.
     */
    public static float[][][] bufferizeSamples(float[][] samples, int bufferSize) {
        // Validate input samples
        if (samples == null) {
            throw new IllegalArgumentException("Samples must not be null");
        }

        // Validate bufferSize
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        }

        int channels = samples.length;
        int totalSamples = samples[0].length;

        // Ensure all channels have the same length
        for (int c = 1; c < channels; c++) {
            if (samples[c].length != totalSamples) {
                throw new IllegalArgumentException("All channels must have the same length");
            }
        }

        ArrayList<float[][]> buffers = new ArrayList<>();

        // Split samples into chunks of bufferSize
        for (int i = 0; i < totalSamples; i += bufferSize) {
            int chunkSize = Math.min(bufferSize, totalSamples - i);
            float[][] chunk = new float[channels][chunkSize];

            // Copy samples for each channel into the chunk
            for (int c = 0; c < channels; c++) {
                System.arraycopy(samples[c], i, chunk[c], 0, chunkSize);
            }

            // Add the chunk to the list of buffers
            buffers.add(chunk);
        }

        // Convert the list of buffers into a 3D float array and return it
        return buffers.toArray(new float[0][][]);
    }
}
