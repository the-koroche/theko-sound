package org.theko.sound;

import java.io.Serializable;

public interface AudioObject extends Serializable {
    static final long serialVersionUID = 1L;
    // Just a marker interface for audio objects, to use in the AudioPreset class.

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default void onLoad() { }
    default void onSave() { }
}