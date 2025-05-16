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

    static {
        if (!validateThreadParameters()) {
            logger.error("Invalid thread type parameters. Only 'virtual' and 'platform' are allowed.");
            logger.error("Using default 'virtual' threads.");
        }
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

        public String getName() {
            return parameter;
        }
    }

    public static Thread createThread(ThreadType threadType, Runnable runnable, String name) {
        String value = getThreadType(threadType);
        Thread thread = getThreadBuilder(value).name(name).unstarted(runnable);
        return thread;
    }

    public static Cleaner createCleaner() {
        String value = getThreadType(ThreadType.CLEANER);
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
        String param = System.getProperty(threadType.getName(), "unknown").toLowerCase();
        if (param.equals("unknown") && threadType != ThreadType.GLOBAL) {
            return System.getProperty(THREAD_TYPE_PROPERTY, "virtual").toLowerCase();
        }
        return param;
    }

    private static boolean validateThreadParameter(String value) {
        return value.equals("virtual") || value.equals("platform");
    }

    private static boolean validateThreadParameters() {
        return validateThreadParameter(AUTOMATION_THREAD_TYPE_PROPERTY) &&
                validateThreadParameter(PLAYBACK_THREAD_TYPE_PROPERTY) &&
                validateThreadParameter(MIXER_THREAD_TYPE_PROPERTY) &&
                validateThreadParameter(CLEANER_THREAD_TYPE_PROPERTY);
    }
}
