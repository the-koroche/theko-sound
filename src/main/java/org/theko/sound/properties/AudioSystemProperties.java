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

package org.theko.sound.properties;

import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioMeasure;
import org.theko.sound.resampling.*;
import org.theko.sound.utility.FormatUtilities;
import org.theko.sound.utility.MathUtilities;
import org.theko.sound.utility.PlatformUtilities;
import org.theko.sound.utility.PlatformUtilities.Architecture;
import org.theko.sound.utility.PlatformUtilities.Platform;

/**
 * AudioSystemProperties holds the configuration properties for the audio system.
 * It includes thread types, priorities, resampling methods, mixer settings, and other.
 * 
 * This class is immutable and provides static access to the properties.
 * 
 * @since 2.0.0
 */
public final class AudioSystemProperties {

    /*
    * --- STRUCTURES / CLASSES ---
    * 
    * === ThreadType ===
    * Values: [virtual, platform]
    * Aliases:
    *   virtual  -> [v]
    *   platform -> [p]
    *
    * === ThreadConfig ===
    * Format: <type;optional>:<priority;optional>
    *   type      - Thread type (ThreadType), optional if priority is specified
    *   priority  - Thread priority (int, 0-10), optional if type is specified
    * Examples:
    *   -Dkey=platform:7   // Platform thread with priority 7
    *   -Dkey=p:10         // Alias for 'platform' with priority 10
    *   -Dkey=platform     // Platform thread with default priority
    *   -Dkey=virtual      // Virtual thread with default priority
    *   -Dkey=v            // Alias for 'virtual'
    *   -Dkey=4            // Default thread type with priority 4
    *
    * === AudioMeasure.Unit ===
    * Values: [frames, samples, bytes, seconds]
    * Aliases:
    *   frames  -> [frame]
    *   samples -> [sample, smp]
    *   bytes   -> [byte]
    *   seconds -> [second, sec]
    *
    * === AudioMeasure ===
    * Format: <value>:<unit;optional>
    *   value - Value (long or double)
    *   unit  - Unit type (AudioMeasure.Unit), optional, default = frames
    * Examples:
    *   -Dkey=512:frames    // 512 frames (default)
    *   -Dkey=2048:bytes    // 2048 bytes
    *   -Dkey=0.52:sec      // 0.52 seconds
    *   -Dkey=1024:smp      // 1024 samples (using alias)
    *   -Dkey=4096          // 4096 frames (default unit)
    *
    * === AudioResamplerConfig ===
    * Format: <method>:<quality;optional>
    *   method  - Resample method (class)
    *   quality - Quality (int > 0), optional
    * Examples:
    *   -Dkey=LinearResampleMethod          // Uses default quality
    *   -Dkey=LanczosResampleMethod:3       // Quality explicitly specified
    *   -Dkey=my.package.MyResampleMethod   // Full class name for non-default resampler
    */

