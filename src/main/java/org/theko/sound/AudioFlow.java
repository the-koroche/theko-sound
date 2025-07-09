package org.theko.sound;

/**
 * Represents the direction of audio flow, either input (IN) or output (OUT).
 * 
 * @see AudioPort
 * 
 * @since v1.0.0
 * @author Theko
 */
public enum AudioFlow {
    
    /** Audio input flow (e.g., recording from a microphone). */
    IN,
    /** Audio output flow (e.g., playing sound through speakers). */
    OUT;

    /**
     * Converts a boolean value to an AudioFlow.
     *
     * @param isOut If true, returns {@code OUT}; otherwise, returns {@code IN}.
     * @return The corresponding AudioFlow value.
     */
    public static AudioFlow fromBoolean (boolean isOut) {
        return (isOut ? AudioFlow.OUT : AudioFlow.IN);
    }

    /**
     * Returns a string representation of this AudioFlow.
     *
     * @return "IN" for input flow, "OUT" for output flow.
     */
    @Override
    public String toString () {
        switch (this) {
            case IN: return "IN";
            case OUT: return "OUT";
            default: return "UNKNOWN";
        }
    }
}
