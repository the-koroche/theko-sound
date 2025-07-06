package org.theko.sound.event;

/**
 * The {@code AudioInputLineAdapter} class is an implementation of the {@link AudioInputLineListener}
 * interface and extends the {@link AudioLineAdapter} class. It provides a mechanism to handle
 * audio input line events.
 *
 * <p>This class overrides the {@code onRead} method from the {@link AudioInputLineListener}
 * interface, which is triggered when an audio line read event occurs. The method can be
 * customized to handle specific behaviors when audio data is read.
 *
 * @see AudioLineAdapter
 * @see AudioInputLineListener
 * @see AudioLineEvent
 * 
 * @since v1.5.0
 * 
 * @author Theko
 */
public class AudioInputLineAdapter extends AudioLineAdapter implements AudioInputLineListener {

    @Override public void onRead (AudioLineEvent e) { }
}