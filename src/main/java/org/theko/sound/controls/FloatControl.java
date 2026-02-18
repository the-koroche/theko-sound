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

package org.theko.sound.controls;

import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;
import org.theko.sound.util.MathUtilities;

/**
 * The {@code FloatControl} class represents a control for managing a floating-point value
 * within a specified range. It extends the {@code AudioControl} class and provides methods
 * to set, retrieve, and normalize the value.
 * 
 * <p>This class is useful for controlling parameters such as volume, pitch, or other
 * audio-related properties that require a floating-point range.
 * 
 * <p>Example usage:
 * <pre>
 * FloatControl volumeControl = new FloatControl("Volume", 0.0f, 1.0f, 0.5f);
 * volumeControl.setValue(0.8f);
 * float normalizedValue = volumeControl.getNormalized();
 * </pre>
 * 
 * @see AudioControl
 * 
 * @since 1.2.0
 * @author Theko
 */
public class FloatControl extends AudioControl {

    protected float value;
    protected final float min, max;

    
    public FloatControl(String name, float min, float max, float value) {
        super(name);
        this.min = min;
        this.max = max;
        this.value = MathUtilities.clamp(value, min, max);
    }

    /**
     * Sets the value of this control to the given value, ensuring that the
     * new value is within the range of {@code min} and {@code max}.
     * 
     * @param value The new value for this control.
     */
    public void setValue(float value) {
        this.value = MathUtilities.clamp(value, min, max);
        eventDispatcher.dispatch(AudioControlEventType.VALUE_CHANGED, new AudioControlEvent(this));
    }

    /**
     * Sets the value of this control to the given normalized value (between 0.0 and 1.0), ensuring that the
     * new value is within the range of {@code min} and {@code max}.
     * If the value is outside of this range, it will be clamped to fit within the range.
     * <p>
     * The resulting value will be remapped to the range of {@code min} to {@code max}.
     * 
     * @param value The normalized value to set this control to.
     */
    public void setNormalized(float value) {
        this.value = MathUtilities.remapClamped(value, 0f, 1f, min, max);
        eventDispatcher.dispatch(AudioControlEventType.VALUE_CHANGED, new AudioControlEvent(this));
    }
    
    /**
     * Retrieves the current value of this control.
     * 
     * @return The current value of this control.
     */
    public float getValue() {
        return value;
    }

    /**
     * Retrieves the minimum bound of the range for this control.
     * 
     * @return The minimum value that this control can have.
     */
    public float getMin() {
        return min;
    }


    /**
     * Retrieves the maximum bound of the range for this control.
     * 
     * @return The maximum value that this control can have.
     */
    public float getMax() {
        return max;
    }

    /**
     * Retrieves the normalized value of this control within the range [0, 1],
     * where 0 represents the minimum value and 1 represents the maximum value.
     * 
     * @return The normalized value of this control.
     */
    public float getNormalized() {
        return (float)MathUtilities.remapClamped(value, min, max, 0f, 1f);
    }

    /**
     * Returns a string representation of the FloatControl object, displaying the name of the control,
     * its current value, and its minimum and maximum bounds.
     * 
     * @return A string that represents the float control.
     */
    @Override
    public String toString() {
        return String.format("FloatControl{Name: %s, Value: %.2f, Min: %.2f, Max: %.2f}", name, value, min, max);
    }
}
