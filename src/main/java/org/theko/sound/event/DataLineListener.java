package org.theko.sound.event;

/**
 * The {@code DataLineListener} interface defines methods for handling events
 * related to data line communication. Implementations of this interface can
 * respond to events such as sending, receiving, and timeouts during data
 * transmission or reception.
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public interface DataLineListener {
    void onSend(DataLineEvent e);
    void onSendTimeout(DataLineEvent e);
    void onReceive(DataLineEvent e);
    void onReceiveTimeout(DataLineEvent e);
}