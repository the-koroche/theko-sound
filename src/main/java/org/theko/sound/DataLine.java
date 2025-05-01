package org.theko.sound;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.theko.sound.event.DataLineEvent;
import org.theko.sound.event.DataLineListener;

/**
 * The `DataLine` class represents a thread-safe audio data pipeline that allows
 * sending and receiving audio data in a queue-based manner. It supports various
 * operations such as blocking and non-blocking data transfer, timeout-based
 * operations, and listener notifications for events like data sent, received,
 * or timeout occurrences.
 * 
 * <p>This class implements the `AudioObject` and `AutoCloseable` interfaces,
 * ensuring compatibility with audio-related systems and providing a mechanism
 * for resource cleanup when the object is no longer in use.</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Thread-safe queue for storing audio data.</li>
 *   <li>Support for blocking and non-blocking send/receive operations.</li>
 *   <li>Timeout-based send and receive operations.</li>
 *   <li>Listener-based event notifications for data transfer and timeouts.</li>
 *   <li>Automatic resource cleanup using Java's `Cleaner` API.</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <p>To use this class, create an instance with a specified audio format and
 * optionally a queue capacity. Use the `send` and `receive` methods to transfer
 * data, and register listeners to handle events.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>The `DataLine` class is designed to be thread-safe, allowing multiple
 * threads to interact with the queue concurrently.</p>
 * 
 * <h2>Listeners:</h2>
 * <p>Listeners can be added to receive notifications for the following events:</p>
 * <ul>
 *   <li>Data sent (`onSend`).</li>
 *   <li>Data received (`onReceive`).</li>
 *   <li>Send timeout (`onSendTimeout`).</li>
 *   <li>Receive timeout (`onReceiveTimeout`).</li>
 * </ul>
 * 
 * <h2>Resource Management:</h2>
 * <p>The `close` method should be called to release resources when the `DataLine`
 * is no longer needed. Alternatively, the `Cleaner` API ensures automatic cleanup
 * when the object is garbage collected.</p>
 * 
 * <h2>Example:</h2>
 * <pre>{@code
 * AudioFormat format = new AudioFormat(...);
 * DataLine dataLine = new DataLine(format, 10);
 * 
 * dataLine.addDataLineListener(new DataLineListener() {
 *     @Override
 *     public void onSend(DataLineEvent e) {
 *         System.out.println("Data sent: " + e.getData());
 *     }
 * 
 *     @Override
 *     public void onReceive(DataLineEvent e) {
 *         System.out.println("Data received: " + e.getData());
 *     }
 * 
 *     @Override
 *     public void onSendTimeout(DataLineEvent e) {
 *         System.out.println("Send timeout occurred.");
 *     }
 * 
 *     @Override
 *     public void onReceiveTimeout(DataLineEvent e) {
 *         System.out.println("Receive timeout occurred.");
 *     }
 * });
 * 
 * float[][] audioData = ...;
 * dataLine.send(audioData);
 * float[][] receivedData = dataLine.receive();
 * 
 * dataLine.close();
 * }</pre>
 * 
 * @see AudioFormat
 * @see DataLineListener
 * @see DataLineEvent
 * 
 * @author Alex Soloviov
 */
public class DataLine implements AudioObject, AutoCloseable {
    private final BlockingQueue<float[][]> queue;
    private final List<DataLineListener> listeners = new ArrayList<>();
    private final AudioFormat audioFormat;

    protected boolean closed = true;

    private static transient final Cleaner cleaner = Cleaner.create(Thread.ofVirtual().factory());

    /**
     * Constructor with specified queue capacity.
     * 
     * @param audioFormat The audio format to be used for the data.
     * @param capacity The capacity of the queue for storing data.
     */
    public DataLine(AudioFormat audioFormat, int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.audioFormat = audioFormat;
        closed = false;
        cleaner.register(this, this::close);  // Register the cleaner to close this DataLine when no longer in use
    }

    /**
     * Default constructor with a queue capacity of 1.
     * 
     * @param audioFormat The audio format to be used for the data.
     */
    public DataLine(AudioFormat audioFormat) {
        this(audioFormat, 1);
    }

    /**
     * Puts data into the queue, blocking if necessary until space is available.
     * 
     * @param data The data to be sent.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public void send(float[][] data) throws InterruptedException {
        if (closed) return;  // Don't send if closed
        cleanListeners();  // Remove any null listeners before continuing
        queue.put(data);  // Blocking call to add data to the queue
        notifySendListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about sent data
    }

    /**
     * Takes data from the queue, blocking if necessary until data is available.
     * 
     * @return The received data.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public float[][] receive() throws InterruptedException {
        if (closed) return null;  // Return null if closed
        float[][] data = queue.take();  // Blocking call to take data from the queue
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about received data
        }
        return data;
    }

    /**
     * Forcefully sends data, overwriting existing data in the queue if necessary.
     * 
     * @param data The data to be forcefully sent.
     */
    public void forceSend(float[][] data) {
        if (closed) return;  // Don't force send if closed
        cleanListeners();  // Remove any null listeners before continuing
        if (queue.size() > 0) {
            queue.poll();  // Remove the oldest data if the queue is full
        }
        try {
            queue.put(data);  // Add new data to the queue
            notifySendListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about sent data
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Preserve interruption status
        }
    }

