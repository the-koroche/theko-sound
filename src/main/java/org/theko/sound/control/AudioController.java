package org.theko.sound.control;

public abstract class AudioController {
    protected final String name;

    public AudioController (String name) {
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
