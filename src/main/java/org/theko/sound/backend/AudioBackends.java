/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.backend;

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
import org.theko.sound.AudioClassRegister;
import org.theko.sound.utility.PlatformUtilities;
import org.theko.sound.utility.PlatformUtilities.Platform;

/**
 * The {@code AudioBackends} class provides a centralized management system for audio backends.
 * It allows for the registration, retrieval, and instantiation of audio input and output backends.
 * This class is designed to be thread-safe and ensures that all registered audio backends are
 * accessible throughout the application.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Automatic registration of audio backends annotated with {@link AudioBackendType}.</li>
 *   <li>Retrieval of audio backends by name or class type.</li>
 *   <li>Support for platform-specific default audio backends.</li>
 *   <li>Thread-safe storage and access to registered audio backends.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code
 * // Retrieve an audio backend by name
 * AudioBackendInfo backendInfo = AudioBackends.fromName("WASAPI");
 *
 * // Get an input backend instance
 * AudioInputBackend inputBackend = AudioBackends.getInputBackend(backendInfo);
 *
 * // Get the default platform-specific audio backend
 * AudioBackendInfo platformBackend = AudioBackends.getPlatformBackend();
 * }
 * </pre>
 *
 * <p>Note: This class cannot be instantiated as it is designed to be a utility class.
 *
 * @see AudioBackend
 * @see AudioBackendInfo
 * @see AudioBackendType
 * @see AudioInputBackend
 * @see AudioOutputBackend
 * 
 * @since 1.0.0
 * @author Theko
 */
public class AudioBackends {

    private static final Logger logger = LoggerFactory.getLogger(AudioBackends.class);

    private static AudioBackendInfo platformBackendInfo;

    private AudioBackends() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    // A thread-safe collection to store registered audio backends.
    private static final Collection<AudioBackendInfo> audioBackends = Collections.synchronizedSet(new LinkedHashSet<>());

    static {
        registerBackends();
    }

    /**
     * Scans and registers all available audio backends that are annotated with {@link AudioBackendType}.
     */
    public static void registerBackends() {
        // Attempt to scan all available packages for audio backends.
        audioBackends.clear();
        Set<Class<? extends AudioBackend>> allAudioBackends = AudioClassRegister.getBackendClasses();

        // Register all found audio backends.
        for (Class<? extends AudioBackend> audioBackendClass : allAudioBackends) {
            if (audioBackendClass.isAnnotationPresent(AudioBackendType.class)) {
                AudioBackendInfo backendInfo = new AudioBackendInfo(audioBackendClass);
                audioBackends.add(backendInfo);
                logger.info("Found audio backend: " + backendInfo);
            } else {
                logger.info("Found audio backend without information: " + audioBackendClass.getSimpleName());
            }
        }
        
        try {
            platformBackendInfo = detectPlatformBackend();
        } catch (AudioBackendNotFoundException e) {
            logger.error("Failed to detect platform-specific audio backend.", e);
        } catch (AudioBackendCreationException e) {
            logger.error("Failed to create platform-specific audio backend.", e);
        }
    }

