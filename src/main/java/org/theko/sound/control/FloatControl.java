package org.theko.sound.control;

import org.theko.sound.event.AudioControlEvent;
import org.theko.sound.utility.MathUtilities;

/**
 * The {@code FloatControl} class represents a control for managing a floating-point value
 * within a specified range. It extends the {@code AudioControl} class and provides methods
 * to set, retrieve, and normalize the value.
 * 
 * <p>This class is useful for controlling parameters such as volume, pitch, or other
 * audio-related properties that require a floating-point range.
 * 
 * <p>Features:
 * <ul>
 *   <li>Set and retrieve the current value within the specified range.</li>
 *   <li>Retrieve the minimum and maximum bounds of the range.</li>
 *   <li>Normalize the value to a range of 0.0 to 1.0.</li>
 * </ul>
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
 * @since v1.2.0
 * @author Theko
 */
public class FloatControl extends AudioControl {

    protected float value;
    protected final float min, max;

    
    public FloatControl (String name, float min, float max, float value) {
        super(name);
        this.min = min;
        this.max = max;
        this.value = Math.clamp(value, min, max);
    }

    /**
     * Sets the value of this control to the given value, ensuring that the
     * new value is within the range of {@code min} and {@code max}.
     * 
     * @param value The new value for this control.
     */
    public void setValue (float value) {
        this.value = Math.clamp(value, min, max);
        notifyListeners(NotifyType.VALUE_CHANGE, new AudioControlEvent(this));
    }
    
    /**
     * Retrieves the current value of this control.
     * 
     * @return The current value of this control.
     */
    public float getValue () {
        return value;
    }

    /**
     * Retrieves the minimum bound of the range for this control.
     * 
     * @return The minimum value that this control can have.
     */
    public float getMin () {
        return min;
    }


    /**
     * Retrieves the maximum bound of the range for this control.
     * 
     * @return The maximum value that this control can have.
     */
    public float getMax () {
        return max;
    }

    /**
     * Retrieves the normalized value of this control within the range [0, 1], 
     * where 0 represents the minimum value and 1 represents the maximum value.
     * 
     * @return The normalized value of this control.
     */
    public float getNormalized () {
        return (float)MathUtilities.remapClamped(value, min, max, 0f, 1f);
    }

    /**
     * Returns a string representation of the FloatControl object, displaying the name of the control, 
     * its current value, and its minimum and maximum bounds.
     * 
     * @return A string that represents the float control.
     */
    @Override
    public String toString () {
        return String.format("FloatControl {Name: %s, Value: %.2f, Min: %.2f, Max: %.2f}", name, value, min, max);
    }
}