    /*
    * --- PROPERTIES ---
    *
    * === Audio Output Layer ===
    * Property                                           | Type                   | Description / Default
    * ---------------------------------------------------|------------------------|-------------------------------------------
    * org.theko.sound.outputLayer.processing.thread      | ThreadConfig           | Processing thread configuration
    * org.theko.sound.outputLayer.processing.timeout     | int                    | Default timeout in milliseconds for stopping the processing thread and waiting for its termination.
    * org.theko.sound.outputLayer.output.thread          | ThreadConfig           | Output thread configuration
    * org.theko.sound.outputLayer.output.timeout         | int                    | Default timeout in milliseconds for stopping the output thread and waiting for its termination.
    * org.theko.sound.outputLayer.defaultBuffer          | AudioMeasure           | Default audio buffer size
    * org.theko.sound.outputLayer.defaultRingBuffers     | int (>= 1)             | Ring buffers count, used if not specified in open()
    * org.theko.sound.outputLayer.resampler              | AudioResamplerConfig   | Resampler method and quality
    * org.theko.sound.outputLayer.enableShutdownHook     | boolean                | Enables or disables shutdown hook on JVM exit
    * 
    * === Shared Resampler ===
    * Property                                           | Type                   | Description / Default
    * ---------------------------------------------------|------------------------|-------------------------------------------
    * org.theko.sound.resampler.shared                   | AudioResamplerConfig   | Resampler method and quality
    * 
    * === Audio Mixer ===
    * Property                                           | Type                   | Description / Default
    * ---------------------------------------------------|------------------------|-------------------------------------------
    * org.theko.sound.mixer.default.enableEffects        | boolean                | Enable or disable effects in every mixer by default
    * org.theko.sound.mixer.default.swapChannels         | boolean                | Swap stereo channels in every mixer by default
    * org.theko.sound.mixer.default.reversePolarity      | boolean                | Reverse polarity in every mixer by default
    * 
    * === Codecs ===
    * Codec     | Property                                           | Type                   | Description / Default
    * ----------|----------------------------------------------------|------------------------|-------------------------------------------
    * WAVE      | org.theko.sound.waveCodec.cleanTagText             | boolean                | Cleans every tag from 'LIST' chunk
    *
    * === Miscellaneous ===
    * Property                                           | Type                   | Description / Default
    * ---------------------------------------------------|------------------------|-------------------------------------------
    * org.theko.sound.automation.thread                  | ThreadConfig           | Thread config for each automation, and LFO
    * org.theko.sound.automation.updateTime              | int > 0                | Update time in millis for automation and LFO process updates
    * org.theko.sound.cleaner.thread                     | ThreadConfig           | Thread config for each Cleaner
    * org.theko.sound.effects.resampler                  | AudioResamplerConfig   | Default ResamplerEffect resampler
    * 
    */

    private static final Logger logger = LoggerFactory.getLogger(AudioSystemProperties.class);

    public enum ThreadType {
        VIRTUAL, PLATFORM;

        public static ThreadType from(String str) {
            return "virtual".equalsIgnoreCase(str) ? VIRTUAL : PLATFORM;
        }

        @Override
        public String toString() {
            return switch (this) {
                case PLATFORM -> "Platform";
                case VIRTUAL -> "Virtual";
                default -> "N/A";
            };
        }
    }

    public static class ThreadConfig {
        public final ThreadType threadType;
        public final int priority;

        public ThreadConfig(ThreadType threadType, int priority) {
            this.threadType = threadType;
            this.priority = priority;
        }

        @Override
        public String toString() {
            return "ThreadConfig{type=%s, priority=%d}".formatted(threadType, priority);
        }
    }

    public static class AudioResamplerConfig {
        public final ResampleMethod resampleMethod;
        public final int quality;

        public AudioResamplerConfig(ResampleMethod resampleMethod, int quality) {
            this.resampleMethod = resampleMethod;
            this.quality = quality;
        }

        @Override
        public String toString() {
            return "AudioResamplerConfig{method=%s, quality=%d}".formatted(
                resampleMethod.getClass().getSimpleName(), quality);
        }
    }

    /* Primitives parsing */

