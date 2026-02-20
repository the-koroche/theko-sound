/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.theko.sound.util.FormatUtilities;

/**
 * Represents a measure of audio data, such as a number of frames, samples, bytes, or seconds.
 * It can be created using the static methods {@code ofFrames}, {@code ofSamples}, {@code ofBytes}, or {@code ofSeconds},
 * and can be converted to other units using the methods {@code getFrames}, {@code getSamples}, {@code getBytes}, or {@code getSeconds}.
 * 
 * @since 2.4.0
 * @author Theko
 */
public class AudioMeasure {

    private static final Pattern VALUE_PATTERN =
        Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([a-z]*)", Pattern.CASE_INSENSITIVE);

    private final long longVal; // for frames, samples, bytes
    private final double timeVal; // for seconds
    private final Unit unit;
    private AudioFormat audioFormat;

    /**
     * Represents the unit of audio measure.
     */
    public enum Unit {
        FRAMES, SAMPLES, BYTES, SECONDS;

        @Override
        public String toString() {
            return switch (this) {
                case FRAMES -> "frames";
                case SAMPLES -> "samples";
                case BYTES -> "bytes";
                case SECONDS -> "seconds";
            };
        }
    }

    private AudioMeasure(long longVal, double timeVal, Unit unit, AudioFormat format) {
        this.longVal = longVal;
        this.timeVal = timeVal;
        this.unit = unit;
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
        return new AudioMeasure(frames, 0, Unit.FRAMES, null);
    }

    /**
     * Creates an audio measure for the specified number of samples.
     * 
     * @param samples The number of samples.
     * @return An audio measure for the specified number of samples.
     */
    public static AudioMeasure ofSamples(long samples) {
        return new AudioMeasure(samples, 0, Unit.SAMPLES, null);
    }

    /**
     * Creates an audio measure for the specified number of bytes.
     * 
     * @param bytes The number of bytes.
     * @return An audio measure for the specified number of bytes.
     */
    public static AudioMeasure ofBytes(long bytes) {
        return new AudioMeasure(bytes, 0, Unit.BYTES, null);
    }

    /**
     * Creates an audio measure for the specified number of seconds.
     * 
     * @param seconds The number of seconds.
     * @return An audio measure for the specified number of seconds.
     */
    public static AudioMeasure ofSeconds(double seconds) {
        return new AudioMeasure(0, seconds, Unit.SECONDS, null);
    }

