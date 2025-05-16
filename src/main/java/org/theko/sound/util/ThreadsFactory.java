package org.theko.sound.util;

import java.lang.ref.Cleaner;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadsFactory {
    private static final Logger logger = LoggerFactory.getLogger(ThreadsFactory.class);

    private static final String THREAD_TYPE_PROPERTY = "org.theko.sound.config.threadType";
    private static final String CLEANER_THREAD_TYPE_PROPERTY = "org.theko.sound.config.cleanerThreadType";

    static {
        if (!validateParameters()) {
            logger.error("Invalid thread type parameter(s). Valid values are 'platform' and 'virtual'.");
            logger.error("Falling back to default: 'virtual'.");
        } else {
            logger.debug("Thread type: {}", getThreadTypeParameter());
            logger.debug("Cleaner thread type: {}", getCleanerThreadTypeParameter());
        }
    }

    private ThreadsFactory() {
    }

    public static Cleaner createCleanerWithThread() {
        return Cleaner.create(getThreadFactory(getCleanerThreadTypeParameter()));
    }

    public static Thread createThread(Runnable runnable, String name) {
        return getThreadBuilder(getThreadTypeParameter()).name(name).unstarted(runnable);
    }

    public static Thread createThread(Runnable runnable) {
        return getThreadBuilder(getThreadTypeParameter()).unstarted(runnable);
    }

    private static ThreadFactory getThreadFactory(String type) {
        return getThreadBuilder(type).factory();
    }

    private static Thread.Builder getThreadBuilder(String type) {
        return switch (type) {
            case "platform" -> Thread.ofPlatform();
            case "virtual" -> Thread.ofVirtual();
            default -> Thread.ofVirtual();
        };
    }

    private static String getThreadTypeParameter() {
        return System.getProperty(THREAD_TYPE_PROPERTY, "virtual").toLowerCase();
    }

    private static String getCleanerThreadTypeParameter() {
        return System.getProperty(CLEANER_THREAD_TYPE_PROPERTY, "virtual").toLowerCase();
    }

    private static boolean validateParameters() {
        return validateParameter(getThreadTypeParameter()) &&
               validateParameter(getCleanerThreadTypeParameter());
    }

    private static boolean validateParameter(String value) {
        return value.equals("platform") || value.equals("virtual");
    }
}
