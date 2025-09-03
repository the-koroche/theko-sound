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

 package org.theko.sound;

/**
 * Represents a measure of audio data, such as a number of frames, samples, bytes, or seconds.
 * It can be created using the static methods {@code ofFrames}, {@code ofSamples}, {@code ofBytes}, or {@code ofSeconds},
 * and can be converted to other units using the methods {@code getFrames}, {@code getSamples}, {@code getBytes}, or {@code getSeconds}.
 * 
 * @since 2.4.0
 * @author Theko
 */
public class AudioMeasure {

    private final long longVal; // for frames, samples, bytes
    private final double timeVal; // for seconds
    private final Type type;
    private AudioFormat audioFormat;

    /**
     * Represents the type of audio measure.
     */
    public enum Type {
        FRAMES, SAMPLES, BYTES, SECONDS
    }

    private AudioMeasure(long longVal, double timeVal, Type type, AudioFormat format) {
        this.longVal = longVal;
        this.timeVal = timeVal;
        this.type = type;
        this.audioFormat = format;
    }

    /**
     * Sets the audio format for this audio measure.
     * 
     * @param format The audio format.
     * @return This audio measure.
     */
    public AudioMeasure onFormat(AudioFormat format) {
        if (format == null) throw new IllegalArgumentException("Audio format must not be null.");
        this.audioFormat = format;
        return this;
    }

    /**
     * Creates an audio measure for the specified number of frames.
     * 
     * @param frames The number of frames.
     * @return An audio measure for the specified number of frames.
     */
    public static AudioMeasure ofFrames(long frames) {
        return new AudioMeasure(frames, 0, Type.FRAMES, null);
    }

    /**
     * Creates an audio measure for the specified number of samples.
     * 
     * @param samples The number of samples.
     * @return An audio measure for the specified number of samples.
     */
    public static AudioMeasure ofSamples(long samples) {
        return new AudioMeasure(samples, 0, Type.SAMPLES, null);
    }

    /**
     * Creates an audio measure for the specified number of bytes.
     * 
     * @param bytes The number of bytes.
     * @return An audio measure for the specified number of bytes.
     */
    public static AudioMeasure ofBytes(long bytes) {
        return new AudioMeasure(bytes, 0, Type.BYTES, null);
    }

    /**
     * Creates an audio measure for the specified number of seconds.
     * 
     * @param seconds The number of seconds.
     * @return An audio measure for the specified number of seconds.
     */
    public static AudioMeasure ofSeconds(double seconds) {
        return new AudioMeasure(0, seconds, Type.SECONDS, null);
    }

    private void checkFormat() {
        if (audioFormat == null)
            throw new IllegalStateException("Audio format must be set before accessing this value.");
    }

    /**
     * Returns the value of this audio measure in frames.
     * 
     * @return The value of this audio measure in frames.
     */
    public long getFrames() {
        checkFormat();
        switch (type) {
            case FRAMES: return longVal;
            case SAMPLES: return longVal / audioFormat.getChannels();
            case BYTES: return longVal / (audioFormat.getChannels() * audioFormat.getBytesPerSample());
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate());
            default: throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    /**
     * Returns the value of this audio measure in samples.
     * 
     * @return The value of this audio measure in samples.
     */
    public long getSamples() {
        checkFormat();
        switch (type) {
            case FRAMES: return longVal * audioFormat.getChannels();
            case SAMPLES: return longVal;
            case BYTES: return longVal / audioFormat.getBytesPerSample();
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate() * audioFormat.getChannels());
            default: throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    /**
     * Returns the value of this audio measure in bytes.
     * 
     * @return The value of this audio measure in bytes.
     */
    public long getBytes() {
        checkFormat();
        switch (type) {
            case FRAMES: return longVal * audioFormat.getChannels() * audioFormat.getBytesPerSample();
            case SAMPLES: return longVal * audioFormat.getBytesPerSample();
            case BYTES: return longVal;
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate() * audioFormat.getChannels() * audioFormat.getBytesPerSample());
            default: throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    /**
     * Returns the value of this audio measure in seconds.
     * 
     * @return The value of this audio measure in seconds.
     */
    public double getSeconds() {
        checkFormat();
        switch (type) {
            case FRAMES: return (double) longVal / audioFormat.getSampleRate();
            case SAMPLES: return (double) longVal / (audioFormat.getSampleRate() * audioFormat.getChannels());
            case BYTES: return (double) longVal / (audioFormat.getSampleRate() * audioFormat.getChannels() * audioFormat.getBytesPerSample());
            case SECONDS: return timeVal;
            default: throw new IllegalStateException("Unexpected type: " + type);
        }
    }
}
