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

import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;

/**
 * The {@code EnumControl} class represents a control that manages a value from an enumeration.
 * Useful for selecting filter types, waveform types, etc.
 * 
 * <p>Example usage:
 * <pre>
 * EnumControl bandTypeControl = new EnumControl("FilterType", BandType.PEAKING);
 * bandTypeControl.setValue(1.0f); // selects last enum constant
 * BandType current = bandTypeControl.getEnumValue();
 * </pre>
 * 
 * @param <T> The type of the enum values managed by this control.
 * 
 * @see AudioControl
 * 
 * @since 2.1.1
 * @author Theko
 */
public class EnumControl<T extends Enum<T>> extends AudioControl {

    private final T[] enumValues;
    private T value;

    /**
     * Constructs an {@code EnumControl} for the specified enum type and default value.
     * 
     * @param name The name of the control.
     * @param defaultValue The default enum value.
     */
    public EnumControl(String name, T defaultValue) {
        super(name);
        this.enumValues = defaultValue.getDeclaringClass().getEnumConstants();
        this.value = defaultValue;
    }

    /**
     * Sets the enum value to the specified index. If the index is out of range,
     * it will be clamped to the nearest valid index.
     * 
     * @param index The index of the enum value to set.
     */
    public void setValue(int index) {
        int clampedIndex = Math.max(0, Math.min(index, enumValues.length - 1));
        if (value != enumValues[clampedIndex]) {
            this.value = enumValues[clampedIndex];
            eventDispatcher.dispatch(AudioControlEventType.VALUE_CHANGED, new AudioControlEvent(this));
        }
    }

    /**
     * Sets the enum value to the specified normalized value (between 0.0 and 1.0).
     * The index of the enum value is calculated as follows: {@code (int)(normalizedValue * (enumValues.length - 1) + 0.5f}.
     * If the index is out of range, it will be clamped to the nearest valid index.
     * 
     * @param normalizedValue The normalized value to set the enum value from.
     */
    public void setValue(float normalizedValue) {
        int index = (int)(normalizedValue * (enumValues.length - 1) + 0.5f);
        setValue(index);
    }

    /**
     * Sets the enum value of this control to the given value.
     * 
     * @param newValue The new enum value to set.
     */
    public void setEnumValue(T newValue) {
        if (newValue != null && !newValue.equals(value)) {
            this.value = newValue;
            eventDispatcher.dispatch(AudioControlEventType.VALUE_CHANGED, new AudioControlEvent(this));
        }
    }

    /**
     * @return The current enum value of this control.
     */
    public T getEnumValue() {
        return value;
    }

    /**
     * @return The index of the current enum value in the array of available enum constants.
     */
    public int getEnumIndex() {
        return value.ordinal();
    }

    /**
     * Retrieves the normalized value of this control within the range [0, 1],
     * where 0 represents the first enum constant and 1 represents the last enum constant.
     * <p>
     * The normalized value is calculated as follows: {@code (float)(getEnumIndex()) / (enumValues.length - 1)}.
     * 
     * @return The normalized value of this control.
     */
    public float getNormalized() {
        return (float)(getEnumIndex()) / (enumValues.length - 1);
    }

    /**
     * @return A copy of the array of available enum constants.
     */
    public T[] getEnumValues() {
        return enumValues.clone();
    }

    /**
     * @return A string representation of this control, including its name, current value, and number of options.
     */
    @Override
    public String toString() {
        return String.format("EnumControl{Name: %s, Value: %s, Options: %d}", name, value.name(), enumValues.length);
    }
}
