package org.theko.sound.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.resampling.*;

public final class AudioSystemProperties {

    private static final Logger logger = LoggerFactory.getLogger(AudioSystemProperties.class);

    public enum ThreadType {
        VIRTUAL, PLATFORM;

        public static ThreadType from(String str) {
            return "virtual".equalsIgnoreCase(str) ? VIRTUAL : PLATFORM;
        }
    }

    private static final int DEFAULT_AUDIO_OUTPUT_LINE_THREAD_PRIORITY = (Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2;

    public static final boolean SCAN_CLASSES =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.classLoader.scanAll", "false"));

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

    public static final int RESAMPLER_SHARED_QUALITY =
            Integer.parseInt(System.getProperty("org.theko.sound.shared.resampler.quality", "2"));
    public static final ResampleMethod RESAMPLER_SHARED_METHOD =
            parseResampleMethod(System.getProperty("org.theko.sound.shared.resampler.method", "lanczos"));
    public static final boolean RESAMPLER_LOG_HIGH_QUALITY =
            Boolean.parseBoolean(System.getProperty("org.theko.sound.resampler.logHighQuality", "true"));

    static {
        logProperties();
    }

    private AudioSystemProperties() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    private static void logProperties() {
        logger.debug("Scan classes: {}", SCAN_CLASSES);

        logger.debug("Audio output line thread: {}", 
                formatThreadInfo(AUDIO_OUTPUT_LINE_THREAD_TYPE, AUDIO_OUTPUT_LINE_THREAD_PRIORITY));
        logger.debug("Automation thread: {}", 
                formatThreadInfo(AUTOMATION_THREAD_TYPE, AUTOMATION_THREAD_PRIORITY));
        logger.debug("Cleaner thread: {}", 
                formatThreadInfo(CLEANER_THREAD_TYPE, CLEANER_THREAD_PRIORITY));

        logger.debug("Resampler shared quality: {}", RESAMPLER_SHARED_QUALITY);
        logger.debug("Resampler shared method: {}", RESAMPLER_SHARED_METHOD.getClass().getSimpleName());
        logger.debug("Resampler log high quality: {}", RESAMPLER_LOG_HIGH_QUALITY);
        logger.debug("Audio system properties initialized.");
    }

    private static String formatThreadInfo(ThreadType type, int priority) {
        return String.format("%s(priority=%d)", type.name().toLowerCase(), priority);
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
            case "linear": return new LinearResampleMethod();
            case "cubic": return new CubicResampleMethod();
            case "lanczos": return new LanczosResampleMethod();
            case "timestretch": return new PhaseVocoderResampler();
            default:
                logger.warn("Resample method '{}' not recognized. Falling back to CubicResampleMethod.", method);
                return new LinearResampleMethod();
        }
    }
}
