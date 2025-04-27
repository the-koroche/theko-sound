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

public abstract class AudioPreset implements Serializable {
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

    public void load(File file) throws FileNotFoundException, AudioObjectIOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            AudioPreset preset = (AudioPreset) ois.readObject();
            AudioObject aobj = preset.getAudioObject();
            this.name = preset.name;
            this.description = preset.description;
            aobj.onLoad(); // Call onLoad method after loading the object
            this.audioObjectState = aobj;
        } catch (FileNotFoundException e) {
            logger.error("File not found: " + e.getMessage());
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        } catch (AudioObjectIOException e) {
            logger.error("Audio object I/O error: " + e.getMessage());
            throw new AudioObjectIOException("Audio object I/O error", e);
        } catch (ClassCastException e) {
            logger.error("Invalid object type: " + e.getMessage());
            throw new AudioObjectIOException("Invalid object type", e);
        } catch (ClassNotFoundException e) {
            logger.error("Class not found: " + e.getMessage());
            throw new AudioObjectIOException("Class not found", e);
        } catch (IOException e) {
            logger.error("Error reading file: " + e.getMessage());
            throw new AudioObjectIOException("Error reading file", e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new AudioObjectIOException("Unknown error", e);
        }
    }

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
}