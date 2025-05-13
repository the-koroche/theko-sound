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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.direct.AudioDevice;
import org.theko.sound.direct.AudioDeviceInfo;
import org.theko.sound.direct.AudioDeviceType;
import org.theko.sound.direct.AudioInputDevice;
import org.theko.sound.direct.AudioOutputDevice;

/**
 * The {@code AudioDevices} class provides a centralized management system for audio devices.
 * It allows for the registration, retrieval, and instantiation of audio input and output devices.
 * This class is designed to be thread-safe and ensures that all registered audio devices are
 * accessible throughout the application.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Automatic registration of audio devices annotated with {@link AudioDeviceType}.</li>
 *   <li>Retrieval of audio devices by name or class type.</li>
 *   <li>Support for platform-specific default audio devices.</li>
 *   <li>Thread-safe storage and access to registered audio devices.</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * {@code
 * // Retrieve an audio device by name
 * AudioDeviceInfo deviceInfo = AudioDevices.fromName("WASAPI");
 *
 * // Get an input device instance
 * AudioInputDevice inputDevice = AudioDevices.getInputDevice(deviceInfo);
 *
 * // Get the default platform-specific audio device
 * AudioDeviceInfo platformDevice = AudioDevices.getPlatformDevice();
 * }
 * </pre>
 *
 * <p>Note: This class cannot be instantiated as it is designed to be a utility class.</p>
 *
 * @see AudioDevice
 * @see AudioDeviceInfo
 * @see AudioDeviceType
 * @see AudioInputDevice
 * @see AudioOutputDevice
 * 
 * @author Alex Soloviov
 */
public class AudioDevices {
    private static final Logger logger = LoggerFactory.getLogger(AudioDevices.class);

    private AudioDevices() { }

    // A thread-safe collection to store registered audio devices.
    private static final Collection<AudioDeviceInfo> audioDevices = Collections.synchronizedSet(new LinkedHashSet<>());

    static {
        registerDevices();
    }

    /**
     * Scans and registers all available audio devices that are annotated with {@link AudioDeviceType}.
     */
    private static void registerDevices() {
        // Attempt to scan all available packages for audio devices.
        audioDevices.clear();
        Set<Class<? extends AudioDevice>> allAudioDevices = AudioClassLoader.getAvailableDevices();

        // Register all found audio devices.
        for (Class<? extends AudioDevice> audioDeviceClass : allAudioDevices) {
            if (audioDeviceClass.isAnnotationPresent(AudioDeviceType.class)) {
                AudioDeviceInfo deviceInfo = new AudioDeviceInfo(audioDeviceClass);
                audioDevices.add(deviceInfo);
                logger.info("Found audio device: " + deviceInfo);
            } else {
                logger.info("Found audio device without information: " + audioDeviceClass.getSimpleName());
            }
        }
    }

    /**
     * Retrieves an {@link AudioDeviceInfo} by its name.
     *
     * @param name The name of the audio device.
     * @return The corresponding {@link AudioDeviceInfo}.
     * @throws AudioDeviceNotFoundException If no device is found with the given name.
     */
    public static AudioDeviceInfo fromName(String name) throws AudioDeviceNotFoundException {
        for (AudioDeviceInfo audioDevice : audioDevices) {
            if (audioDevice.getName().equalsIgnoreCase(name)) {
                return audioDevice;
            }
        }
        logger.error("No audio devices found by name: '" + name + "'.");
        throw new AudioDeviceNotFoundException("No audio devices found by name: '" + name + "'.");
    }

    /**
     * Retrieves an {@link AudioInputDevice} instance by its class type.
     *
     * @param audioDeviceClass The class of the desired input device.
     * @return The instantiated {@link AudioInputDevice}.
     * @throws AudioDeviceNotFoundException If the device is not registered.
     * @throws AudioDeviceCreationException If an error occurs during instantiation.
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
                    logger.error("Failed to instantiate input device: " + audioDeviceClass.getSimpleName() + ".", e);
                    throw new AudioDeviceCreationException("Failed to instantiate input device: " + audioDeviceClass.getSimpleName(), e);
                }
            }
        }
        logger.error("No input device found for class: " + audioDeviceClass.getSimpleName());
        throw new AudioDeviceNotFoundException("No input device found for class: " + audioDeviceClass.getSimpleName());
    }

    public static AudioInputDevice getInputDevice(AudioDeviceInfo audioDeviceInfo) throws AudioDeviceCreationException {
        try {
            return getInputDevice(audioDeviceInfo.getDeviceClass());
        } catch (AudioDeviceNotFoundException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves an {@link AudioOutputDevice} instance by its class type.
     *
     * @param audioDeviceClass The class of the desired output device.
     * @return The instantiated {@link AudioOutputDevice}.
     * @throws AudioDeviceNotFoundException If the device is not registered.
     * @throws AudioDeviceCreationException If an error occurs during instantiation.
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
                    logger.error("Failed to instantiate input device: " + audioDeviceClass.getSimpleName() + ".", e);
                    throw new AudioDeviceCreationException("Failed to instantiate output device: " + audioDeviceClass.getSimpleName(), e);
                }
            }
        }
        logger.error("No output device found for class: " + audioDeviceClass.getSimpleName());
        throw new AudioDeviceNotFoundException("No output device found for class: " + audioDeviceClass.getSimpleName());
    }

    public static AudioOutputDevice getOutputDevice(AudioDeviceInfo audioDeviceInfo) throws AudioDeviceCreationException {
        try {
            return getOutputDevice(audioDeviceInfo.getDeviceClass());
        } catch (AudioDeviceNotFoundException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * Returns all registered audio devices.
     *
     * @return A collection of all registered {@link AudioDeviceInfo} instances.
     */
    public static Collection<AudioDeviceInfo> getAllDevices() {
        return audioDevices;
    }

    /**
     * Retrieves the default platform-specific audio device.
     *
     * @return The best matching platform-specific audio device.
     * @throws AudioDeviceNotFoundException If no suitable device is found.
     */
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
                        AudioDeviceInfo deviceInfo = fromName(device);
                        logger.debug("Found platform device: " + device + ", Class: " + deviceInfo.getDeviceClass().getSimpleName());
                        return deviceInfo;
                    } catch (AudioDeviceNotFoundException ignored) { }
                }
            }
        }
    
        // Fallback to JavaSound if no platform-specific device is found.
        String fallbackDeviceName = "JavaSound";
        logger.info("No compatible audio devices for this platform founded. Using fallback: " + fallbackDeviceName);
        try {
            return fromName(fallbackDeviceName);
        } catch (AudioDeviceNotFoundException ex) {
            // Already logged in 'fromName' method.
            // logger.error("No fallback device found by name: '" + fallbackDeviceName + "'.", ex);
            throw new AudioDeviceNotFoundException("No fallback device found by name: '" + fallbackDeviceName + "'.", ex);
        }
    }
}
