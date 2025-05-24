package org.theko.sound.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.theko.sound.util.InstanceCounter;

/**
 * The {@code GlobalPerformanceMonitor} class provides a global utility for tracking and reporting
 * performance metrics (such as execution times) for various objects in this audio library.
 * <p>
 * It maintains a mapping between monitored objects and their associated {@link PerformanceMonitor}
 * instances, allowing for the recording and retrieval of timing statistics.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Adds time records for monitored objects.</li>
 *   <li>Retrieves a list of all monitored objects.</li>
 *   <li>Provides access to the underlying monitor map.</li>
 *   <li>Retrieves performance statistics for individual objects.</li>
 *   <li>Generates a formatted summary of performance metrics for all monitored objects.</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * The class uses a {@link java.util.concurrent.ConcurrentHashMap} for storing monitors,
 * but the {@code monitoredObjects} list is not thread-safe and may require external synchronization
 * if accessed concurrently.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * GlobalPerformanceMonitor.addTimeRecord(obj, startNanos, endNanos);
 * String report = GlobalPerformanceMonitor.getInfo();
 * </pre>
 *
 * <h2>Note:</h2>
 * <ul>
 *   <li>This class cannot be instantiated.</li>
 *   <li>Objects implementing {@link InstanceCounter} will have their instance number included in reports.</li>
 * </ul>
 */
public class GlobalPerformanceMonitor {
    private static final Map<Object, PerformanceMonitor> monitors = new ConcurrentHashMap<>();
    private static final List<Object> monitoredObjects = new ArrayList<>();

    private GlobalPerformanceMonitor () {
    }

    /**
     * Adds a time record for the given object to the global performance monitor.
     * <p>
     * If the object is not already being monitored, a new monitor is created and
     * added to the map. The object is also added to the list of monitored objects.
     * </p>
     *
     * @param obj        the object to record the time for
     * @param nanosOpStart  the start time of the operation in nanoseconds
     * @param nanosOpEnd    the end time of the operation in nanoseconds
     */
    public static void addTimeRecord(Object obj, long nanosOpStart, long nanosOpEnd) {
        PerformanceMonitor monitor = monitors.get(obj);
        if (monitor == null) {
            monitor = new PerformanceMonitor();
            monitors.put(obj, monitor);
            monitoredObjects.add(obj);
        }
        monitor.addTimeRecord(nanosOpStart, nanosOpEnd);
    }

    public static List<Object> getMonitoredObjects() {
        return monitoredObjects;
    }

    public static Map<Object, PerformanceMonitor> getMonitors() {
        return monitors;
    }

    public static PerformanceMonitor getMonitor(Object obj) {
        return monitors.get(obj);
    }

    /**
     * Generates a formatted summary of performance metrics for all monitored objects.
     * <p>
     * The method iterates over all monitored objects and retrieves their performance statistics
     * using the associated {@link PerformanceMonitor} instances. It calculates the average,
     * minimum, and maximum elapsed times for each object and formats them into a table.
     * </p>
     * 
     * <p>
     * The output includes the object's class name and instance number (if the object
     * implements {@link InstanceCounter}), followed by the average, minimum, and maximum
     * times in milliseconds. The times are averaged over the last 512 recorded statistics.
     * </p>
     * 
     * @return A string representation of the performance metrics for all monitored objects,
     *         formatted as a table.
     */
    public static String getInfo() {
        StringBuilder outString = new StringBuilder();
        outString.append("Object                | Average Time (ms) | Min Time (ms) | Max Time (ms) \n");
        for (Object obj : monitoredObjects) {
            PerformanceMonitor monitor = monitors.get(obj);

            int instance = -1;
            if (obj instanceof InstanceCounter) {
                instance = ((InstanceCounter) obj).getCurrentInstance();
            }
            int size = monitor.getStatistics().size();
            long averageTime = monitor.getAverageTime(size - 512);
            long minTime = monitor.getMinimumTime(size - 512);
            long maxTime = monitor.getMaximumTime(size - 512);

            double averageMs = averageTime / 1_000_000.0;
            double minMs = minTime / 1_000_000.0;
            double maxMs = maxTime / 1_000_000.0;

            StringBuilder objectName = new StringBuilder();
            if (instance == -1) {
                objectName.append(obj.getClass().getSimpleName());
            } else {
                objectName.append(obj.getClass().getSimpleName() + "-" + instance);
            }

            outString.append(addWhitespaces(objectName.toString(), 24));
            outString.append(addWhitespaces(String.format("%.6f", averageMs), 20));
            outString.append(addWhitespaces(String.format("%.6f", minMs), 16));
            outString.append(String.format("%.6f", maxMs));
            outString.append("\n");
        }
        return outString.toString();
    }

    private static String addWhitespaces(String str, int outputLength) {
        StringBuilder outString = new StringBuilder();
        outString.append(str);
        for (int i = 0; i < outputLength - str.length(); i++) {
            outString.append(' ');
        }
        return outString.toString();
    }
}
