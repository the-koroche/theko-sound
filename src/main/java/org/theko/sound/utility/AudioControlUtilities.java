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

package org.theko.sound.utility;

import java.util.Objects;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.EnumControl;
import org.theko.sound.control.FloatControl;

/**
 * Utility class providing methods for working with audio controls.
 * 
 * <p>This class is a holder for static utility methods related to audio controls.
 * It provides methods to retrieve the value of an audio control as a float,
 * and to set the value of an audio control from a float.
 * 
 * <p>This class is not intended to be instantiated.
 * 
 * @since 2.4.1
 * @author Theko
 */
public final class AudioControlUtilities {
    
    private AudioControlUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Retrieves the value of an audio control as a float.
     * 
     * <p>This method is useful for retrieving the value of an audio control without knowing its type.
     * It will return the value of the control as a float in the range of 0.0 to 1.0.
     * If the control is of type FloatControl, it will return the value directly.
     * If the control is of type BooleanControl, it will return 1.0 if the control is enabled, 0.0 if disabled.
     * If the control is of type EnumControl, it will return the index of the enum constant as a float.
     * 
     * @param control The audio control to retrieve the value from.
     * @return The value of the audio control as a float.
     * @throws IllegalArgumentException If the control type is unsupported.
     */
    public static float getValueAsFloat(AudioControl control) {
        Objects.requireNonNull(control);
        Class<? extends AudioControl> controlClass = control.getClass();
        if (FloatControl.class.isAssignableFrom(controlClass)) {
            return ((FloatControl) control).getValue();
        } else if (BooleanControl.class.isAssignableFrom(controlClass)) {
            return ((BooleanControl) control).isEnabled() ? 1.0f : 0.0f;
        } else if (EnumControl.class.isAssignableFrom(controlClass)) {
            return (float) ((EnumControl) control).getEnumIndex();
        } else {
            throw new IllegalArgumentException("Unsupported control type: " + controlClass.getName());
        }
    }

    /**
     * Sets the value of an audio control from a float.
     * 
     * <p>This method is useful for setting the value of an audio control without knowing its type.
     * It will set the value of the control from a float in the range of 0.0 to 1.0.
     * If the control is of type FloatControl, it will set the value directly.
     * If the control is of type BooleanControl, it will set the control to enabled if the value is greater than 0.5, disabled otherwise.
     * If the control is of type EnumControl, it will set the enum index from the float value.
     * 
     * @param control The audio control to set the value from.
     * @param value The float representing the value to set.
     * @throws IllegalArgumentException If the control type is unsupported.
     */
    public static void setValueFromFloat(AudioControl control, float value) {
        Objects.requireNonNull(control);
        Class<? extends AudioControl> controlClass = control.getClass();
        if (FloatControl.class.isAssignableFrom(controlClass)) {
            ((FloatControl) control).setValue(value);
        } else if (BooleanControl.class.isAssignableFrom(controlClass)) {
            ((BooleanControl) control).setValue(value > 0.5f);
        } else if (EnumControl.class.isAssignableFrom(controlClass)) {
            ((EnumControl) control).setValue((int) value);
        } else {
            throw new IllegalArgumentException("Unsupported control type: " + controlClass.getName());
        }
    }
}
