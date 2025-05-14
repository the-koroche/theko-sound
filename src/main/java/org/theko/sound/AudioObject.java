package org.theko.sound;

import java.io.Serializable;

/**
 * The {@code AudioObject} interface represents a serializable audio-related object
 * with default behaviors for loading and saving operations.
 * <p>
 * Implementing classes can override the default methods to provide specific
 * functionality for loading and saving, as well as utilize the default method
 * to retrieve the name of the class.
 * </p>
 *
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Provides a default implementation for retrieving the class name as the object's name.</li>
 *   <li>Defines default methods for handling load and save operations, which can be overridden.</li>
 * </ul>
 *
 * <p><b>Serialization:</b></p>
 * <ul>
 *   <li>Implements {@link Serializable} to allow objects to be serialized.</li>
 *   <li>Defines a {@code serialVersionUID} with a default value of {@code -1}.</li>
 * </ul>
 * 
 * @see AudioPreset
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public interface AudioObject extends Serializable {
    static final long serialVersionUID = -1;

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default void onLoad() { }
    default void onSave() { }
}