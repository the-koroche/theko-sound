package org.theko.sound;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The {@code AudioPreset} class represents an audio preset that can be saved to or loaded from a file.
 * It includes metadata such as the name and description of the preset, as well as the state of an
 * associated {@code AudioObject}.
 * 
 * <p>This class provides methods to:
 * <ul>
 *   <li>Load an audio preset from a file</li>
 *   <li>Save an audio preset to a file</li>
 *   <li>Retrieve the name, description, and associated audio object</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * AudioPreset preset = new AudioPreset("My Preset", "A description of the preset");
 * preset.save(new File("preset.dat"), audioObject);
 * preset.load(new File("preset.dat"));
 * }
 * </pre>
 * 
 * <p>Note: The {@code AudioObject} class must implement the {@code Serializable} interface and provide
 * methods {@code onLoad()} and {@code onSave()} to handle custom logic during loading and saving.
 * 
 * <p>Exceptions such as {@code FileNotFoundException} and {@code AudioObjectIOException} are thrown
 * to indicate errors during file operations or invalid audio object states.
 * 
 * @author Alex Soloviov
 */
public class AudioPreset implements Serializable {
    private static final long serialVersionUID = -1;
    private static final Logger logger = LoggerFactory.getLogger(AudioPreset.class);
    
    private String name;
    private String description;
    private AudioObject audioObjectState;

    public AudioPreset(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AudioObject getAudioObject() {
        return audioObjectState;
    }

    /**
     * Loads the audio object state from a file. The file must be a valid audio preset file. 
     * The onLoad method is called after loading the object.
     * 
     * @param file the file to load the audio object state from
     * @throws FileNotFoundException if the file is not found
     * @throws AudioObjectIOException if there is an error loading the audio object
     */
    public void load(File file) throws FileNotFoundException, AudioObjectIOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            AudioPreset preset = (AudioPreset) ois.readObject();
            AudioObject aobj = preset.getAudioObject();
            this.name = preset.name;
            this.description = preset.description;
            aobj.onLoad(); // Call onLoad method after loading the object
            this.audioObjectState = aobj;
        } catch (FileNotFoundException e) {
            logger.error("File not found.", e);
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        } catch (AudioObjectIOException e) {
            logger.error("Audio object I/O error.", e);
            throw new AudioObjectIOException("Audio object I/O error", e);
        } catch (ClassCastException e) {
            logger.error("Invalid object type.", e);
            throw new AudioObjectIOException("Invalid object type", e);
        } catch (ClassNotFoundException e) {
            logger.error("Class not found.", e);
            throw new AudioObjectIOException("Class not found", e);
        } catch (IOException e) {
            logger.error("Error reading file.", e);
            throw new AudioObjectIOException("Error reading file", e);
        } catch (Exception e) {
            logger.error("Unchecked exception.", e);
            throw new AudioObjectIOException(e);
        }
    }

    /**
     * Saves the audio object to the specified file. Returns true if the operation
     * succeeds, false otherwise.
     *
     * @param file the file to save the audio object to
     * @param audioObject the audio object to save
     * @return true if the operation succeeds, false otherwise
     */
    public boolean save(File file, AudioObject audioObject) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            audioObject.onSave(); // Call onSave method before saving the object
            this.audioObjectState = audioObject;
            oos.writeObject(this);
            return true;
        } catch (FileNotFoundException e) {
            logger.error("File not found: " + e.getMessage());
            return false;
        } catch (IOException e) {
            logger.error("I/O error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unknown error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns a string representation of the AudioPreset object, including the name and description.
     *
     * @return A string that represents the audio preset.
     */
    @Override
    public String toString() {
        return String.format("AudioPreset {Name: %s, Description: %s}", name, description);
    }
}