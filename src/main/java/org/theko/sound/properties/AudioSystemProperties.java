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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.resampling.*;
import org.theko.sound.utility.FormatUtilities;
import org.theko.sound.utility.PlatformUtilities;
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

    private static final Logger logger = LoggerFactory.getLogger(AudioSystemProperties.class);

    public enum ThreadType {
        VIRTUAL, PLATFORM;

        public static ThreadType from(String str) {
            return "virtual".equalsIgnoreCase(str) ? VIRTUAL : PLATFORM;
        }
    }

    public static final Platform PLATFORM = PlatformUtilities.getPlatform();

    public static final boolean IS_SUPPORTING_JAVA_21 = Runtime.version().feature() >= 21;
    public static final boolean IS_SUPPORTING_VIRTUAL_THREADS = IS_SUPPORTING_JAVA_21;

    public static final int CPU_AVAILABLE_CORES = Runtime.getRuntime().availableProcessors();
    public static final long TOTAL_MEMORY = Runtime.getRuntime().totalMemory();
    public static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();

    private static final int DEFAULT_AUDIO_OUTPUT_LINE_THREAD_PRIORITY = (Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2;

    public static final ThreadType AUDIO_OUTPUT_LINE_THREAD_TYPE =
            ThreadType.from(System.getProperty("org.theko.sound.threadType.audioOutputLine", "platform"));
    public static final ThreadType AUTOMATION_THREAD_TYPE =
            ThreadType.from(System.getProperty("org.theko.sound.threadType.automation", "virtual"));
    public static final ThreadType CLEANER_THREAD_TYPE =
            ThreadType.from(System.getProperty("org.theko.sound.threadType.cleaner", "virtual"));

    public static final int AUDIO_OUTPUT_LINE_THREAD_PRIORITY =
            parsePriority("org.theko.sound.threadPriority.audioOutputLine", DEFAULT_AUDIO_OUTPUT_LINE_THREAD_PRIORITY);
    public static final int AUTOMATION_THREAD_PRIORITY =
            parsePriority("org.theko.sound.threadPriority.automation", Thread.NORM_PRIORITY);
    public static final int CLEANER_THREAD_PRIORITY =
            parsePriority("org.theko.sound.threadPriority.cleaner", Thread.MIN_PRIORITY);

    public static final int AUDIO_OUTPUT_LAYER_BUFFER_SIZE =
            Integer.parseInt(System.getProperty("org.theko.sound.audioOutputLayer.defaultBufferSize", "2048"));

    public static final int RESAMPLER_SHARED_QUALITY =
            Integer.parseInt(System.getProperty("org.theko.sound.shared.resampler.quality", "2"));
    public static final ResampleMethod RESAMPLER_SHARED_METHOD =
            parseResampleMethod(System.getProperty("org.theko.sound.shared.resampler.method", "cubic"));
            
    public static final boolean ENABLE_EFFECTS_IN_MIXER =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.mixer.enableEffects", "true"));
    public static final boolean SWAP_CHANNELS_IN_MIXER =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.mixer.swapChannels", "false"));
    public static final boolean REVERSE_POLARITY_IN_MIXER =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.mixer.reversePolarity", "false"));
    public static final boolean CHECK_LENGTH_MISMATCH_IN_MIXER =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.mixer.checkLengthMismatch", "true"));

    public static final int RESAMPLER_EFFECT_QUALITY =
            Integer.parseInt(System.getProperty("org.theko.sound.effects.resampler.quality", "2"));
    public static final ResampleMethod RESAMPLER_EFFECT_METHOD =
            parseResampleMethod(System.getProperty("org.theko.sound.effects.resampler.method", "lanczos"));

    public static final boolean WAVE_CODEC_INFO_CLEAN_TEXT =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.codec.wave.cleanText", "true"));

    static {
        logProperties();
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

        // CPU info
        builder.append("  CPU Cores (logical): ").append(CPU_AVAILABLE_CORES).append("\n");

        builder.append("Audio system properties:\n");
        builder.append("  Threads:")
            .append(" Output").append(FormatUtilities.formatThreadInfo(AUDIO_OUTPUT_LINE_THREAD_TYPE, AUDIO_OUTPUT_LINE_THREAD_PRIORITY))
            .append(", Automation").append(FormatUtilities.formatThreadInfo(AUTOMATION_THREAD_TYPE, AUTOMATION_THREAD_PRIORITY))
            .append(", Cleaner").append(FormatUtilities.formatThreadInfo(CLEANER_THREAD_TYPE, CLEANER_THREAD_PRIORITY))
            .append("\n");
        builder.append("  Default Output Buffer Size=").append(AUDIO_OUTPUT_LAYER_BUFFER_SIZE).append("\n");
        builder.append("  Resampler (Shared): ")
            .append(RESAMPLER_SHARED_METHOD.getClass().getSimpleName()).append("(quality=").append(RESAMPLER_SHARED_QUALITY).append(")\n");
        builder.append("  Mixer (default):")
            .append(" Enable effects=").append(ENABLE_EFFECTS_IN_MIXER)
            .append(", Swap channels=").append(SWAP_CHANNELS_IN_MIXER)
            .append(", Reverse polarity=").append(REVERSE_POLARITY_IN_MIXER)
            .append(", Check length mismatch=").append(CHECK_LENGTH_MISMATCH_IN_MIXER)
            .append("\n");
        builder.append("  Effect resampler: ")
            .append(RESAMPLER_EFFECT_METHOD.getClass().getSimpleName()).append("(quality=").append(RESAMPLER_EFFECT_QUALITY).append(")\n");
        builder.append("  Wave Codec:")
            .append(" Clean Tag Text=").append(WAVE_CODEC_INFO_CLEAN_TEXT);
        logger.debug(builder.toString());
    }

    private static int parsePriority(String key, int defaultValue) {
        try {
            int value = Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
            if (value < Thread.MIN_PRIORITY || value > Thread.MAX_PRIORITY) {
                logger.warn("Invalid thread priority {} for '{}'. Using default {}", value, key, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException e) {
            logger.warn("Invalid thread priority format for '{}'. Using default {}", key, defaultValue);
            return defaultValue;
        }
    }

    private static ResampleMethod parseResampleMethod(String method) {
        switch (method.toLowerCase()) {
            case "nearest": case "nearest-neighbor": return new NearestResampleMethod();
            case "linear": return new LinearResampleMethod();
            case "cubic": return new CubicResampleMethod();
            case "lanczos": return new LanczosResampleMethod();
            default:
                ResampleMethod fallbackResample = new LinearResampleMethod();
                logger.warn("Resample method '{}' not recognized. Falling back to {}.", method, fallbackResample.getClass().getSimpleName());
                return fallbackResample;
        }
    }
}
