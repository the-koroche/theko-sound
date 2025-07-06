package org.theko.sound.resampling;

/**
 * Interface representing a resampling method for audio data.
 * Implementations of this interface define how to resample an input array
 * of audio samples to a specified target length.
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public interface ResampleMethod {
    
    float[] resample (float[] input, int targetLength, int quality);
}
