package org.theko.sound.backend;

/**
 * Represents information about an audio backend, including its name, version, 
 * and the class type of the audio backend. This class extracts metadata from 
 * the provided audio backend class, which must be annotated with {@link AudioBackendType}.
 * 
 * <p>Usage:</p>
 * <pre>
 * {@code
 * AudioBackendInfo info = new AudioBackendInfo(MyAudioBackend.class);
 * System.out.println(info.getName());
 * System.out.println(info.getVersion());
 * }
 * </pre>
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioBackendInfo {
    private final String name, version;
    private final Class<? extends AudioBackend> audioBackend;

    
    /**
     * Constructs an AudioBackendInfo object by extracting metadata from the given audio backend class.
     * The class must be annotated with {@link AudioBackendType}.
     *
     * @param audioBackend The audio backend class to extract information from.
     * @throws IllegalArgumentException if the class is not annotated with {@link AudioBackendType}.
     */
    public AudioBackendInfo(Class<? extends AudioBackend> audioBackend) {
        // Check if the audio backend class has the AudioBackendType annotation
        if (audioBackend.isAnnotationPresent(AudioBackendType.class)) {
            // Retrieve the annotation to extract name and version
            AudioBackendType audioBackendType = audioBackend.getAnnotation(AudioBackendType.class);
            this.name = audioBackendType.name();
            this.version = audioBackendType.version();
            this.audioBackend = audioBackend;
        } else {
            // Throw an exception if the annotation is not present
            throw new IllegalArgumentException("The provided audio backend class doesn't provide info about itself.");
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Returns the class of the audio backend this info object represents.
     *
     * @return The class of the audio backend.
     */
    public Class<? extends AudioBackend> getBackendClass() {
        return audioBackend;
    }

    @Override
    public String toString() {
        return "AudioBackendInfo {Class: " + audioBackend.getSimpleName() + ", Name: " + name + ", Version: " + version + "}";
    }
}
