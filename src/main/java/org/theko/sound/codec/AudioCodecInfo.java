package org.theko.sound.codec;

/**
 * Represents metadata information about an audio codec. This class extracts
 * codec details such as name, file extensions, and version from the provided
 * codec class, which must be annotated with {@link AudioCodecType}.
 * 
 * <p>
 * The metadata includes:
 * <ul>
 *   <li>Name of the codec</li>
 *   <li>File extensions associated with the codec</li>
 *   <li>Version of the codec</li>
 *   <li>The codec class itself</li>
 * </ul>
 * </p>
 * 
 * <p>
 * If the provided codec class is not annotated with {@link AudioCodecType},
 * an {@link IllegalArgumentException} is thrown.
 * </p>
 * 
 * @since v1.3.1
 * @author Theko
 */
public class AudioCodecInfo {

    private final String name;
    private final String[] extensions;
    private final String version;
    private final Class<? extends AudioCodec> codecClass;

    public AudioCodecInfo (Class<? extends AudioCodec> codecClass) {
                if (codecClass.isAnnotationPresent(AudioCodecType.class)) {
            AudioCodecType audioCodecType = codecClass.getAnnotation(AudioCodecType.class);
            this.name = audioCodecType.name();
            this.extensions = audioCodecType.extensions();
            this.version = audioCodecType.version();
            this.codecClass = codecClass;
        } else {
            throw new IllegalArgumentException("The provided audio codec class doesn't provide info about itself.");
        }
    }

    public String getName () {
        return name;
    }

    public String[] getExtensions () {
        return extensions;
    }

    public String getExtension () {
        return extensions[0];
    }

    public String getVersion () {
        return version;
    }

    public Class<? extends AudioCodec> getCodecClass () {
        return codecClass;
    }

    @Override
    public String toString () {
        return "AudioCodecInfo {Class: " + codecClass.getSimpleName() + ", Name: " + name + ", Extensions: [" + String.join(", ", extensions) + "], Version: " + version + "}";
    }
}
