package org.theko.sound.event;

/**
 * The {@code DataLineAdapter} class provides an empty implementation of the
 * {@link DataLineListener} interface. This adapter class can be used as a base
 * class for creating custom implementations of the {@code DataLineListener}
 * interface, allowing subclasses to override only the methods they are
 * interested in.
 *
 * <p>Each method in this class is implemented as a no-op (no operation),
 * meaning it does nothing when called. Subclasses can override these methods
 * to provide specific behavior for handling {@link DataLineEvent} instances.
 *
 * @see DataLineListener
 * @see DataLineEvent
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public class DataLineAdapter implements DataLineListener {
    @Override public void onSend(DataLineEvent e) { }
    @Override public void onSendTimeout(DataLineEvent e) { }
    @Override public void onReceive(DataLineEvent e) { }
    @Override public void onReceiveTimeout(DataLineEvent e) { }
}