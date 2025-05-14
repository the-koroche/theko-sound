package org.theko.sound.control;

/**
 * The {@code AudioControl} class serves as an abstract base class for audio control components.
 * It provides a common structure for managing audio controls with a name property.
 * 
 * <p>This class is intended to be extended by specific types of audio controls.
 * Subclasses must implement additional functionality as required.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Stores a name for the audio control.</li>
 *   <li>Provides a method to retrieve the name.</li>
 *   <li>Overrides {@code toString()} to provide a string representation of the audio control.</li>
 * </ul>
 * 
 * @see BooleanControl
 * @see FloatControl
 * @see Controllable
 * 
  * @since v1.2.0
* 
* @author Theko
 */
public abstract class AudioControl {
    protected final String name;

    public AudioControl (String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of this audio control.
     *
     * @return The name of the audio control.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of the AudioControl object, 
     * displaying the name of the control.
     *
     * @return A string that represents the audio control.
     */
    @Override
    public String toString() {
        return String.format("AudioControl {Name: %s}", name);
    }
}
