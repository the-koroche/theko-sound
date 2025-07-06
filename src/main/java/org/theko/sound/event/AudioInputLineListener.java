package org.theko.sound.event;

/**
 * The {@code AudioInputLineListener} interface extends {@code AudioLineListener}
 * and provides a method to handle events related to reading audio input.
 * Implementations of this interface can be used to process audio input events
 * as they occur.
 *
 * @see AudioLineListener
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public interface AudioInputLineListener extends AudioLineListener {

    void onRead (AudioLineEvent e);
}