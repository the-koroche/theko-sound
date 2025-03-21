package org.theko.sound;

public enum AudioFlow {
    IN, OUT;

    public static AudioFlow fromBoolean(boolean isOut) {
        return (isOut ? AudioFlow.OUT : AudioFlow.IN);
    }

    @Override
    public String toString() {
        switch (this) {
            case IN: return "IN";
            case OUT: return "OUT";
            default: return "UNKNOWN";
        }
    }
}
