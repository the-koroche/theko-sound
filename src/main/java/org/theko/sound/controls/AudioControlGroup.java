/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.controls;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class for managing a collection of {@link AudioControl} objects.
 * It provides methods to add, remove, change and retrieve controls by type and name.
 * This class is thread-safe, and uses {@link CopyOnWriteArrayList} to ensure thread-safety.
 * 
 * <p>This class can be used to group controls by category, such as controlling
 * a specific audio effect or instrument.
 * 
 * @see AudioControl
 * @see FloatControl
 * 
 * @since 0.2.1-beta
 * @author Theko
 */
@SuppressWarnings("rawtypes")
public class AudioControlGroup {
    
    protected final List<FloatControl> floatControls = new CopyOnWriteArrayList<>();
    protected final List<BooleanControl> booleanControls = new CopyOnWriteArrayList<>();
    protected final List<EnumControl> enumControls = new CopyOnWriteArrayList<>();

    /**
     * Constructs an AudioControlGroup from a list of controls.
     * 
     * @param controls A list of AudioControl objects.
     */
    public AudioControlGroup(List<AudioControl> controls) {
        controls.forEach(this::addControl);
    }

    /**
     * Constructs an empty AudioControlGroup.
     */
    public AudioControlGroup() {
    }

    /**
     * Adds a control to the group.
     * If the control type is not Float, Boolean or Enum, it is ignored.
     * 
     * @param control The control to add.
     */
    public void addControl(AudioControl control) {
        if (control == null) {
            return;
        }
        if (control instanceof FloatControl) {
            floatControls.add((FloatControl) control);
        } else if (control instanceof BooleanControl) {
            booleanControls.add((BooleanControl) control);
        } else if (control instanceof EnumControl) {
            enumControls.add((EnumControl) control);
        }
    }

    /**
     * Removes a control from the group.
     * 
     * @param control The control to remove.
     */
    public void removeControl(AudioControl control) {
        if (control == null) {
            return;
        }
        if (control instanceof FloatControl) {
            floatControls.remove((FloatControl) control);
        } else if (control instanceof BooleanControl) {
            booleanControls.remove((BooleanControl) control);
        } else if (control instanceof EnumControl) {
            enumControls.remove((EnumControl) control);
        }
    }

    /**
     * Clears all controls from the group.
     */
    public void clearControls() {
        floatControls.clear();
        booleanControls.clear();
        enumControls.clear();
    }

    /**
     * Applies the given value to all controls in the group.
     * <p>
     * Behavior depends on control type:
     * <ul>
     *     <li>{@link FloatControl} - sets the float value directly</li>
     *     <li>{@link BooleanControl} - sets to true if value &gt; 0.5, false otherwise</li>
     *     <li>{@link EnumControl} - sets to the integer part of the value</li>
     * </ul>
     *
     * @param value the value to apply to all controls
     */
    public void apply(float value) {
        for (FloatControl control : floatControls) {
            control.setValue(value);
        }
        for (BooleanControl control : booleanControls) {
            control.setValue(value > 0.5f);
        }
        for (EnumControl control : enumControls) {
            control.setValue((int)value);
        }
    }

    /**
     * Applies a value to all Float controls in the group.
     * 
     * @param value The value to apply.
     */
    public void applyFloat(float value) {
        for (FloatControl control : floatControls) {
            control.setValue(value);
        }
    }

    /**
     * Applies a value to all Boolean controls in the group.
     * 
     * @param value The value to apply.
     */
    public void applyBoolean(boolean value) {
        for (BooleanControl control : booleanControls) {
            control.setValue(value);
        }
    }

    /**
     * Applies a value to all Enum controls in the group.
     * 
     * @param value The value to apply.
     */
    public void applyEnum(int value) {
        for (EnumControl control : enumControls) {
            control.setValue(value);
        }
    }

    /**
     * Returns an unmodifiable list of all controls in the group.
     * @return A list of AudioControl objects.
     */
    public List<AudioControl> getControls() {
        return Stream.of(floatControls, booleanControls, enumControls)
             .flatMap(List::stream)
             .collect(Collectors.collectingAndThen(
                 Collectors.toList(),
                 Collections::unmodifiableList
             ));
    }

    /**
     * Returns an unmodifiable list of all Float controls in the group.
     * @return A list of FloatControl objects.
     */
    public List<FloatControl> getFloatControls() {
        return Collections.unmodifiableList(floatControls);
    }

    /**
     * Returns an unmodifiable list of all Boolean controls in the group.
     * @return A list of BooleanControl objects.
     */
    public List<BooleanControl> getBooleanControls() {
        return Collections.unmodifiableList(booleanControls);
    }

    /**
     * Returns an unmodifiable list of all Enum controls in the group.
     * @return A list of EnumControl objects.
     */
    public List<EnumControl> getEnumControls() {
        return Collections.unmodifiableList(enumControls);
    }
}
