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

/**
 * The {@code BooleanControl} class represents a type of {@link AudioControl}
 * that holds a boolean value. It provides methods to set and retrieve the
 * boolean value, as well as a string representation of the control.
 * 
 * <p>This class is typically used to manage audio-related boolean settings,
 * such as enabling or disabling a feature.
 * 
 * @since 1.2.0
 * @author Theko
 */
public class BooleanControl extends AudioControl {

    protected boolean value;

    public BooleanControl(String name, boolean value) {
        super(name);
        this.value = value;
    }
    
    /**
     * Sets the boolean value of this BooleanControl to the given value.
     * 
     * @param value The boolean value to set. If true, the control is enabled; if false, the control is disabled.
     */
    public void setValue(boolean value) {
        this.value = value;
        notifyListeners(NotifyType.VALUE_CHANGE, new AudioControlEvent(this));
    }

    /**
     * Checks if the current boolean value is true.
     *
     * @return {@code true} if the value is true, otherwise {@code false}.
     */
    public boolean isEnabled() {
        return value;
    }

    /**
     * Checks if the current boolean value is false.
     *
     * @return {@code true} if the value is false, otherwise {@code false}.
     */
    public boolean isDisabled() {
        return !value;
    }

    /**
     * Returns a string representation of the BooleanControl object, displaying the name of the control
     * and the boolean value.
     * 
     * @return A string that represents the boolean control.
     */
    @Override
    public String toString() {
        return String.format("AudioControl {Name: %s, Value: %b}", name, value);
    }
}
