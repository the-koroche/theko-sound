package org.theko.sound;

import java.io.Serializable;

public interface AudioObject extends Serializable {
    static final long serialVersionUID = -1;

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default void onLoad() { }
    default void onSave() { }
}