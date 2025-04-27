package org.theko.sound;

import java.util.Arrays;

/**
 * Utility class for converting audio data between different formats.
 */
public class AudioConverter {
    private AudioConverter() {
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
    public static byte[] convert(byte[] data, AudioFormat source, AudioFormat target) {
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
            samples = AudioResampler.resample(samples, target, (float) source.getSampleRate() / target.getSampleRate());
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
    private static float[][] convertChannels(float[][] samples, int sourceChannels, int targetChannels) {
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
    public static long microsecondsToFrames(long microseconds, AudioFormat format) {
        return (microseconds * format.getSampleRate()) / 1_000_000L;
    }

    /**
     * Converts a number of audio frames to the equivalent time duration in microseconds.
     *
     * @param frames The number of audio frames.
     * @param format The audio format.
     * @return The equivalent time duration in microseconds.
     */
    public static long framesToMicroseconds(long frames, AudioFormat format) {
        return (frames * 1_000_000L) / format.getSampleRate();
    }
}