    /**
     * Forcefully receives data from the queue. Returns an empty byte array if no data is available.
     * 
     * @return The received data or an empty byte array if no data is available.
     */
    public float[][] forceReceive() {
        if (closed) return null;  // Return null if closed
        float[][] data = queue.poll();  // Non-blocking attempt to get data
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about received data
            return data;
        }
        return new float[0][];  // Return empty array if no data is available
    }

    /**
     * Attempts to send data with a timeout. Returns whether the data was successfully sent.
     * 
     * @param data The data to be sent.
     * @param timeout The maximum time to wait for space in the queue.
     * @param unit The time unit for the timeout.
     * @return True if the data was successfully sent, false otherwise.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public boolean sendWithTimeout(float[][] data, long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) return false;  // Don't send if closed
        cleanListeners();  // Remove any null listeners before continuing
        boolean success = queue.offer(data, timeout, unit);  // Attempt to add data to the queue with timeout
        
        if (success) {
            notifySendListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about sent data
        } else {
            notifySendTimeoutListeners(new DataLineEvent(this, null, null, timeout, unit));  // Notify about timeout
        }
        
        return success;
    }

    /**
     * Attempts to receive data with a timeout. Returns either the data or an empty byte array if no data is received.
     * 
     * @param timeout The maximum time to wait for data.
     * @param unit The time unit for the timeout.
     * @return The received data or an empty byte array if no data is received.
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public float[][] receiveWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) return null;  // Return null if closed
        float[][] data = queue.poll(timeout, unit);  // Non-blocking attempt with timeout
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, audioFormat, data));  // Notify listeners about received data
            return data;
        } else {
            notifyReceiveTimeoutListeners(new DataLineEvent(this, null, null, timeout, unit));  // Notify about timeout
            return new float[0][];
        }
    }

    /**
     * Checks if the queue is empty.
     * 
     * @return True if the queue is empty, false otherwise.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the current size of the queue.
     * 
     * @return The size of the queue.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Clears all data from the queue.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Returns the audio format associated with this data line.
     * 
     * @return The audio format.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Adds a listener to be notified of events related to data sending and receiving.
     * 
     * @param listener The listener to be added.
     */
    public void addDataLineListener(DataLineListener listener) {
        cleanListeners();  // Remove any null listeners before adding
        this.listeners.add(listener);
    }

    /**
     * Removes a listener from the list of listeners.
     * 
     * @param listener The listener to be removed.
     */
    public void removeDataLineListener(DataLineListener listener) {
        listeners.removeIf(current -> {
            return current == null || current.equals(listener);  // Remove null or matching listener
        });
    }

    /**
     * Closes the data line, cleaning up resources and clearing the queue and listeners.
     */
    @Override
    public void close() {
        closed = true;
        queue.clear();  // Clear the queue when closing
        listeners.clear();  // Remove all listeners when closing
    }

    /**
     * Cleans up null listeners from the listeners list.
     */
    private void cleanListeners() {
        listeners.removeIf(listener -> listener == null);  // Remove null listeners
    }

    /**
     * Notifies all listeners that data has been sent.
     * 
     * @param e The event containing information about the sent data.
     */
    private void notifySendListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onSend(e);  // Notify each listener about sent data
            } catch (Exception ex) {
                throw new RuntimeException(ex);  // Handle any listener exceptions
            }
        }
    }

    /**
     * Notifies all listeners that data has been received.
     * 
     * @param e The event containing information about the received data.
     */
    private void notifyReceiveListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onReceive(e);  // Notify each listener about received data
            } catch (Exception ex) {
                throw new RuntimeException(ex);  // Handle any listener exceptions
            }
        }
    }

    /**
     * Notifies all listeners that the send operation has timed out.
     * 
     * @param e The event containing information about the send timeout.
     */
    private void notifySendTimeoutListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onSendTimeout(e);  // Notify each listener about send timeout
            } catch (Exception ex) {
                throw new RuntimeException(ex);  // Handle any listener exceptions
            }
        }
    }

    /**
     * Notifies all listeners that the receive operation has timed out.
     * 
     * @param e The event containing information about the receive timeout.
     */
    private void notifyReceiveTimeoutListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onReceiveTimeout(e);  // Notify each listener about receive timeout
            } catch (Exception ex) {
                throw new RuntimeException(ex);  // Handle any listener exceptions
            }
        }
    }
}
