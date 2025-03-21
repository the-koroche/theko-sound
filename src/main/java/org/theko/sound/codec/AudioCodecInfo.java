package org.theko.sound.codec;

public class AudioCodecInfo {
    private final String name, extension, version;
    private final Class<? extends AudioCodec> codecClass;

    public AudioCodecInfo(Class<? extends AudioCodec> codecClass) {
                if (codecClass.isAnnotationPresent(AudioCodecType.class)) {
            AudioCodecType audioCodecType = codecClass.getAnnotation(AudioCodecType.class);
            this.name = audioCodecType.name();
            this.extension = audioCodecType.extension();
            this.version = audioCodecType.version();
            this.codecClass = codecClass;
        } else {
            throw new IllegalArgumentException("The provided audio codec class doesn't provide info about itself.");
        }
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String getVersion() {
        return version;
    }

    public Class<? extends AudioCodec> getCodecClass() {
        return codecClass;
    }

    @Override
    public String toString() {
        return "AudioCodecInfo {Class: " + codecClass.getSimpleName() + ", Name: " + name + ", Extension: " + extension + ", Version: " + version + "}";
    }
}
