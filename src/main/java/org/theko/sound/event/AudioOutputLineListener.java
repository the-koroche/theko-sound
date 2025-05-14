package org.theko.sound.event;

/**
 * The {@code AudioOutputLineListener} interface extends the {@code AudioLineListener}
 * interface and provides a method to handle events related to writing audio data
 * to an output line.
 *
 * <p>Implementations of this interface can be used to monitor and respond to
 * audio output line events, such as when audio data is written to the line.
 *
 * @see AudioLineListener
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public interface AudioOutputLineListener extends AudioLineListener {
    void onWrite(AudioLineEvent e);
}