package org.theko.sound;

import java.util.Arrays;

import org.theko.sound.resampling.AudioResampler;

/**
 * The AudioConverter class provides utility methods for converting audio data
 * between different formats, including channel manipulation, resampling, and
 * format conversion. It also includes methods for converting between time
 * durations and audio frames.
 *
 * <p>This class is designed to handle common audio conversion tasks while
 * ensuring that unsupported operations, such as improper multi-channel
 * manipulation, are prevented.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Convert audio data between different formats.</li>
 *   <li>Handle channel configuration changes (e.g., Mono to Stereo).</li>
 *   <li>Resample audio data to match different sample rates.</li>
 *   <li>Convert between time durations (in microseconds) and audio frames.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <p>All methods in this class are static and can be accessed directly without
 * creating an instance of the class.</p>
 *
 * <h2>Exceptions:</h2>
 * <p>Throws {@link IllegalArgumentException} if unsupported channel manipulation
 * is attempted.</p>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * AudioFormat sourceFormat = new AudioFormat(44100, 16, 2, true, false);
 * AudioFormat targetFormat = new AudioFormat(48000, 16, 1, true, false);
 * byte[] convertedData = AudioConverter.convert(audioData, sourceFormat, targetFormat);
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe as it does not maintain any internal state.</p>
 * 
 * @see AudioFormat
 * 
 * @since v1.1.0
 * 
 * @author Theko
 */
public class AudioConverter {

    private AudioConverter () {
    }

    /**
     * Converts audio data from one format to another.
     *
     * @param data   The raw audio data.
     * @param source The source audio format.
     * @param target The target audio format.
     * @return The converted audio data.
     * @throws IllegalArgumentException if unsupported channel manipulation is attempted.
     */
    public static byte[] convert (byte[] data, AudioFormat source, AudioFormat target) {
        if (source.isSameFormat(target)) return data;

        // Ensure that multi-channel audio conversion is not attempted improperly
        if (source.getChannels() > 2 && target.getChannels() != source.getChannels()) {
            throw new IllegalArgumentException("Unsupported channel manipulation: " + source.getChannels() + " -> " + target.getChannels());
        }

        // Convert raw byte data to floating-point samples
        float[][] samples = SampleConverter.toSamples(data, source);

        // Convert the number of audio channels if needed
        samples = convertChannels(samples, source.getChannels(), target.getChannels());

        // Resample if the sample rate differs between source and target formats
        if (source.getSampleRate() != target.getSampleRate()) {
            samples = AudioResampler.SHARED.resample(samples, (float) source.getSampleRate() / target.getSampleRate());
        }

        // Convert samples back to raw byte data in the target format
        return SampleConverter.fromSamples(samples, target);
    }

    /**
     * Converts audio channels between different configurations.
     *
     * @param samples        The audio sample data.
     * @param sourceChannels The number of channels in the source audio.
     * @param targetChannels The number of channels in the target audio.
     * @return The converted sample data with the appropriate number of channels.
     */
    private static float[][] convertChannels (float[][] samples, int sourceChannels, int targetChannels) {
        if (sourceChannels == targetChannels) return samples;

        float[][] newSamples = new float[targetChannels][samples[0].length];

        if (sourceChannels == 1 && targetChannels == 2) {
            // Convert Mono to Stereo by duplicating the mono channel
            for (int i = 0; i < samples[0].length; i++) {
                newSamples[0][i] = samples[0][i];
                newSamples[1][i] = samples[0][i];
            }
        } else if (sourceChannels == 2 && targetChannels == 1) {
            // Convert Stereo to Mono by averaging both channels
            for (int i = 0; i < samples[0].length; i++) {
                newSamples[0][i] = (samples[0][i] + samples[1][i]) * 0.5f;
            }
        } else if (targetChannels > sourceChannels) {
            // If increasing the number of channels, duplicate existing channels
            for (int i = 0; i < samples[0].length; i++) {
                for (int ch = 0; ch < targetChannels; ch++) {
                    newSamples[ch][i] = samples[ch % sourceChannels][i];
                }
            }
        } else {
            // If reducing channels, mix them down
            for (int i = 0; i < samples[0].length; i++) {
                float sum = 0;
                for (int ch = 0; ch < sourceChannels; ch++) {
                    sum += samples[ch][i];
                }
                float mixedValue = sum / sourceChannels;
                Arrays.fill(newSamples, mixedValue);
            }
        }

        return newSamples;
    }

    /**
     * Converts a time duration in microseconds to the equivalent number of audio frames.
     *
     * @param microseconds The time duration in microseconds.
     * @param format       The audio format.
     * @return The equivalent number of audio frames.
     */
    public static long microsecondsToFrames (long microseconds, int sampleRate) {
        return (microseconds * sampleRate) / 1_000_000L;
    }

    /**
     * Converts a number of audio frames to the equivalent time duration in microseconds.
     *
     * @param frames The number of audio frames.
     * @param format The audio format.
     * @return The equivalent time duration in microseconds.
     */
    public static long framesToMicroseconds (long frames, int sampleRate) {
        return (frames * 1_000_000L) / sampleRate;
    }

    public static long samplesToMicrosecond (long samples, int sampleRate) {
        return (long) (samples * 1000000.0 / sampleRate);
    }

    public static long samplesToMicrosecond (float[][] samples, int sampleRate) {
        long maxSamples = 0;
        for (int ch = 0; ch < samples.length; ch++) {
            maxSamples = Math.max(samples[ch].length, maxSamples);
        }
        return samplesToMicrosecond(maxSamples, sampleRate);
    }

    public static long microsecondsToSamples (long microseconds, int sampleRate) {
        return (long) (microseconds * sampleRate / 1000000.0);
    }
}