    private static String getString(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            logger.debug("Value {} is not set.", key);
            return defaultValue;
        }
        return value;
    }

    private static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid integer value for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private static int getIntInRange(String key, int min, int max, boolean clamp, int defaultValue) {
        int value;
        try {
            value = Integer.parseInt(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid integer value for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }

        if (value < min || value > max) {
            if (clamp) {
                value = MathUtilities.clamp(value, min, max);
            } else {
                return defaultValue;
            }
        }

        return value;
    }

    private static double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            logger.warn("Invalid floating-point value for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private static float getFloat(String key, float defaultValue) {
        return (float)getDouble(key, defaultValue);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        String lowerCaseValue = value.toLowerCase(Locale.US);
        switch (lowerCaseValue) {
            case "true", "yes", "on", "1", "enabled" -> { return true; }
            case "false", "no", "off", "0", "disabled" -> { return false; }
            default -> {
                logger.warn("Invalid boolean value '{}' for '{}'. Using default {}", value, key, defaultValue);
                return defaultValue;
            }
        }
    }

    /* Structures/Classes parsing */

    private static ThreadType getThreadType(String key, ThreadType defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        String lowerCaseValue = value.toLowerCase(Locale.US);
        switch (lowerCaseValue) {
            case "platform", "p" -> { return ThreadType.PLATFORM; }
            case "virtual", "v" -> { return ThreadType.VIRTUAL; }
            default -> {
                logger.warn("Invalid ThreadType value '{}' for '{}'. Using default {}", value, key, defaultValue);
                return defaultValue;
            }
        }
    }

    private static ThreadConfig getThreadConfig(String key, ThreadConfig defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String[] parts = value.trim().split(":", 2);
        String threadTypeStr = parts[0].trim();
        String priorityStr = (parts.length > 1) ? parts[1].trim() : null;

        if (threadTypeStr.isEmpty()) {
            logger.warn("Thread type is missing for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }

        ThreadType threadType = defaultValue.threadType;
        switch (threadTypeStr.toLowerCase(Locale.US)) {
            case "platform", "p" -> { threadType = ThreadType.PLATFORM; }
            case "virtual", "v" -> { threadType = ThreadType.VIRTUAL; }
            default -> {
                logger.warn("Invalid ThreadType value '{}' for '{}'. Using default {}", threadTypeStr, key, defaultValue);
                return defaultValue;
            }
        }

        int priority = defaultValue.priority;
        if (priorityStr != null && !priorityStr.isBlank()) {
            try {
                priority = MathUtilities.clamp(
                    Integer.parseInt(priorityStr),
                    Thread.MIN_PRIORITY, Thread.MAX_PRIORITY
                );
            } catch (NumberFormatException ex) {
                logger.warn("Invalid priority value for '{}'. Using default {}", key, priority);
            }
        }

        return new ThreadConfig(threadType, priority);
    }

    private static AudioMeasure getAudioMeasure(String key, AudioMeasure defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String[] parts = value.trim().split(":", 2);
        String valueStr = parts[0].trim();
        String unitStr = (parts.length > 1) ? parts[1].trim().toLowerCase(Locale.US) : "";

        if (valueStr.isEmpty()) {
            logger.warn("Value missing for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }

        Number numericValue;
        try {
            numericValue = Long.parseLong(valueStr);
            if (numericValue.longValue() <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e1) {
            try {
                numericValue = Double.parseDouble(valueStr);
                if (numericValue.doubleValue() <= 0.0) throw new NumberFormatException();
            } catch (NumberFormatException e2) {
                logger.warn("Invalid value '{}' for '{}'. Using default {}", valueStr, key, defaultValue);
                return defaultValue;
            }
        }

        AudioMeasure.Unit unit = switch (unitStr) {
            case "frames", "frame" -> AudioMeasure.Unit.FRAMES;
            case "samples", "sample", "smp" -> AudioMeasure.Unit.SAMPLES;
            case "bytes", "byte" -> AudioMeasure.Unit.BYTES;
            case "seconds", "second", "sec" -> AudioMeasure.Unit.SECONDS;
            default -> {
                logger.warn("Invalid unit '{}' for '{}'. Using default {}", unitStr, key, AudioMeasure.Unit.FRAMES.name());
                yield AudioMeasure.Unit.FRAMES;
            }
        };

        return switch (unit) {
            case FRAMES, SAMPLES, BYTES -> AudioMeasure.of(numericValue.longValue(), unit);
            case SECONDS -> AudioMeasure.of(numericValue.doubleValue(), unit);
        };
    }

    private static AudioResamplerConfig getAudioResamplerConfig(String key, AudioResamplerConfig defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String[] parts = value.trim().split(":", 2);
        String resampleMethodStr = parts[0].trim();
        String qualityStr = (parts.length > 1) ? parts[1].trim() : null;

        if (resampleMethodStr.isEmpty()) {
            logger.warn("Resample method missing for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }

        ResampleMethod resampleMethod;
        try {
            Class<?> clazz;
            try {
                // search in default resamplers package
                clazz = Class.forName("org.theko.sound.resampling." + resampleMethodStr);
            } catch (ClassNotFoundException e) {
                // if not found, search for full class
                clazz = Class.forName(resampleMethodStr);
            }

            if (!ResampleMethod.class.isAssignableFrom(clazz)) {
                logger.warn("Class '{}' is not a ResampleMethod, for '{}'. Using default {}", resampleMethodStr, key, defaultValue);
                return defaultValue;
            }
            resampleMethod = (ResampleMethod) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.warn("Failed to instantiate '{}' for '{}'. Using default {}", resampleMethodStr, key, defaultValue);
            logger.debug("Stack trace:", e);
            return defaultValue;
        }

        int quality = defaultValue.quality;
        if (qualityStr != null && !qualityStr.isEmpty()) {
            try {
                quality = Integer.parseInt(qualityStr);
            } catch (NumberFormatException ex) {
                logger.warn("Invalid quality '{}' for '{}'. Using default {}", qualityStr, key, defaultValue.quality);
                quality = defaultValue.quality;
            }
        }

        return new AudioResamplerConfig(resampleMethod, quality);
    }

    public static final Platform PLATFORM = PlatformUtilities.getPlatform();
    public static final Architecture ARCHITECTURE = PlatformUtilities.getArchitecture();

    public static final boolean IS_SUPPORTING_JAVA_21 = Runtime.version().feature() >= 21;
    public static final boolean IS_SUPPORTING_VIRTUAL_THREADS = IS_SUPPORTING_JAVA_21;

    public static final int CPU_AVAILABLE_CORES = Runtime.getRuntime().availableProcessors();
    public static final long TOTAL_MEMORY = Runtime.getRuntime().totalMemory();
    public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();

    public static final ThreadConfig AOL_PROCESSING_THREAD = getThreadConfig(
        "org.theko.sound.outputLayer.processing.thread", new ThreadConfig(ThreadType.PLATFORM, 7));

    public static final int AOL_PROCESSING_STOP_TIMEOUT = getIntInRange(
        "org.theko.sound.outputLayer.processing.timeout", 10, 60000, true /* clamp */, 1000);

    public static final ThreadConfig AOL_OUTPUT_THREAD = getThreadConfig(
        "org.theko.sound.outputLayer.output.thread", new ThreadConfig(ThreadType.PLATFORM, 8));

    public static final int AOL_OUTPUT_STOP_TIMEOUT = getIntInRange(
        "org.theko.sound.outputLayer.output.timeout", 10, 60000, true /* clamp */, 1000);

    public static final AudioMeasure AOL_DEFAULT_BUFFER = getAudioMeasure(
        "org.theko.sound.outputLayer.defaultBuffer", AudioMeasure.ofFrames(2048));
    
    public static final int AOL_DEFAULT_RING_BUFFERS = getIntInRange(
        "org.theko.sound.outputLayer.defaultRingBuffers", 1, Integer.MAX_VALUE, false /* use default */, 1);
    
    public static final AudioResamplerConfig AOL_RESAMPLER = getAudioResamplerConfig(
        "org.theko.sound.outputLayer.resampler", new AudioResamplerConfig(new LinearResampleMethod(), 2));

    public static final boolean AOL_ENABLE_SHUTDOWN_HOOK = getBoolean(
        "org.theko.sound.outputLayer.enableShutdownHook", true);

    public static final AudioResamplerConfig SHARED_RESAMPLER = getAudioResamplerConfig(
        "org.theko.sound.resampler.shared", new AudioResamplerConfig(new LinearResampleMethod(), 2));

    public static final boolean MIXER_DEFAULT_ENABLE_EFFECTS = getBoolean(
        "org.theko.sound.mixer.default.enableEffects", true);

    public static final boolean MIXER_DEFAULT_SWAP_CHANNELS = getBoolean(
        "org.theko.sound.mixer.default.swapChannels", false);

    public static final boolean MIXER_DEFAULT_REVERSE_POLARITY = getBoolean(
        "org.theko.sound.mixer.default.reversePolarity", false);

    public static final boolean WAVE_CODEC_CLEAN_TAG_TEXT = getBoolean(
        "org.theko.sound.waveCodec.cleanTagText", true);

    public static final ThreadConfig AUTOMATIONS_THREAD = getThreadConfig(
        "org.theko.sound.automation.thread", new ThreadConfig(ThreadType.VIRTUAL, 5));

    public static final int AUTOMATIONS_UPDATE_TIME = getIntInRange(
        "org.theko.sound.automation.updateTime", 0, Integer.MAX_VALUE, false /* use default */, 15);

    public static final ThreadConfig CLEANERS_THREAD = getThreadConfig(
        "org.theko.sound.cleaner.thread", new ThreadConfig(ThreadType.VIRTUAL, 1));

    public static final AudioResamplerConfig RESAMPLER_EFFECT = getAudioResamplerConfig(
        "org.theko.sound.effects.resampler", new AudioResamplerConfig(new LanczosResampleMethod(), 2));

    static {
        if (logger.isDebugEnabled()) {
            logProperties();
        }
    }

    private AudioSystemProperties() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    private static void logProperties() {
        StringBuilder builder = new StringBuilder();
        builder.append("Info:\nCurrent environment:\n");
        builder.append("  JVM: ").append(System.getProperty("java.vm.name"))
            .append(" ").append(System.getProperty("java.vm.version"))
            .append(" (").append(System.getProperty("java.vendor")).append(")\n");
        builder.append("  Runtime: ").append(System.getProperty("java.runtime.name"))
            .append(" ").append(System.getProperty("java.runtime.version")).append("\n");
        builder.append("  Memory: total=").append(FormatUtilities.formatBytesBinary(TOTAL_MEMORY, 3))
            .append(", max=").append(FormatUtilities.formatBytesBinary(MAX_MEMORY, 3)).append("\n");

        // OS info
        builder.append("  OS: ").append(System.getProperty("os.name"))
            .append(" ").append(System.getProperty("os.version"))
            .append(" (").append(System.getProperty("os.arch")).append(")").append("\n");
        builder.append("  Runtime detected platform: ").append(PLATFORM.name()).append("\n");
        builder.append("  Runtime detected architecture: ").append(ARCHITECTURE.name()).append("\n");

        // CPU info
        builder.append("  CPU Cores (logical): ").append(CPU_AVAILABLE_CORES).append("\n");

        builder.append("Audio system properties:\n");
        builder.append("  Threads:")
            .append(" OutputLayer: {")
            .append("Processing").append(FormatUtilities.formatThreadInfo(AOL_PROCESSING_THREAD))
            .append(", Output").append(FormatUtilities.formatThreadInfo(AOL_OUTPUT_THREAD))
            .append("}, Automation").append(FormatUtilities.formatThreadInfo(AUTOMATIONS_THREAD))
            .append(", Cleaner").append(FormatUtilities.formatThreadInfo(CLEANERS_THREAD))
            .append("\n");
        builder.append("  Default Output Buffer Size=").append(AOL_DEFAULT_BUFFER).append("\n");
        builder.append("  Default OutputLayer Ring Buffers Count=").append(AOL_DEFAULT_RING_BUFFERS).append("\n");
        builder.append("  OutputLayer resampler: ")
            .append(AOL_RESAMPLER).append("\n");
        builder.append("  Enable OutputLayer shutdown hook: ").append(AOL_ENABLE_SHUTDOWN_HOOK).append("\n");
        builder.append("  Resampler (Shared): ")
            .append(SHARED_RESAMPLER).append("\n");
        builder.append("  Mixer (default):")
            .append(" Enable effects=").append(MIXER_DEFAULT_ENABLE_EFFECTS)
            .append(", Swap channels=").append(MIXER_DEFAULT_SWAP_CHANNELS)
            .append(", Reverse polarity=").append(MIXER_DEFAULT_REVERSE_POLARITY)
            .append("\n");
        builder.append("  Effect resampler: ")
            .append(RESAMPLER_EFFECT).append("\n");
        builder.append("  Wave Codec:")
            .append(" Clean Tags Text=").append(WAVE_CODEC_CLEAN_TAG_TEXT);
        logger.debug(builder.toString());

        StringBuilder propertiesLog = new StringBuilder();

        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("org.theko.sound.")) {
                propertiesLog.append(key).append(": ").append(props.getProperty(key)).append("\n");
            }
        }

        logger.debug("Properties: \n{}", propertiesLog.toString());
    }
}
