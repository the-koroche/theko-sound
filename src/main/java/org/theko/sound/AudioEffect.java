package org.theko.sound;

/**
 * Represents an abstract audio effect that can be applied to audio data.
 */
public abstract class AudioEffect {
    /**
     * Defines the type of audio effect.
     */
    public enum Type {
        /** Effect applied in real-time during playback. */
        REALTIME,
        /** Effect applied as a post-processing step. */
        OFFLINE_PROCESSING
    }

    /** The audio format associated with this effect. */
    protected final AudioFormat audioFormat;
    /** The type of the effect (real-time or offline processing). */
    protected final Type type;

    /**
     * Constructs an AudioEffect with a specified type and audio format.
     *
     * @param type The type of the audio effect.
     * @param audioFormat The format of the audio data the effect will process.
     */
    public AudioEffect (Type type, AudioFormat audioFormat) {
        this.type = type;
        this.audioFormat = audioFormat;
    }

    /**
     * Processes the given audio samples.
     *
     * @param samples A 2D array of audio samples, where {@code samples[channel][sample]} represents the
     *                sample data for each channel.
     * @return The processed audio samples in the same format.
     */
    public abstract float[][] process(float[][] samples);

    /**
     * Gets the type of this audio effect.
     *
     * @return The type of the audio effect.
     */
    public Type getType() {
        return type;
    }
}
