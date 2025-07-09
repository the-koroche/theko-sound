package org.theko.sound;

/**
 * Represents a node in the audio processing graph.
 * <p>Each node can process audio samples and is responsible for rendering
 * its output into the provided sample buffer.
 * 
 * @since v2.0.0
 * @author Theko
 * 
 * @see org.theko.sound.AudioMixer
 * @see org.theko.sound.effects.AudioEffect
 */
public interface AudioNode {
    
    public void render (float[][] samples, int sampleRate, int length);
}
