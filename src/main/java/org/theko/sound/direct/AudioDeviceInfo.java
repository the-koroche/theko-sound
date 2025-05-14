package org.theko.sound.direct;

/**
 * Represents information about an audio device, including its name, version, 
 * and the class type of the audio device. This class extracts metadata from 
 * the provided audio device class, which must be annotated with {@link AudioDeviceType}.
 * 
 * <p>Usage:</p>
 * <pre>
 * {@code
 * AudioDeviceInfo info = new AudioDeviceInfo(MyAudioDevice.class);
 * System.out.println(info.getName());
 * System.out.println(info.getVersion());
 * }
 * </pre>
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioDeviceInfo {
    private final String name, version;
    private final Class<? extends AudioDevice> audioDevice;

    
    /**
     * Constructs an AudioDeviceInfo object by extracting metadata from the given audio device class.
     * The class must be annotated with {@link AudioDeviceType}.
     *
     * @param audioDevice The audio device class to extract information from.
     * @throws IllegalArgumentException if the class is not annotated with {@link AudioDeviceType}.
     */
    public AudioDeviceInfo(Class<? extends AudioDevice> audioDevice) {
        // Check if the audio device class has the AudioDeviceType annotation
        if (audioDevice.isAnnotationPresent(AudioDeviceType.class)) {
            // Retrieve the annotation to extract name and version
            AudioDeviceType audioDeviceType = audioDevice.getAnnotation(AudioDeviceType.class);
            this.name = audioDeviceType.name();
            this.version = audioDeviceType.version();
            this.audioDevice = audioDevice;
        } else {
            // Throw an exception if the annotation is not present
            throw new IllegalArgumentException("The provided audio device class doesn't provide info about itself.");
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Returns the class of the audio device this info object represents.
     *
     * @return The class of the audio device.
     */
    public Class<? extends AudioDevice> getDeviceClass() {
        return audioDevice;
    }

    @Override
    public String toString() {
        return "AudioDeviceInfo {Class: " + audioDevice.getSimpleName() + ", Name: " + name + ", Version: " + version + "}";
    }
}
