package org.theko.sound.control;

import java.lang.reflect.Field;
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
 * <p>Methods included:
 * <ul>
 *   <li>{@link #getAllControls()}: Retrieves all AudioControl objects declared as fields.</li>
 *   <li>{@link #getControl(String)}: Retrieves a specific AudioControl by its name.</li>
 *   <li>{@link #getFloatControl(String)}: Retrieves a FloatControl by its name.</li>
 *   <li>{@link #getBooleanControl(String)}: Retrieves a BooleanControl by its name.</li>
 * </ul>
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
 * @since v1.4.1
 * 
 * @author Theko
 */
public interface Controllable {
    /**
     * Retrieves all AudioControl objects declared as fields in the implementing class.
     *
     * This method uses reflection to access all declared fields in the implementing class. 
     * It collects fields that are instances of AudioControl or its subclasses, 
     * adds them to a list, and returns an unmodifiable view of this list. 
     * If a field is inaccessible due to security restrictions, it is ignored.
     *
     * @return An unmodifiable list of AudioControl objects found in the implementing class.
     */
    default List<AudioControl> getAllControls() {
        List<AudioControl> controls = new ArrayList<>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (AudioControl.class.isAssignableFrom(field.getType())) {
                try {
                    AudioControl control = (AudioControl) field.get(this);
                    if (control != null) {
                        controls.add(control);
                    }
                } catch (IllegalAccessException ex) {
                    // Ignore inaccesible fields
                }
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
}
