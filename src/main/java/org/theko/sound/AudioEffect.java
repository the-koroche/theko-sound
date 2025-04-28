package org.theko.sound;

import org.theko.sound.control.BooleanController;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatController;

/**
 * Represents an abstract audio effect that can be applied to audio data.
 */
public abstract class AudioEffect implements AudioObject, Controllable {
    /**
     * Defines the type of audio effect.
     */
    public enum Type {
        /** Effect applied in real-time during playback. */
        REALTIME,
        /** Effect applied as a long time step. */
        OFFLINE_PROCESSING
    }

    /** The audio format associated with this effect. */
    protected final AudioFormat audioFormat;
    /** The type of the effect (real-time or offline processing). */
    protected final Type type;

    protected transient final BooleanController enableController;
    protected transient final FloatController mixController;

    /**
     * Constructs an AudioEffect with a specified type and audio format.
     *
     * @param type The type of the audio effect.
     * @param audioFormat The format of the audio data the effect will process.
     */
    public AudioEffect (Type type, AudioFormat audioFormat) {
        this.type = type;
        this.audioFormat = audioFormat;
        this.enableController = new BooleanController("Enable", true);
        this.mixController = new FloatController("Mix", 0, 1.0f, 1.0f);
    }

    public final float[][] callProcess(float[][] samples) {
        if (!enableController.getValue()) {
            return samples;
        }
    
        float[][] original = new float[samples.length][];
        for (int ch = 0; ch < samples.length; ch++) {
            original[ch] = new float[samples[ch].length];
            System.arraycopy(samples[ch], 0, original[ch], 0, samples[ch].length);
        }
    
        float[][] processed = process(samples);
    
        float mixValue = mixController.getValue();
        for (int ch = 0; ch < processed.length; ch++) {
            int minLength = Math.min(original[ch].length, processed[ch].length);
            for (int i = 0; i < minLength; i++) {
                samples[ch][i] = (original[ch][i] * (1.0f - mixValue)) + (processed[ch][i] * mixValue);
            }
        }
    
        return samples;
    }

    /**
     * Processes the given audio samples.
     *
     * @param samples A 2D array of audio samples, where {@code samples[channel][sample]} represents the
     *                sample data for each channel.
     * @return The processed audio samples in the same format.
     */
    protected abstract float[][] process(float[][] samples);

    /**
     * Gets the audio format of this audio effect.
     *
     * @return The audio format of the audio effect.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Gets the type of this audio effect.
     *
     * @return The type of the audio effect.
     */
    public Type getType() {
        return type;
    }

    public BooleanController getEnableController() {
        return enableController;
    }

    public FloatController getMixController() {
        return mixController;
    }

    /**
     * Does this audio effect is realtime.
     *
     * @return Is it realtime (true), or offline-processing (false).
     */
    public boolean isRealTime() {
        return type == AudioEffect.Type.REALTIME;
    }

    @Override
    public String toString() {
        return String.format("AudioEffect {Type: %s, AudioFormat: %s}", type, audioFormat);
    }
}
