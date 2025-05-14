package org.theko.sound;

import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;

/**
 * The {@code AudioEffect} class represents an abstract base class for audio effects
 * that can process audio data in either real-time or offline modes. It provides
 * a framework for implementing custom audio effects with controls for enabling/disabling
 * the effect and adjusting the mix level between the original and processed audio.
 *
 * <p>Subclasses must implement the {@link #process(float[][])} method to define the
 * specific audio processing logic.</p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Supports real-time and offline processing modes via the {@link Type} enum.</li>
 *   <li>Provides a {@link BooleanControl} to enable or disable the effect.</li>
 *   <li>Includes a {@link FloatControl} to adjust the mix level between original and processed audio.</li>
 *   <li>Ensures non-destructive processing by preserving the original audio samples.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <p>To create a custom audio effect, extend this class and implement the {@link #process(float[][])} method.
 * Use the {@link #callProcess(float[][])} method to apply the effect to audio samples.</p>
 *
 * <h2>Thread Safety:</h2>
 * <p>This class is not thread-safe. Synchronization must be handled externally if used in a multi-threaded environment.</p>
 *
 * @see AudioObject
 * @see Controllable
 * @see BooleanControl
 * @see FloatControl
 * 
 * @since v1.2.0
 * 
 * @author Theko
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

    protected transient final BooleanControl enableControl;
    protected transient final FloatControl mixControl;

    /**
     * Constructs an AudioEffect with a specified type and audio format.
     *
     * @param type The type of the audio effect.
     * @param audioFormat The format of the audio data the effect will process.
     */
    public AudioEffect (Type type, AudioFormat audioFormat) {
        this.type = type;
        this.audioFormat = audioFormat;
        this.enableControl = new BooleanControl("Enable", true);
        this.mixControl = new FloatControl("Mix", 0, 1.0f, 1.0f);
    }

    public final float[][] callProcess(float[][] samples) {
        if (!enableControl.getValue()) {
            return samples;
        }
    
        float[][] original = new float[samples.length][];
        for (int ch = 0; ch < samples.length; ch++) {
            original[ch] = new float[samples[ch].length];
            System.arraycopy(samples[ch], 0, original[ch], 0, samples[ch].length);
        }
    
        float[][] processed = process(samples);
    
        float mixValue = mixControl.getValue();
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

    public BooleanControl getEnableControl() {
        return enableControl;
    }

    public FloatControl getMixControl() {
        return mixControl;
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
