package org.theko.sound.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.properties.AudioSystemProperties.ThreadType;

public class ThreadUtilities {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtilities.class);

    private ThreadUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static Thread createThread(String name, ThreadType type, int priority, Runnable task) {
        Thread thread;

        if (type == ThreadType.VIRTUAL) {
            // Java 21+ — virtual threads
            thread = Thread.ofVirtual().name(name).unstarted(task);
        } else {
            thread = new Thread(task, name);
            thread.setPriority(priority);
        }

        logger.debug("Created thread: {} (type={}, priority={})", name, type, priority);

        return thread;
    }
}
