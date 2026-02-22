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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.theko.sound.util.MathUtilities;

/**
 * Utility class providing methods for working with audio controls.
 * 
 * <p>This class is a holder for static utility methods related to audio controls.
 * It provides methods to retrieve the value of an audio control as a float,
 * and to set the value of an audio control from a float.
 * 
 * <p>This class is not intended to be instantiated.
 * 
 * @since 0.2.4-beta
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
    @SuppressWarnings("rawtypes")
    public static float getValueAsFloat(AudioControl control) {
        Objects.requireNonNull(control);
        Class<? extends AudioControl> controlClass = control.getClass();
        if (FloatControl.class.isAssignableFrom(controlClass)) {
            return ((FloatControl) control).getValue();
        } else if (BooleanControl.class.isAssignableFrom(controlClass)) {
            return ((BooleanControl) control).getValue() ? 1.0f : 0.0f;
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
    @SuppressWarnings("rawtypes")
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

    /**
     * Creates a custom FloatControl with the specified name, minimum, and maximum values.
     * The control will use the supplier to retrieve the current value of the control, and the consumer to set the value of the control.
     * 
     * @param name The name of the custom FloatControl.
     * @param min The minimum value of the control.
     * @param max The maximum value of the control.
     * @param supplier The supplier to retrieve the current value of the control.
     * @param consumer The consumer to set the value of the control.
     * @return The custom FloatControl.
     */
    public static FloatControl getCustomFloatControl(String name, float min, float max, Supplier<Float> supplier, Consumer<Float> consumer) {
        class CustomFloatControl extends FloatControl {
            private final Supplier<Float> supplier;
            private final Consumer<Float> consumer;

            public CustomFloatControl(String name, float min, float max, Supplier<Float> supplier, Consumer<Float> consumer) {
                super(name, min, max, 0.0f);
                this.supplier = supplier;
                this.consumer = consumer;
            }

            @Override
            public void setValue(float value) {
                if (consumer == null) {
                    return;
                }
                consumer.accept(MathUtilities.clamp(value, getMin(), getMax()));
            }

            @Override
            public float getValue() {
                if (supplier == null) {
                    return Math.max(0.0f, getMin());
                }
                return supplier.get();
            }
        }
        return new CustomFloatControl(name, min, max, supplier, consumer);
    }

    /**
     * Creates a custom BooleanControl with the specified name, supplier to retrieve the current value of the control,
     * and consumer to set the value of the control.
     * 
     * @param name The name of the custom BooleanControl.
     * @param supplier The supplier to retrieve the current value of the control.
     * @param consumer The consumer to set the value of the control.
     * @return The custom BooleanControl.
     */
    public static BooleanControl getCustomBooleanControl(String name, Supplier<Boolean> supplier, Consumer<Boolean> consumer) {
        class CustomBooleanControl extends BooleanControl {
            private final Supplier<Boolean> supplier;
            private final Consumer<Boolean> consumer;

            public CustomBooleanControl(String name, Supplier<Boolean> supplier, Consumer<Boolean> consumer) {
                super(name, false);
                this.supplier = supplier;
                this.consumer = consumer;
            }

            @Override
            public void setValue(boolean value) {
                if (consumer == null) {
                    return;
                }
                consumer.accept(value);
            }

            @Override
            public boolean getValue() {
                if (supplier == null) {
                    return false;
                }
                return supplier.get();
            }
        }
        return new CustomBooleanControl(name, supplier, consumer);
    }

    /**
     * Creates a custom EnumControl with the specified name, enum class, supplier to retrieve the current index of the control,
     * and consumer to set the index of the control.
     * 
     * @param name The name of the custom EnumControl.
     * @param enumClass The enum class of the control.
     * @param supplier The supplier to retrieve the current index of the control.
     * @param consumer The consumer to set the index of the control.
     * @return The custom EnumControl.
     */
    public static <T extends Enum<T>> EnumControl<T> getCustomEnumControl(
        String name,
        Class<T> enumClass,
        Supplier<Integer> supplier,
        Consumer<Integer> consumer) {

        class CustomEnumControl extends EnumControl<T> {
            private final Supplier<Integer> supplier;
            private final Consumer<Integer> consumer;

            public CustomEnumControl(String name, Class<T> enumClass, Supplier<Integer> supplier, Consumer<Integer> consumer) {
                super(name, enumClass.getEnumConstants()[0]);
                this.supplier = supplier;
                this.consumer = consumer;
            }

            @Override
            public void setValue(int value) {
                if (consumer != null) {
                    int safe = MathUtilities.clamp(value, 0, enumValues.length - 1);
                    consumer.accept(safe);
                }
            }

            @Override
            public int getEnumIndex() {
                if (supplier == null) return 0;
                int idx = supplier.get();
                return MathUtilities.clamp(idx, 0, enumValues.length - 1);
            }

            @Override
            public T getEnumValue() {
                return enumValues[getEnumIndex()];
            }
        }

        return new CustomEnumControl(name, enumClass, supplier, consumer);
    }
}
