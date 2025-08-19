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
import org.theko.sound.AudioClassScanner;

/**
 * The {@code AudioBackends} class provides a centralized management system for audio backends.
 * It allows for the registration, retrieval, and instantiation of audio input and output backends.
 * This class is designed to be thread-safe and ensures that all registered audio backends are
 * accessible throughout the application.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Automatic registration of audio backends annotated with {@link AudioBackendType}.</li>
 *   <li>Retrieval of audio backends by name or class type.</li>
 *   <li>Support for platform-specific default audio backends.</li>
 *   <li>Thread-safe storage and access to registered audio backends.</li>
 * </ul>
 *
 * <p>Usage:</p>
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
 * <p>Note: This class cannot be instantiated as it is designed to be a utility class.</p>
 *
 * @see AudioBackend
 * @see AudioBackendInfo
 * @see AudioBackendType
 * @see AudioInputBackend
 * @see AudioOutputBackend
 * 
 * @since v1.0.0
 * @author Theko
 */
public class AudioBackends {

    private static final Logger logger = LoggerFactory.getLogger(AudioBackends.class);

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
    private static void registerBackends() {
        // Attempt to scan all available packages for audio backends.
        audioBackends.clear();
        Set<Class<? extends AudioBackend>> allAudioBackends = AudioClassScanner.getBackendClasses();

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
    }

    /**
     * Retrieves an {@link AudioBackendInfo} by its name.
     *
     * @param name The name of the audio backend.
     * @return The corresponding {@link AudioBackendInfo}.
     * @throws AudioBackendNotFoundException If no backend is found with the given name.
     */
    public static AudioBackendInfo fromName(String name) throws AudioBackendNotFoundException {
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
     * @throws AudioBackendNotFoundException If no backend is found with the given class type.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioBackend getBackend(Class<? extends AudioBackend> audioBackendClass) throws AudioBackendNotFoundException, AudioBackendCreationException {
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
        logger.error("No audio backends found by class: '" + audioBackendClass.getSimpleName() + "'.");
        throw new AudioBackendNotFoundException("No audio backends found by class: '" + audioBackendClass.getSimpleName() + "'.");
    }

    /**
     * Retrieves an {@link AudioBackend} by its {@link AudioBackendInfo}.
     *
     * @param audioBackendInfo The {@link AudioBackendInfo} of the audio backend.
     * @return The corresponding {@link AudioBackend}.
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioBackend getBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendNotFoundException, AudioBackendCreationException {
        return getBackend(audioBackendInfo.getBackendClass());
    }

    /**
     * Retrieves an {@link AudioInputBackend} instance by its class type.
     *
     * @param audioBackendClass The class of the desired input backend.
     * @return The instantiated {@link AudioInputBackend}.
     * @throws AudioBackendNotFoundException If the backend is not registered.
     * @throws AudioBackendCreationException If an error occurs during instantiation.
     */
    public static AudioInputBackend getInputBackend(Class<? extends AudioBackend> audioBackendClass) throws AudioBackendNotFoundException, AudioBackendCreationException {
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
        logger.error("No input backend found for class: " + audioBackendClass.getSimpleName());
        throw new AudioBackendNotFoundException("No input backend found for class: " + audioBackendClass.getSimpleName());
    }

    public static AudioInputBackend getInputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException {
        try {
            return getInputBackend(audioBackendInfo.getBackendClass());
        } catch (AudioBackendNotFoundException ex) {
            logger.error("No input backend found for class: " + audioBackendInfo.getBackendClass().getSimpleName(), ex);
            return null;
        }
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

    public static AudioOutputBackend getOutputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException {
        try {
            return getOutputBackend(audioBackendInfo.getBackendClass());
        } catch (AudioBackendNotFoundException ex) {
            logger.error("No output backend found for class: " + audioBackendInfo.getBackendClass().getSimpleName(), ex);
            return null;
        }
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
     */
    public static AudioBackendInfo getPlatformBackend() throws AudioBackendNotFoundException {
        String name = System.getProperty("os.name").toLowerCase();
    
        Map<String, List<String>> platformBackends = Map.of(
            "win", List.of("WASAPI", "DirectSound"),
            "linux", List.of("ALSA", "PulseAudio"),
            "mac", List.of("CoreAudio")
        );
    
        for (Map.Entry<String, List<String>> entry : platformBackends.entrySet()) {
            if (name.contains(entry.getKey())) {
                for (String backend : entry.getValue()) {
                    try {
                        AudioBackendInfo backendInfo = fromName(backend);
                        logger.debug("Found platform backend: " + backend + ", Class: " + backendInfo.getBackendClass().getSimpleName());
                        return backendInfo;
                    } catch (AudioBackendNotFoundException ignored) { }
                }
            }
        }
    
        // Fallback to JavaSound if no platform-specific backend is found.
        String fallbackBackendName = "JavaSound";
        logger.info("No compatible audio backends for this platform founded. Using fallback: " + fallbackBackendName);
        try {
            return fromName(fallbackBackendName);
        } catch (AudioBackendNotFoundException ex) {
            // Already logged in 'fromName' method.
            // logger.error("No fallback backend found by name: '" + fallbackBackendName + "'.", ex);
            throw new AudioBackendNotFoundException("No fallback backend found by name: '" + fallbackBackendName + "'.", ex);
        }
    }
}
