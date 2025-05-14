package org.theko.sound.event;

/**
 * The {@code AudioLineAdapter} class provides an empty implementation of the
 * {@link AudioLineListener} interface. This adapter class can be used as a
 * base class for creating listeners, allowing subclasses to override only the
 * methods they are interested in.
 *
 * <p>Each method in this class corresponds to an event in the audio line
 * lifecycle, such as opening, closing, flushing, draining, starting, or
 * stopping the audio line. By default, these methods do nothing.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * AudioLineAdapter adapter = new AudioLineAdapter() {
 *     @Override
 *     public void onStart(AudioLineEvent e) {
 *         System.out.println("Audio line started: " + e);
 *     }
 * };
 * }
 * </pre>
 *
 * @see AudioLineListener
 * @see AudioLineEvent
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioLineAdapter implements AudioLineListener {
    public void onOpen(AudioLineEvent e) { }
    public void onClose(AudioLineEvent e) { }
    public void onFlush(AudioLineEvent e) { }
    public void onDrain(AudioLineEvent e) { }
    public void onStart(AudioLineEvent e) { }
    public void onStop(AudioLineEvent e) { }
}