package org.theko.sound.resampling;

/**
 * Interface representing a resampling method for audio data.
 * Implementations of this interface define how to resample an input array
 * of audio samples to a specified target length.
 * 
 * @since v1.4.1
 * @author Theko
 */
public interface ResampleMethod {
    
    /**
     * Resamples the input audio samples to a new length.
     * 
     * @param input The input audio samples as a float array.
     * @param targetLength The desired length of the output audio samples.
     * @param quality The quality of the resampling process, typically an integer value.
     * @return A float array containing the resampled audio samples.
     */
    float[] resample (float[] input, int targetLength, int quality);
}
