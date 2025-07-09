package org.theko.sound.event;

/**
 * The {@code AudioLineListener} interface defines a set of callback methods
 * that are invoked to notify listeners about various state changes or events
 * occurring on an audio line.
 * <p>
 * Implementations of this interface can be used to handle events such as
 * opening, closing, flushing, draining, starting, and stopping an audio line.
 * </p>
 *
 * <p>Each method receives an {@link AudioLineEvent} object that provides
 * additional details about the event.</p>
 *
 * <ul>
 *   <li>{@code onOpen(AudioLineEvent e)} - Invoked when the audio line is opened.</li>
 *   <li>{@code onClose(AudioLineEvent e)} - Invoked when the audio line is closed.</li>
 *   <li>{@code onFlush(AudioLineEvent e)} - Invoked when the audio line is flushed.</li>
 *   <li>{@code onDrain(AudioLineEvent e)} - Invoked when the audio line is drained.</li>
 *   <li>{@code onStart(AudioLineEvent e)} - Invoked when the audio line starts playback or recording.</li>
 *   <li>{@code onStop(AudioLineEvent e)} - Invoked when the audio line stops playback or recording.</li>
 * </ul>
 *
 * <p>Implement this interface to handle audio line events in a custom way.</p>
 * 
 * @since v1.4.1
 * @author Theko
 */
public interface AudioLineListener {

    void onOpen (AudioLineEvent e);
    void onClose (AudioLineEvent e);
    void onFlush (AudioLineEvent e);
    void onDrain (AudioLineEvent e);
    void onStart (AudioLineEvent e);
    void onStop (AudioLineEvent e);
}