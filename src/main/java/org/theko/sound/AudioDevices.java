package org.theko.sound;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.theko.sound.direct.AudioDevice;
import org.theko.sound.direct.AudioDeviceInfo;
import org.theko.sound.direct.AudioDeviceType;
import org.theko.sound.direct.AudioInputDevice;
import org.theko.sound.direct.AudioOutputDevice;

public class AudioDevices {
    private AudioDevices() { }

    // A collection to hold all registered audio devices
    private static final Collection<AudioDeviceInfo> audioDevices = Collections.synchronizedSet(new LinkedHashSet<>());


    static {
        registerDevices();
    }

    /**
     * Register all audio devices that are annotated with AudioDeviceType
     */
    private static void registerDevices() {
        // Use Reflections to find all classes implementing AudioDevice
        Reflections reflections = null;
        try {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("") // Сканировать всё
                    .addScanners(Scanners.SubTypes) // Искать подтипы
            );
        } catch (ReflectionsException ex) {
            ex.printStackTrace();
            reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound") // Сканировать только установленные классы (пропустить пользовательские)
                .addScanners(Scanners.SubTypes) // Искать подтипы
            );
        }
        audioDevices.clear();
        Set<Class<? extends AudioDevice>> allAudioDevices = reflections.getSubTypesOf(AudioDevice.class);

        for (Class<? extends AudioDevice> audioDeviceClass : allAudioDevices) {
            if (audioDeviceClass.isAnnotationPresent(AudioDeviceType.class)) {
                AudioDeviceInfo deviceInfo = new AudioDeviceInfo(audioDeviceClass);
                audioDevices.add(deviceInfo);
                System.out.println(deviceInfo);
            }
        }
    }

    /**
     * Get an AudioDeviceInfo by its name
     */
    public static AudioDeviceInfo fromName(String name) throws AudioDeviceNotFoundException {
        for (AudioDeviceInfo audioDevice : audioDevices) {
            if (audioDevice.getName().equalsIgnoreCase(name)) {
                return audioDevice;
            }
        }
        throw new AudioDeviceNotFoundException("No audio devices found by name: '" + name + "'.");
    }

    /**
     * Get an AudioInputDevice by its class type
     */
    public static AudioInputDevice getInputDevice(Class<? extends AudioDevice> audioDeviceClass) throws AudioDeviceNotFoundException, AudioDeviceCreationException {
        for (AudioDeviceInfo deviceInfo : audioDevices) {
            if (deviceInfo.getDeviceClass().equals(audioDeviceClass)) {
                try {
                    Constructor<? extends AudioDevice> constructor = audioDeviceClass.getDeclaredConstructor();
                    if (!Modifier.isAbstract(audioDeviceClass.getModifiers()) && Modifier.isPublic(constructor.getModifiers())) {
                        return constructor.newInstance().getInputDevice();
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                    throw new AudioDeviceCreationException("Failed to instantiate input device: " + audioDeviceClass.getSimpleName(), e);
                }
            }
        }
        throw new AudioDeviceNotFoundException("No input device found for class: " + audioDeviceClass.getSimpleName());
    }

    /**
     * Get an AudioOutputDevice by its class type
     */
    public static AudioOutputDevice getOutputDevice(Class<? extends AudioDevice> audioDeviceClass) throws AudioDeviceNotFoundException, AudioDeviceCreationException {
        for (AudioDeviceInfo deviceInfo : audioDevices) {
            if (deviceInfo.getDeviceClass().equals(audioDeviceClass)) {
                try {
                    Constructor<? extends AudioDevice> constructor = audioDeviceClass.getDeclaredConstructor();
                    if (!Modifier.isAbstract(audioDeviceClass.getModifiers()) && Modifier.isPublic(constructor.getModifiers())) {
                        return constructor.newInstance().getOutputDevice();
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
                    throw new AudioDeviceCreationException("Failed to instantiate output device: " + audioDeviceClass.getSimpleName(), e);
                }
            }
        }
        throw new AudioDeviceNotFoundException("No output device found for class: " + audioDeviceClass.getSimpleName());
    }

    /**
     * Get all registered audio devices
     */
    public static Collection<AudioDeviceInfo> getAllDevices() {
        return audioDevices;
    }

    public static AudioDeviceInfo getPlatformDevice() throws AudioDeviceNotFoundException {
        String name = System.getProperty("os.name").toLowerCase();
    
        Map<String, List<String>> platformDevices = Map.of(
            "win", List.of("WASAPI", "DirectSound"),
            "linux", List.of("ALSA", "PulseAudio"),
            "mac", List.of("CoreAudio")
        );
    
        for (Map.Entry<String, List<String>> entry : platformDevices.entrySet()) {
            if (name.contains(entry.getKey())) {
                for (String device : entry.getValue()) {
                    try {
                        return fromName(device);
                    } catch (AudioDeviceNotFoundException ignored) { }
                }
            }
        }
    
        // Фоллбек к JavaSound
        return fromName("JavaSound");
    }
}
