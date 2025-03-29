package org.theko.sound;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.theko.sound.event.DataLineEvent;
import org.theko.sound.event.DataLineListener;

public class DataLine implements AutoCloseable {
    private final BlockingQueue<byte[]> queue;
    private final List<DataLineListener> listeners = new ArrayList<>();
    private final AudioFormat audioFormat;

    protected boolean closed = true;

    private static final Cleaner cleaner = Cleaner.create(Thread.ofVirtual().factory());

    /** Constructor with specified capacity */
    public DataLine(AudioFormat audioFormat, int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.audioFormat = audioFormat;
        closed = false;
        cleaner.register(this, this::close);
    }

    /** Default constructor with capacity of 1 */
    public DataLine(AudioFormat audioFormat) {
        this(audioFormat, 1);
    }

    /** Puts data into the queue, blocking if necessary */
    public void send(byte[] data) throws InterruptedException {
        if (closed) return;
        cleanListeners();
        queue.put(data);  // Blocking until space is available
        notifySendListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
    }

    /** Takes data from the queue, blocking if necessary */
    public byte[] receive() throws InterruptedException {
        if (closed) return null;
        byte[] data = queue.take();  // Blocking until data is available
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
        }
        return data;
    }

    /** Set the data in the queue, forcefully overwriting existing data */
    public void forceSend(byte[] data) {
        if (closed) return;
        cleanListeners();
        if (queue.size() > 0) {
            queue.poll();  // Discard the oldest data
        }
        try {
            queue.put(data);  // Add new data
            notifySendListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Preserve interruption status
        }
    }

    /** Get data from the queue, return an empty byte array if no data is available */
    public byte[] forceReceive() {
        if (closed) return null;
        byte[] data = queue.poll();  // Non-blocking attempt to get data
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
            return data;
        }
        return new byte[0];  // Return empty array if no data
    }

    /** Try to take data from the queue with a timeout */
    public boolean sendWithTimeout(byte[] data, long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) return false;
        cleanListeners();
        // Пытаемся добавить данные в очередь с указанным тайм-аутом
        boolean success = queue.offer(data, timeout, unit);
    
        if (success) {
            notifySendListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
        } else {
            notifySendTimeoutListeners(new DataLineEvent(this, new AudioData(data, audioFormat), timeout, unit));
        }
    
        return success;
    }

    /** Try to take data from the queue with a timeout */
    public byte[] receiveWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        if (closed) return null;
        byte[] data = queue.poll(timeout, unit);  // Non-blocking attempt with timeout
        if (data != null) {
            notifyReceiveListeners(new DataLineEvent(this, new AudioData(data, audioFormat)));
            return data;
        } else {
            notifyReceiveTimeoutListeners(new DataLineEvent(this, null, timeout, unit));
            return new byte[0];
        }
    }

    /** Check if the queue is empty */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /** Check the current size of the queue */
    public int size() {
        return queue.size();
    }

    /** Clear the queue */
    public void clear() {
        queue.clear();
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /** Add a listener to receive notifications */
    public void addDataLineListener(DataLineListener listener) {
        cleanListeners();
        this.listeners.add(listener);
    }

    /** Remove a listener from receiving notifications */
    public void removeDataLineListener(DataLineListener listener) {
        listeners.removeIf(current -> {
            return current == null || current.equals(listener);
        });
    }

    @Override
    public void close() {
        closed = true;
        queue.clear();
        listeners.clear();
    }

    private void cleanListeners() {
        listeners.removeIf(listener -> listener == null);
    }

    /** Notify all listeners that data has been sent */
    private void notifySendListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onSend(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /** Notify all listeners that data has been received */
    private void notifyReceiveListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onReceive(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /** Notify all listeners that the send operation has timed out */
    private void notifySendTimeoutListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onSendTimeout(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /** Notify all listeners that the receive operation has timed out */
    private void notifyReceiveTimeoutListeners(DataLineEvent e) {
        for (DataLineListener listener : listeners) {
            try {
                if (listener != null) listener.onReceiveTimeout(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