    /**
     * Retrieves an {@link AudioBackendInfo} by its name.
     * The name is case-insensitive.
     *
     * @param name The name of the audio backend.
     * @return The corresponding {@link AudioBackendInfo}.
     * @throws IllegalArgumentException If the audio backend name is null or empty.
     * @throws AudioBackendNotFoundException If no backend is found with the given name.
     */
    public static AudioBackendInfo fromName(String name) throws AudioBackendNotFoundException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Audio backend name cannot be null or empty.");
        }
        for (AudioBackendInfo audioBackend : audioBackends) {
            if (audioBackend.getName().equalsIgnoreCase(name)) {
                return audioBackend;
            }
        }
        logger.error("No audio backends found by name: '" + name + "'.");
        throw new AudioBackendNotFoundException("No audio backends found by name: '" + name + "'.");
    }

    /**
     * Retrieves an {@link AudioBackend} by its class type.
     *
     * @param audioBackendClass The class type of the audio backend.
     * @return The corresponding {@link AudioBackend}.
     * @throws IllegalArgumentException If the audio backend class is null.
     * @throws AudioBackendNotFoundException If no backend is found with the given class type.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioBackend getBackend(Class<? extends AudioBackend> audioBackendClass) throws AudioBackendNotFoundException, AudioBackendCreationException {
        if (audioBackendClass == null) {
            throw new IllegalArgumentException("Audio backend class cannot be null.");
        }
        for (AudioBackendInfo backendInfo : audioBackends) {
            if (backendInfo.getBackendClass().equals(audioBackendClass)) {
                try {
                    Constructor<? extends AudioBackend> constructor = audioBackendClass.getDeclaredConstructor();
                    if (!Modifier.isAbstract(audioBackendClass.getModifiers()) && Modifier.isPublic(constructor.getModifiers())) {
                        return constructor.newInstance();
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    logger.error("Failed to instantiate backend: " + audioBackendClass.getSimpleName() + ".", ex);
                    throw new AudioBackendCreationException("Failed to instantiate backend: " + audioBackendClass.getSimpleName(), ex);
                }
            }
        }
        logger.error("No audio backends found by class: '{}'. (not registered?).", audioBackendClass.getSimpleName());
        throw new AudioBackendNotFoundException("No audio backends found by class: '" + audioBackendClass.getSimpleName() + "'.");
    }

    /**
     * Retrieves an {@link AudioBackend} by its {@link AudioBackendInfo}.
     *
     * @param audioBackendInfo The {@link AudioBackendInfo} of the audio backend.
     * @return The corresponding {@link AudioBackend}.
     * @throws IllegalArgumentException If the audio backend info is null.
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioBackend getBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendNotFoundException, AudioBackendCreationException {
        if (audioBackendInfo == null) {
            throw new IllegalArgumentException("Audio backend info cannot be null.");
        }
        return getBackend(audioBackendInfo.getBackendClass());
    }

    /**
     * Retrieves an {@link AudioInputBackend} instance by its class type.
     *
     * @param audioBackendClass The class of the desired input backend.
     * @return The instantiated {@link AudioInputBackend}.
     * @throws IllegalArgumentException If the audio backend class is null.
     * @throws AudioBackendNotFoundException If the backend is not registered.
     * @throws AudioBackendCreationException If an error occurs during instantiation.
     */
    public static AudioInputBackend getInputBackend(Class<? extends AudioBackend> audioBackendClass) throws AudioBackendNotFoundException, AudioBackendCreationException {
        if (audioBackendClass == null) {
            throw new IllegalArgumentException("Audio backend class cannot be null.");
        }
        for (AudioBackendInfo backendInfo : audioBackends) {
            if (backendInfo.getBackendClass().equals(audioBackendClass)) {
                try {
                    Constructor<? extends AudioBackend> constructor = audioBackendClass.getDeclaredConstructor();
                    if (!Modifier.isAbstract(audioBackendClass.getModifiers()) && Modifier.isPublic(constructor.getModifiers())) {
                        return constructor.newInstance().getInputBackend();
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    logger.error("Failed to instantiate input backend: " + audioBackendClass.getSimpleName() + ".", ex);
                    throw new AudioBackendCreationException("Failed to instantiate input backend: " + audioBackendClass.getSimpleName(), ex);
                }
            }
        }
        logger.error("No input backend found for class: '{}'. (not registered?).", audioBackendClass.getSimpleName());
        throw new AudioBackendNotFoundException("No input backend found for class: " + audioBackendClass.getSimpleName());
    }

    /**
     * Retrieves an {@link AudioInputBackend} instance by its {@link AudioBackendInfo}.
     *
     * @param audioBackendInfo The {@link AudioBackendInfo} of the audio backend.
     * @return The instantiated {@link AudioInputBackend}.
     * @throws IllegalArgumentException If the audio backend info is null.
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioInputBackend getInputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException, AudioBackendNotFoundException {
        if (audioBackendInfo == null) {
            throw new IllegalArgumentException("Audio backend info cannot be null.");
        }
        return getInputBackend(audioBackendInfo.getBackendClass());
    }

    /**
     * Retrieves an {@link AudioOutputBackend} instance by its class type.
     *
     * @param audioBackendClass The class of the desired output backend.
     * @return The instantiated {@link AudioOutputBackend}.
     * @throws AudioBackendNotFoundException If the backend is not registered.
     * @throws AudioBackendCreationException If an error occurs during instantiation.
     */
    public static AudioOutputBackend getOutputBackend(Class<? extends AudioBackend> audioBackendClass) throws AudioBackendNotFoundException, AudioBackendCreationException {
        for (AudioBackendInfo backendInfo : audioBackends) {
            if (backendInfo.getBackendClass().equals(audioBackendClass)) {
                try {
                    Constructor<? extends AudioBackend> constructor = audioBackendClass.getDeclaredConstructor();
                    if (!Modifier.isAbstract(audioBackendClass.getModifiers()) && Modifier.isPublic(constructor.getModifiers())) {
                        return constructor.newInstance().getOutputBackend();
                    }
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException ex) {
                    logger.error("Failed to instantiate input backend: " + audioBackendClass.getSimpleName() + ".", ex);
                    throw new AudioBackendCreationException("Failed to instantiate output backend: " + audioBackendClass.getSimpleName(), ex);
                }
            }
        }
        logger.error("No output backend found for class: " + audioBackendClass.getSimpleName());
        throw new AudioBackendNotFoundException("No output backend found for class: " + audioBackendClass.getSimpleName());
    }
/**
     * Retrieves an {@link AudioOutputBackend} instance by its {@link AudioBackendInfo}.
     *
     * @param audioBackendInfo The {@link AudioBackendInfo} of the audio backend.
     * @return The instantiated {@link AudioOutputBackend}.
     * @throws IllegalArgumentException If the audio backend info is null.
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioOutputBackend getOutputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException, AudioBackendNotFoundException {
        if (audioBackendInfo == null) {
            throw new IllegalArgumentException("Audio backend info cannot be null.");
        }
        return getOutputBackend(audioBackendInfo.getBackendClass());
    }

    /**
     * Returns all registered audio backends.
     *
     * @return A collection of all registered {@link AudioBackendInfo} instances.
     */
    public static Collection<AudioBackendInfo> getAllBackends() {
        return audioBackends;
    }

    /**
     * Retrieves the default platform-specific audio backend.
     *
     * @return The best matching platform-specific audio backend.
     * @throws AudioBackendNotFoundException If no suitable backend is found.
     * @throws AudioBackendCreationException 
     */
    private static AudioBackendInfo detectPlatformBackend() throws AudioBackendNotFoundException, AudioBackendCreationException {
        Platform platform = PlatformUtilities.getPlatform();
        if (platform == null) {
            platform = Platform.UNKNOWN;
        }

        Map<Platform, List<String>> platformBackends = Map.of(
                Platform.WINDOWS, List.of("WASAPI", "DirectSound"),
                Platform.LINUX, List.of("ALSA", "PulseAudio"),
                Platform.MAC, List.of("CoreAudio"),
                Platform.OTHER_UNIX, List.of("ALSA", "PulseAudio", "CoreAudio", "JavaSound"),
                Platform.UNKNOWN, List.of("JavaSound")
        );
    
        for (Map.Entry<Platform, List<String>> entry : platformBackends.entrySet()) {
            if (entry.getKey().equals(platform)) {
                for (String backendName : entry.getValue()) {
                    try {
                        AudioBackendInfo backendInfo = fromName(backendName);
                        logger.info("Found compatible audio backend for this platform '{}': {}", System.getProperty("os.name"), backendName);
                        AudioBackend backend = getBackend(backendInfo);
                        if (!backend.isAvailableOnThisPlatform()) {
                            logger.info("Audio backend '{}' is not available on this platform '{}'.", backendName, System.getProperty("os.name"));
                            continue;
                        }
                        return backendInfo;
                    } catch (AudioBackendNotFoundException ex) {
                        logger.info("Audio backend '{}' is not available on this platform '{}'.", backendName, System.getProperty("os.name"));
                        continue;
                    }
                }
            }
        }
    
        // Fallback to JavaSound if no platform-specific backend is found.
        String fallbackBackendName = "JavaSound";
        AudioBackendInfo fallbackBackendInfo = fromName(fallbackBackendName);
        logger.warn("No compatible audio backends for this platform '{}'' founded. Using fallback: {}",
                System.getProperty("os.name"), fallbackBackendInfo.getName());
        return fallbackBackendInfo;
    }
    
    /**
     * Retrieves the default platform-specific audio backend.
     *
     * @return The platform-specific audio backend.
     */
    public static AudioBackendInfo getPlatformBackend() {
        return platformBackendInfo;
    }
}
