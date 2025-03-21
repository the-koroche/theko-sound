package org.theko.sound.direct;

public class AudioDeviceInfo {
    private final String name, version;
    private final Class<? extends AudioDevice> audioDevice;

    public AudioDeviceInfo(Class<? extends AudioDevice> audioDevice) {
        if (audioDevice.isAnnotationPresent(AudioDeviceType.class)) {
            AudioDeviceType audioDeviceType = audioDevice.getAnnotation(AudioDeviceType.class);
            this.name = audioDeviceType.name();
            this.version = audioDeviceType.version();
            this.audioDevice = audioDevice;
        } else {
            throw new IllegalArgumentException("The provided audio device class doesn't provide info about itself.");
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Class<? extends AudioDevice> getDeviceClass() {
        return audioDevice;
    }

    @Override
    public String toString() {
        return "AudioDeviceInfo {Class: " + audioDevice.getSimpleName() + ", Name: " + name + ", Version: " + version + "}";
    }
}