    /**
     * Creates an {@code AudioMeasure} from a string value.
     * <p>
     * The input must match the format:
     * <pre>

     * [number][unit]
     * </pre>
     * where:
     * <ul>
     * <li><b>number</b> - a non-negative integer or decimal value (e.g. {@code 123}, {@code 12.5})</li>
     * <li><b>unit</b> - an optional sequence of letters without spaces</li>
     * </ul>
     * There must be no space between the number and the unit.
     *
     * <p>Examples of valid inputs:
     * <ul>
     * <li>{@code "123"} (interpreted as {@code 123 frames})</li>
     * <li>{@code "123frames"}</li>
     * <li>{@code "12.5smp"}</li>
     * <li>{@code "2048bytes"}</li>
     * <li>{@code "3.0seconds"}</li>
     * </ul>
     *
     * <p>The following units are recognized (case-insensitive):
     * <ul>
     * <li>{@code frames}: {@code f}, {@code frm}, {@code frms}, {@code frame}, {@code frames}</li>
     * <li>{@code samples}: {@code smp}, {@code sample}, {@code samples}</li>
     * <li>{@code bytes}: {@code b}, {@code byte}, {@code bytes}</li>
     * <li>{@code seconds}: {@code s}, {@code sec}, {@code second}, {@code seconds}</li>
     * </ul>
     *
     * <p>If the string does not match the expected format or contains an invalid
     * number or unit, an {@link IllegalArgumentException} is thrown.
     *
     * @param value the string to parse
     * @return a corresponding {@code AudioMeasure}
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    public static AudioMeasure of(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Value must not be null or empty.");

        value = value.trim();

        Matcher matcher = VALUE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid format '" + value + "'.");
        }

        String valueStr = matcher.group(1);
        String unitStr = matcher.group(2).toLowerCase(Locale.US);

        double numericValue;
        try {
            numericValue = Double.parseDouble(valueStr);
            if (numericValue < 0.0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid numeric value '" + valueStr + "'.");
        }

        AudioMeasure.Unit unit = switch (unitStr) {
            case "", "f", "frm", "frms", "frame", "frames" -> AudioMeasure.Unit.FRAMES;
            case "smp", "samples", "sample" -> AudioMeasure.Unit.SAMPLES;
            case "b", "byte", "bytes" -> AudioMeasure.Unit.BYTES;
            case "s", "sec", "second", "seconds" -> AudioMeasure.Unit.SECONDS;
            default -> throw new IllegalArgumentException("Invalid unit '" + unitStr + "'.");
        };

        return switch (unit) {
            case FRAMES, SAMPLES, BYTES -> AudioMeasure.of((long)numericValue, unit);
            case SECONDS -> AudioMeasure.of(numericValue, unit);
        };
    }

    public static AudioMeasure of(long value, Unit unit) {
        return new AudioMeasure(value, (double)value, unit, null);
    }

    public static AudioMeasure of(double value, Unit unit) {
        return new AudioMeasure((long)value, value, unit, null);
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
        switch (unit) {
            case FRAMES: return longVal;
            case SAMPLES: return longVal / audioFormat.getChannels();
            case BYTES: return longVal / (audioFormat.getChannels() * audioFormat.getBytesPerSample());
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate());
            default: throw new IllegalStateException("Unexpected unit: " + unit);
        }
    }

    /**
     * Returns the value of this audio measure in samples.
     * 
     * @return The value of this audio measure in samples.
     */
    public long getSamples() {
        checkFormat();
        switch (unit) {
            case FRAMES: return longVal * audioFormat.getChannels();
            case SAMPLES: return longVal;
            case BYTES: return longVal / audioFormat.getBytesPerSample();
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate() * audioFormat.getChannels());
            default: throw new IllegalStateException("Unexpected unit: " + unit);
        }
    }

    /**
     * Returns the value of this audio measure in bytes.
     * 
     * @return The value of this audio measure in bytes.
     */
    public long getBytes() {
        checkFormat();
        switch (unit) {
            case FRAMES: return longVal * audioFormat.getChannels() * audioFormat.getBytesPerSample();
            case SAMPLES: return longVal * audioFormat.getBytesPerSample();
            case BYTES: return longVal;
            case SECONDS: return (long)(timeVal * audioFormat.getSampleRate() * audioFormat.getChannels() * audioFormat.getBytesPerSample());
            default: throw new IllegalStateException("Unexpected unit: " + unit);
        }
    }

    /**
     * Returns the value of this audio measure in seconds.
     * 
     * @return The value of this audio measure in seconds.
     */
    public double getSeconds() {
        checkFormat();
        switch (unit) {
            case FRAMES: return (double) longVal / audioFormat.getSampleRate();
            case SAMPLES: return (double) longVal / (audioFormat.getSampleRate() * audioFormat.getChannels());
            case BYTES: return (double) longVal / (audioFormat.getSampleRate() * audioFormat.getChannels() * audioFormat.getBytesPerSample());
            case SECONDS: return timeVal;
            default: throw new IllegalStateException("Unexpected unit: " + unit);
        }
    }

    @Override
    public String toString() {
        boolean isLongValue = unit != Unit.SECONDS;
        if (isLongValue) {
            return String.format("%d %s", longVal, unit);
        } else {
            return FormatUtilities.formatTime((long)(timeVal*FormatUtilities.SECONDS_NS), 4);
        }
    }

    public String getDetailedString() {
        if (audioFormat == null) {
            return String.format("AudioMeasure{%s}", toString());
        }
        long frames = getFrames();
        long samples = getSamples();
        long bytes = getBytes();
        double seconds = getSeconds();

        return String.format("AudioMeasure{%d frames, %d samples, %d bytes, %s}",
                frames, samples, bytes,
                FormatUtilities.formatTime((long)(seconds*FormatUtilities.SECONDS_NS), 4));
    }
}
