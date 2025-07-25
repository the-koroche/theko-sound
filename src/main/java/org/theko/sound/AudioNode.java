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
    
    /**
     * Renders the audio node's output into the provided sample buffer.
     * 
     * @param samples The sample buffer to render into.
     * @param sampleRate The sample rate of the audio.
     */
    public void render (float[][] samples, int sampleRate);
}
