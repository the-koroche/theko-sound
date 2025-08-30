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

import org.theko.sound.event.AudioControlEvent;
import java.util.Arrays;

/**
 * The {@code EnumControl} class represents a control that manages a value from an enumeration.
 * Useful for selecting filter types, waveform types, etc.
 * 
 * <p>Features:
 * <ul>
 *   <li>Set and retrieve enum value.</li>
 *   <li>Supports normalized float (0.0 to 1.0) mapping to enum constants.</li>
 *   <li>Notifies listeners on value change.</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * EnumControl bandTypeControl = new EnumControl("FilterType", BandType.PEAKING);
 * bandTypeControl.setValue(1.0f); // selects last enum constant
 * BandType current = bandTypeControl.getEnumValue();
 * </pre>
 * 
 * @see AudioControl
 * 
 * @since 2.1.1
 * @author Theko
 */
public class EnumControl extends AudioControl {

    private final Enum<?>[] enumValues;
    private Enum<?> value;

    /**
     * Constructs an {@code EnumControl} for the specified enum type and default value.
     * 
     * @param name The name of the control.
     * @param defaultValue The default enum value.
     */
    public EnumControl(String name, Enum<?> defaultValue) {
        super(name);
        this.enumValues = defaultValue.getDeclaringClass().getEnumConstants();
        this.value = defaultValue;
    }

    /**
     * Sets the enum value by index (clamped).
     * 
     * @param index The index of the enum constant to set.
     */
    public void setValue(int index) {
        int clampedIndex = Math.max(0, Math.min(index, enumValues.length - 1));
        if (value != enumValues[clampedIndex]) {
            this.value = enumValues[clampedIndex];
            eventDispatcher.dispatch(AudioControlNotifyType.VALUE_CHANGE, new AudioControlEvent(this));
        }
    }

    /**
     * Sets the enum value based on a normalized float from 0.0 to 1.0.
     * 
     * @param normalizedValue The normalized float representing the enum position.
     */
    public void setValue(float normalizedValue) {
        int index = (int)(normalizedValue * (enumValues.length - 1) + 0.5f);
        setValue(index);
    }

    /**
     * Sets the enum value directly.
     * 
     * @param newValue The new enum value.
     */
    public void setEnumValue(Enum<?> newValue) {
        if (newValue != null && !newValue.equals(value)) {
            this.value = newValue;
            eventDispatcher.dispatch(AudioControlNotifyType.VALUE_CHANGE, new AudioControlEvent(this));
        }
    }

    /**
     * Gets the current enum value.
     * 
     * @return The current enum value.
     */
    public Enum<?> getEnumValue() {
        return value;
    }

    /**
     * Gets the current value as normalized float [0.0 – 1.0].
     * 
     * @return Normalized float representing enum value.
     */
    public float getNormalized() {
        return (float) Arrays.asList(enumValues).indexOf(value) / (enumValues.length - 1);
    }

    /**
     * Gets the array of available enum constants.
     * 
     * @return Enum values available for this control.
     */
    public Enum<?>[] getEnumValues() {
        return enumValues.clone();
    }

    /**
     * Returns a string representation of this enum control.
     * 
     * @return A string describing the current state of the control.
     */
    @Override
    public String toString() {
        return String.format("EnumControl{Name: %s, Value: %s, Options: %d}", name, value.name(), enumValues.length);
    }
}
