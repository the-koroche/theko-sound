package org.theko.sound.event;

/**
 * The {@code AudioOutputLayerAdapter} class extends {@code AudioLineAdapter} and implements
 * the {@code AudioOutputLayerListener} interface. It provides an adapter implementation
 * for handling audio output line events.
 *
 * <p>This class can be used as a base class for creating custom audio output line
 * listeners by overriding the {@code onWrite} method to handle specific audio output
 * events.
 *
 * <p>Methods:
 * <ul>
 *   <li>{@link #onWrite(AudioLineEvent)} - Invoked when audio data is written to the output line.</li>
 * </ul>
 *
 * @see AudioLineAdapter
 * @see AudioOutputLayerListener
 * @see AudioLineEvent
 * 
 * @since v1.4.1
 * @author Theko
 */
public class AudioOutputLayerAdapter extends AudioLineAdapter implements AudioOutputLayerListener {
    
    @Override public void onWrite (AudioLineEvent e) { }
}