package org.theko.sound.util;

import java.lang.ref.Cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadsFactory {
    private static final Logger logger = LoggerFactory.getLogger(ThreadsFactory.class);

    private static final String THREAD_TYPE_PROPERTY = "org.theko.sound.threadType";
    private static final String PLAYBACK_THREAD_TYPE_PROPERTY = "org.theko.sound.threadType.playback";
    private static final String AUTOMATION_THREAD_TYPE_PROPERTY = "org.theko.sound.threadType.automation";
    private static final String MIXER_THREAD_TYPE_PROPERTY = "org.theko.sound.threadType.mixer";
    private static final String CLEANER_THREAD_TYPE_PROPERTY = "org.theko.sound.threadType.cleaner";

    private static final String DEFAULT_PLAYBACK_THREAD_TYPE = "platform";
    private static final String DEFAULT_AUTOMATION_THREAD_TYPE = "virtual";
    private static final String DEFAULT_MIXER_THREAD_TYPE = "platform";
    private static final String DEFAULT_CLEANER_THREAD_TYPE = "virtual";

    static {
        validateThreadParameters();
    }

    public enum ThreadType {
        GLOBAL(THREAD_TYPE_PROPERTY),
        PLAYBACK(PLAYBACK_THREAD_TYPE_PROPERTY),
        AUTOMATION(AUTOMATION_THREAD_TYPE_PROPERTY),
        MIXER(MIXER_THREAD_TYPE_PROPERTY),
        CLEANER(CLEANER_THREAD_TYPE_PROPERTY);

        private String parameter;

        ThreadType (String parameter) {
            this.parameter = parameter;
        }

        public String getPropertyName() {
            return parameter;
        }
    }

    public static Thread createThread(ThreadType threadType, Runnable runnable, String name) {
        String value = getThreadType(threadType);
        Thread thread = getThreadBuilder(value).name(name).unstarted(runnable);
        logger.debug("Creating {} thread of type {}", name, value);
        return thread;
    }

    public static Cleaner createCleaner() {
        String value = getThreadType(ThreadType.CLEANER);
        logger.debug("Creating cleaner thread of type {}", value);
        return Cleaner.create(getThreadBuilder(value).factory());
    }

    private static Thread.Builder getThreadBuilder(String value) {
        switch (value) {
            case "platform":
                return Thread.ofPlatform();
            case "virtual":
            default:
                return Thread.ofVirtual();
        }
    }

    private static String getThreadType(ThreadType threadType) {
        String param = System.getProperty(threadType.getPropertyName(), "unknown").toLowerCase();
        if (param.equals("unknown") && threadType != ThreadType.GLOBAL) {
            return System.getProperty(THREAD_TYPE_PROPERTY, "virtual").toLowerCase();
        }
        return param;
    }

    private static boolean validateThreadParameter(String value) {
        return value.equals("virtual") || value.equals("platform");
    }

    private static boolean validateThreadParameters() {
        String playbackValue = System.getProperty(
            PLAYBACK_THREAD_TYPE_PROPERTY, DEFAULT_PLAYBACK_THREAD_TYPE
        ).toLowerCase();
        String automationValue = System.getProperty(
            AUTOMATION_THREAD_TYPE_PROPERTY, DEFAULT_AUTOMATION_THREAD_TYPE
        ).toLowerCase();
        String mixerValue = System.getProperty(
            MIXER_THREAD_TYPE_PROPERTY, DEFAULT_MIXER_THREAD_TYPE
        ).toLowerCase();
        String cleanerValue = System.getProperty(
            CLEANER_THREAD_TYPE_PROPERTY, DEFAULT_CLEANER_THREAD_TYPE
        ).toLowerCase();

        if (!validateThreadParameter(playbackValue)) {
            logger.warn("Invalid value for " + PLAYBACK_THREAD_TYPE_PROPERTY + ": " + playbackValue + ". Using default value: " + DEFAULT_PLAYBACK_THREAD_TYPE);
            System.setProperty(PLAYBACK_THREAD_TYPE_PROPERTY, DEFAULT_PLAYBACK_THREAD_TYPE);
        } else if (!validateThreadParameter(automationValue)) {
            logger.warn("Invalid value for " + AUTOMATION_THREAD_TYPE_PROPERTY + ": " + automationValue + ". Using default value: " + DEFAULT_AUTOMATION_THREAD_TYPE);
            System.setProperty(AUTOMATION_THREAD_TYPE_PROPERTY, DEFAULT_AUTOMATION_THREAD_TYPE);
        } else if (!validateThreadParameter(mixerValue)) {
            logger.warn("Invalid value for " + MIXER_THREAD_TYPE_PROPERTY + ": " + mixerValue + ". Using default value: " + DEFAULT_MIXER_THREAD_TYPE);
            System.setProperty(MIXER_THREAD_TYPE_PROPERTY, DEFAULT_MIXER_THREAD_TYPE);
        } else if (!validateThreadParameter(cleanerValue)) {
            logger.warn("Invalid value for " + CLEANER_THREAD_TYPE_PROPERTY + ": " + cleanerValue + ". Using default value: " + DEFAULT_CLEANER_THREAD_TYPE);
            System.setProperty(CLEANER_THREAD_TYPE_PROPERTY, DEFAULT_CLEANER_THREAD_TYPE);
        }
        return true;
    }
}
