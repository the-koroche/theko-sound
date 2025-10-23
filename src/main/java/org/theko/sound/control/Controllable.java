/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.control;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Controllable interface provides methods for managing and retrieving 
 * audio control objects within a class. It uses reflection to dynamically 
 * access fields of the implementing class that are instances of AudioControl 
 * or its subclasses. This interface includes methods to retrieve all 
 * controls, as well as specific types of controls by name.
 *
 * <p>Note: The {@code getAllControls()} method uses reflection to access 
 * declared fields, which may be subject to security restrictions. If a field 
 * is inaccessible, an empty list is returned.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * Controllable controllable = new MyAudioClass();
 * List<AudioControl> controls = controllable.getAllControls();
 * AudioControl volumeControl = controllable.getControl("Volume");
 * FloatControl bassControl = controllable.getFloatControl("Bass");
 * BooleanControl muteControl = controllable.getBooleanControl("Mute");
 * }
 * </pre>
 *
 * @see AudioControl
 * @see FloatControl
 * @see BooleanControl
 * 
 * @since 1.4.1
 * @author Theko
 */
public interface Controllable {

    /**
     * Returns all {@link AudioControl} objects declared as fields in the implementing class.
     *
     * @implSpec
     * The default implementation uses reflection to:
     * <ul>
     *   <li>Access all declared fields of the implementing class (excluding inherited ones).</li>
     *   <li>Collect those that are instances of {@code AudioControl} or its subclasses.</li>
     *   <li>Ignore fields that are inaccessible due to security restrictions.</li>
     *   <li>Return an unmodifiable list of the collected controls.</li>
     * </ul>
     *
     * <p>It is recommended to override this method in performance-critical implementations
     * to avoid the cost of reflection.
     *
     * @return an unmodifiable list of {@code AudioControl} objects found in the implementing class
     */
    default List<AudioControl> getAllControls() {
        List<AudioControl> controls = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (AudioControl.class.isAssignableFrom(field.getType())) {
                    AudioControl control = (AudioControl) field.get(this);
                    if (control != null) {
                        controls.add(control);
                    }
                }
            } catch (IllegalAccessException | InaccessibleObjectException ignored) {
            }
        }
        return Collections.unmodifiableList(controls);
    }

    /**
     * Retrieves a specific AudioControl by its name.
     *
     * This method iterates over all available audio controls obtained from 
     * {@link #getAllControls()} and checks if any control's name matches 
     * the specified name. If a match is found, the corresponding 
     * AudioControl is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the AudioControl to be retrieved.
     * @return The AudioControl with the specified name, or null if no 
     *         such control exists.
     */
    default AudioControl getControl(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name)) {
                return control;
            }
        }
        return null;
    }

    /**
     * Retrieves a FloatControl by its name.
     *
     * This method iterates over all available audio controls obtained from 
     * {@link #getAllControls()} and checks if any control is both a FloatControl 
     * and its name matches the specified name. If a match is found, the 
     * corresponding FloatControl is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the FloatControl to be retrieved.
     * @return The FloatControl with the specified name, or null if no 
     *         such control exists.
     */
    default FloatControl getFloatControl(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name) && control instanceof FloatControl) {
                return (FloatControl) control;
            }
        }
        return null;
    }

    /**
     * Retrieves a BooleanControl by its name.
     *
     * This method iterates over all available audio controls obtained from
     * {@link #getAllControls()} and checks if any control is both a BooleanControl
     * and its name matches the specified name. If a match is found, the
     * corresponding BooleanControl is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the BooleanControl to be retrieved.
     * @return The BooleanControl with the specified name, or null if no
     *         such control exists.
     */
    default BooleanControl getBooleanControl(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name) && control instanceof BooleanControl) {
                return (BooleanControl) control;
            }
        }
        return null;
    }

    /**
     * Retrieves an EnumControl by its name.
     *
     * This method iterates over all available audio controls obtained from
     * {@link #getAllControls()} and checks if any control is both a EnumControl
     * and its name matches the specified name. If a match is found, the
     * corresponding EnumControl is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the EnumControl to be retrieved.
     * @return The EnumControl with the specified name, or null if no
     *         such control exists.
     */
    default EnumControl getEnumControl(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name) && control instanceof EnumControl) {
                return (EnumControl) control;
            }
        }
        return null;
    }

    /**
     * Retrieves a Vector2Control by its name.
     *
     * This method iterates over all available audio controls obtained from
     * {@link #getAllControls()} and checks if any control is both a Vector2Control
     * and its name matches the specified name. If a match is found, the
     * corresponding Vector2Control is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the Vector2Control to be retrieved.
     * @return The Vector2Control with the specified name, or null if no
     *         such control exists.
     */
    default Vector2Control getVector2Control(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name) && control instanceof Vector2Control) {
                return (Vector2Control) control;
            }
        }
        return null;
    }

    /**
     * Retrieves a Vector3Control by its name.
     *
     * This method iterates over all available audio controls obtained from
     * {@link #getAllControls()} and checks if any control is both a Vector3Control
     * and its name matches the specified name. If a match is found, the
     * corresponding Vector3Control is returned. If no matching control is found,
     * the method returns null.
     *
     * @param name The name of the Vector3Control to be retrieved.
     * @return The Vector3Control with the specified name, or null if no
     *         such control exists.
     */
    default Vector3Control getVector3Control(String name) {
        for (AudioControl control : getAllControls()) {
            if (control.getName().equals(name) && control instanceof Vector3Control) {
                return (Vector3Control) control;
            }
        }
        return null;
    }
}
