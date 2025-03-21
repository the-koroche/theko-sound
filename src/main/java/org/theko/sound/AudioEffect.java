package org.theko.sound;

public abstract class AudioEffect {
    public enum Type {
        REALTIME, PROCESS
    }

    protected final AudioFormat audioFormat;
    protected final Type type;

    public AudioEffect (Type type, AudioFormat audioFormat) {
        this.type = type;
        this.audioFormat = audioFormat;
    }

    public abstract float[][] process(float[][] samples);

    public Type getType() {
        return type;
    }
}
