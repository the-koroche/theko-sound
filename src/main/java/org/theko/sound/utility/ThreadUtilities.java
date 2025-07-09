package org.theko.sound.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.properties.AudioSystemProperties.ThreadType;

/**
 * Utility class for creating threads.
 * 
 * @author Theko
 * @since v2.0.0
 */
public class ThreadUtilities {

    private static final Logger logger = LoggerFactory.getLogger(ThreadUtilities.class);

    private ThreadUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Creates a new thread with the specified name, type, priority, and task.
     * 
     * @param name The name of the thread.
     * @param type The type of the thread (virtual or platform).
     * @param priority The priority of the thread.
     * @param task The task to be executed by the thread.
     * @return A new Thread instance configured with the specified parameters.
     */
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
