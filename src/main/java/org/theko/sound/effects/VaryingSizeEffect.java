package org.theko.sound.effects;

/**
 * VaryingSizeEffect is an interface for effects that can change their output size
 * based on the required length of the input samples.
 * 
 * Implementations should provide a method to determine the target length
 * based on the required length.
 * 
 * @since v2.0.0
 * @author Theko
 * 
 * @see AudioEffect
 * @see ResamplerEffect
 */
public interface VaryingSizeEffect {

    /**
     * Returns the target length for the effect based on the required length.
     * 
     * @param requiredLength The length of the input samples that the effect will process.
     * @return The target length for the output samples after applying the effect.
     */
    public int getTargetLength (int requiredLength);
}
