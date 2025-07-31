package org.theko.sound;

/**
 * Utility class for generating waveforms.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class WaveformGenerator {
    
    private WaveformGenerator() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static float generate(WaveformType type, float value) {
        switch (type) {
            case SINE:
                return (float)Math.sin(value * Math.PI * 2);
            case SQUARE:
                return (float)Math.signum(Math.sin(value * Math.PI * 2));
            case TRIANGLE:
                return 1f - 4f * Math.abs(value % 1f - 0.5f);
            case SAWTOOTH:
                return 2f * (value % 1f) - 1f;
        }
        return 0.0f;
    }
}
