package org.theko.sound.event;

/**
 * Listener for {@link AudioControl} events.
 * 
 * @since 2.4.0
 * @author Theko
 */
public interface AudioControlListener {

    /**
     * Called when the value of an {@link AudioControl} is changed.
     * @param event the event with the audio control
     */
    default void onValueChanged(AudioControlEvent event) { }
}
