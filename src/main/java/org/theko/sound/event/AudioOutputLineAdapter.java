package org.theko.sound.event;

/**
 * The {@code AudioOutputLineAdapter} class extends {@code AudioLineAdapter} and implements
 * the {@code AudioOutputLineListener} interface. It provides an adapter implementation
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
 * @see AudioOutputLineListener
 * @see AudioLineEvent
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioOutputLineAdapter extends AudioLineAdapter implements AudioOutputLineListener {
    
    @Override public void onWrite (AudioLineEvent e) { }
}