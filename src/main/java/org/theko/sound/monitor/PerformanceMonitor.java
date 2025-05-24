package org.theko.sound.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code PerformanceMonitor} class provides functionality to record and analyze the performance
 * of operations by tracking their execution times in nanoseconds.
 * <p>
 * It maintains a list of {@link Statistic} objects, each representing the start and end time of an operation.
 * The class offers methods to add new time records, retrieve statistical information such as average,
 * minimum, and maximum elapsed times, and manage the recorded statistics.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * PerformanceMonitor monitor = new PerformanceMonitor();
 * long start = System.nanoTime();
 * // ... perform operation ...
 * long end = System.nanoTime();
 * monitor.addTimeRecord(start, end);
 * long avg = monitor.getAverageTime(0);
 * </pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is <b>not</b> thread-safe. If multiple threads access an instance concurrently,
 * external synchronization is required.
 * </p>
 * 
 * @since v1.6.0
 *
 * @author Theko
 */
public class PerformanceMonitor {
    private final List<Statistic> statistics = new ArrayList<>();

    /**
     * Represents a statistic for a monitored operation, capturing the start and end times in nanoseconds.
     * <p>
     * This class provides a method to calculate the elapsed time of the operation.
     * </p>
     *
     * <p>
     * Fields:
     * <ul>
     *   <li>{@code nanosOpStart} - The start time of the operation in nanoseconds.</li>
     *   <li>{@code nanosOpEnd} - The end time of the operation in nanoseconds.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Methods:
     * <ul>
     *   <li>{@link #getElapsedTime()} - Returns the elapsed time between start and end in nanoseconds.</li>
     * </ul>
     * </p>
     * 
     * @since v1.6.0
     * 
     * @author Theko
     */
    public static class Statistic {
        public final long nanosOpStart;
        public final long nanosOpEnd;

        public Statistic(long nanosOpStart, long nanosOpEnd) {
            this.nanosOpStart = nanosOpStart;
            this.nanosOpEnd = nanosOpEnd;
        }

        public long getElapsedTime() {
            return nanosOpEnd - nanosOpStart;
        }
    }

    public PerformanceMonitor () {
    }

    /**
     * Adds a time record to the monitor. The record is represented by a pair of
     * timestamps, one for the start of the operation and one for the end. The
     * timestamps are given in nanoseconds.
     *
     * @param nanosOpStart The start time of the operation in nanoseconds.
     * @param nanosOpEnd The end time of the operation in nanoseconds.
     */
    public void addTimeRecord(long nanosOpStart, long nanosOpEnd) {
        Statistic statistic = new Statistic(nanosOpStart, nanosOpEnd);
        statistics.add(statistic);
    }

    /**
     * Adds the specified {@link Statistic} object to the list of statistics.
     * This method records the elapsed time of an operation for later analysis.
     *
     * @param statistic The {@link Statistic} object containing the start and end
     *                  times of the operation in nanoseconds.
     */
    public void addTimeRecord(Statistic statistic) {
        statistics.add(statistic);
    }

    /**
     * Calculates the average elapsed time of the operations that have been
     * recorded so far. The average is calculated over the last
     * {@code statistics.size() - indexUntil} operations.
     *
     * @param indexUntil The index until which the average should be calculated.
     *                   If negative, the average is calculated over all
     *                   operations.
     * @return The average elapsed time in nanoseconds.
     */
    public long getAverageTime(int indexUntil) {
        if (indexUntil < 0) {
            indexUntil = 0;
        }
        if (statistics.isEmpty() || indexUntil >= statistics.size()) {
            return -1;
        }
        long sum = 0;
        for (int i = indexUntil; i < statistics.size(); i++) {
            Statistic statistic = statistics.get(i);
            sum += statistic.nanosOpEnd - statistic.nanosOpStart;
        }
        return sum / statistics.size();
    }

    /**
     * Calculates the minimum elapsed time of the operations that have been
     * recorded so far. The minimum is calculated over the last
     * {@code statistics.size() - indexUntil} operations.
     *
     * @param indexUntil The index until which the minimum should be calculated.
     *                   If negative, the minimum is calculated over all
     *                   operations.
     * @return The minimum elapsed time in nanoseconds.
     */
    public long getMinimumTime(int indexUntil) {
        if (indexUntil < 0) {
            indexUntil = 0;
        }
        if (statistics.isEmpty() || indexUntil >= statistics.size()) {
            return -1;
        }
        long min = Long.MAX_VALUE;
        for (int i = indexUntil; i < statistics.size(); i++) {
            Statistic statistic = statistics.get(i);
            min = Math.min(min, statistic.getElapsedTime());
        }
        return min;
    }

    /**
     * Calculates the maximum elapsed time of the operations that have been
     * recorded so far. The maximum is calculated over the last
     * {@code statistics.size() - indexUntil} operations.
     *
     * @param indexUntil The index until which the maximum should be
     *                   calculated. If negative, the maximum is calculated
     *                   over all operations.
     * @return The maximum elapsed time in nanoseconds.
     */
    public long getMaximumTime(int indexUntil) {
        if (indexUntil < 0) {
            indexUntil = 0;
        }
        if (statistics.isEmpty() || indexUntil >= statistics.size()) {
            return -1;
        }
        long max = 0;
        for (int i = indexUntil; i < statistics.size(); i++) {
            Statistic statistic = statistics.get(i);
            max = Math.max(max, statistic.getElapsedTime());
        }
        return max;
    }

    public List<Statistic> getStatistics() {
        return statistics;
    }

    /**
     * Clears all recorded statistics.
     */
    public void clear() {
        statistics.clear();
    }

    @Override
    public String toString() {
        return "PerformanceMonitor{" + "statistics=" + statistics + '}';
    } 
}
