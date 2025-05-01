package org.theko.sound.control;

public abstract class AudioControl {
    protected final String name;

    public AudioControl (String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("AudioController {Name: %s}", name);
    }
}
