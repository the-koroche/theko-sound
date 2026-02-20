/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.backends;

import java.lang.reflect.Constructor;
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
import org.theko.sound.util.PlatformUtilities;
import org.theko.sound.util.PlatformUtilities.Platform;

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
public final class AudioBackends {

    private static final Logger logger = LoggerFactory.getLogger(AudioBackends.class);

    private static final Map<Platform, List<String>> PLATFORM_BACKENDS = Map.of(
            Platform.WINDOWS, List.of("DirectSound", "WASAPI"),
            Platform.LINUX, List.of("ALSA", "PulseAudio", "PipeWire", "JavaSound"),
            Platform.OTHER_UNIX, List.of("ALSA", "PulseAudio", "PipeWire", "JavaSound"),
            Platform.MAC, List.of("CoreAudio"),
            Platform.UNKNOWN, List.of("JavaSound")
    );

    private static AudioBackendInfo platformInputBackendInfo, platformOutputBackendInfo;

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
            detectPlatformBackends(false /* allow different backends for input and output */);
        } catch (AudioBackendNotFoundException | AudioBackendCreationException e) {
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
                return getBackend(backendInfo);
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
        
        try {
            Constructor<? extends AudioBackend> constructor = audioBackendInfo.getBackendClass().getDeclaredConstructor();
            if (!Modifier.isAbstract(audioBackendInfo.getBackendClass().getModifiers())
                && Modifier.isPublic(constructor.getModifiers())) {
                return constructor.newInstance();
            }
        } catch (ReflectiveOperationException | SecurityException ex) {
            logger.error("Failed to instantiate backend: {}.", audioBackendInfo.getBackendClass().getSimpleName(), ex);
            throw new AudioBackendCreationException("Failed to instantiate backend: " + audioBackendInfo.getBackendClass().getSimpleName(), ex);
        }

        throw new AudioBackendNotFoundException("No audio backends found for class: '" + audioBackendInfo.getBackendClass().getSimpleName() + "'.");
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
            if (backendInfo == null) {
                logger.debug("Encountered null audio backend info while searching for input backend of class: '{}'. Skipping.", audioBackendClass.getSimpleName());
                continue;
            }
            if (backendInfo.getBackendClass().equals(audioBackendClass)) {
                return getInputBackend(backendInfo);
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
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}, or if the backend does not support input.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioInputBackend getInputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException, AudioBackendNotFoundException {
        if (audioBackendInfo == null) {
            throw new IllegalArgumentException("Audio backend info cannot be null.");
        }

        if (!audioBackendInfo.supportsInput()) {
            logger.error("Audio backend '{}' does not support input.", audioBackendInfo.getBackendClass().getSimpleName());
            throw new AudioBackendNotFoundException("Audio backend '" + audioBackendInfo.getBackendClass().getSimpleName() + "' does not support input.");
        }

        try {
            Constructor<? extends AudioBackend> constructor = audioBackendInfo.getBackendClass().getDeclaredConstructor();
            if (!Modifier.isAbstract(audioBackendInfo.getBackendClass().getModifiers())
                && Modifier.isPublic(constructor.getModifiers())) {
                return constructor.newInstance().getInputBackend();
            }
        } catch (ReflectiveOperationException | SecurityException ex) {
            logger.error("Failed to instantiate input backend: {}.", audioBackendInfo.getBackendClass().getSimpleName(), ex);
            throw new AudioBackendCreationException("Failed to instantiate input backend: " + audioBackendInfo.getBackendClass().getSimpleName(), ex);
        }

        throw new AudioBackendNotFoundException("No input backend found for class: " + audioBackendInfo.getBackendClass().getSimpleName());
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
        if (audioBackendClass == null) {
            throw new IllegalArgumentException("Audio backend class cannot be null.");
        }
        for (AudioBackendInfo backendInfo : audioBackends) {
            if (backendInfo == null) {
                logger.debug("Encountered null audio backend info while searching for output backend of class: '{}'. Skipping.", audioBackendClass.getSimpleName());
                continue;
            }
            if (backendInfo.getBackendClass().equals(audioBackendClass)) {
                return getOutputBackend(backendInfo);
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
     * @throws AudioBackendNotFoundException If no backend is found with the given {@link AudioBackendInfo}, or if the backend does not support output.
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     */
    public static AudioOutputBackend getOutputBackend(AudioBackendInfo audioBackendInfo) throws AudioBackendCreationException, AudioBackendNotFoundException {
        if (audioBackendInfo == null) {
            throw new IllegalArgumentException("Audio backend info cannot be null.");
        }
        
        if (!audioBackendInfo.supportsOutput()) {
            logger.error("Audio backend '{}' does not support output.", audioBackendInfo.getBackendClass().getSimpleName());
            throw new AudioBackendNotFoundException("Audio backend '" + audioBackendInfo.getBackendClass().getSimpleName() + "' does not support output.");
        }

        try {
            Constructor<? extends AudioBackend> constructor = audioBackendInfo.getBackendClass().getDeclaredConstructor();
            if (!Modifier.isAbstract(audioBackendInfo.getBackendClass().getModifiers())
                && Modifier.isPublic(constructor.getModifiers())) {
                return constructor.newInstance().getOutputBackend();
            }
        } catch (ReflectiveOperationException | SecurityException ex) {
            logger.error("Failed to instantiate output backend: {}.", audioBackendInfo.getBackendClass().getSimpleName(), ex);
            throw new AudioBackendCreationException("Failed to instantiate output backend: " + audioBackendInfo.getBackendClass().getSimpleName(), ex);
        }

        throw new AudioBackendNotFoundException("No output backend found for class: " + audioBackendInfo.getBackendClass().getSimpleName());
    }

    /**
     * Returns all registered audio backends as an unmodifiable collection.
     *
     * @return A collection of all registered {@link AudioBackendInfo} instances.
     */
    public static Collection<AudioBackendInfo> getAllBackends() {
        return Collections.unmodifiableCollection(audioBackends);
    }

    
    /**
     * Detects and selects platform-specific audio backends that support the desired
     * features. This method is private and intended for internal use only.
     *
     * @param requireDuplex Whether the selected backend must support full duplex
     *                    audio operations (both input and output).
     *
     * @throws AudioBackendCreationException If the backend cannot be instantiated.
     * @throws AudioBackendNotFoundException If no backend is found that supports the
     *                              desired features.
     */
    private static void detectPlatformBackends(boolean requireDuplex) throws AudioBackendCreationException, AudioBackendNotFoundException {
        Platform platform = PlatformUtilities.getPlatform();
        if (platform == null) platform = Platform.UNKNOWN;

        List<String> backendNames = PLATFORM_BACKENDS.getOrDefault(platform, List.of("JavaSound"));

        for (String name : backendNames) {
            AudioBackendInfo info;
            try {
                info = fromName(name);
            } catch (AudioBackendNotFoundException e) {
                continue; // backend not found
            }

            if (requireDuplex && (!info.supportsInput() || !info.supportsOutput())) {
                continue;
            }

            boolean outputOk = false;
            if (info.supportsOutput()) {
                AudioOutputBackend out = getOutputBackend(info);
                outputOk = (out != null && out.isAvailableOnThisPlatform());
            }

            boolean inputOk = false;
            if (info.supportsInput()) {
                AudioInputBackend in = getInputBackend(info);
                inputOk = (in != null && in.isAvailableOnThisPlatform());
            }

            if (requireDuplex && (!outputOk || !inputOk)) {
                continue;
            }

            platformOutputBackendInfo = outputOk ? info : null;
            platformInputBackendInfo  = inputOk  ? info : null;

            logger.info("Selected backend '{}' (duplex={}, output={}, input={})",
                    name, requireDuplex, outputOk, inputOk);

            return;
        }

        // Fallback to JavaSound if some backend was not found
        AudioBackendInfo fallback = fromName("JavaSound");
        if (platformOutputBackendInfo == null) platformOutputBackendInfo = fallback;
        if (platformInputBackendInfo == null)  platformInputBackendInfo  = fallback;

        logger.warn("Fallback backend selected: JavaSound (duplex mode = {})", requireDuplex);
    }
    
    /**
     * Retrieves the default audio backend for output that is available on the current platform.
     * <p>
     * This is equivalent to {@link #getPlatformOutputBackend()} and is provided for convenience.
     * @return the default audio backend for output that is available on the current platform.
     */
    public static AudioBackendInfo getPlatformBackend() {
        return platformOutputBackendInfo;
    }

    /**
     * Retrieves the audio input backend that is available on the current platform.
     * <p>
     * This method is thread-safe and will return the same result every time it is called.
     * <p>
     * The returned backend is guaranteed to be available on the current platform and
     * support input functionality. If no audio backend was found, it falls back to the
     * JavaSound backend.
     * @return the audio input backend that is available on the current platform.
     */
    public static AudioBackendInfo getPlatformInputBackend() {
        return platformInputBackendInfo;
    }

    /**
     * Retrieves the audio output backend that is available on the current platform.
     * <p>
     * This method is thread-safe and will return the same result every time it is called.
     * <p>
     * The returned backend is guaranteed to be available on the current platform and
     * support output functionality. If no audio backend was found, it falls back to the
     * JavaSound backend.
     * @return the audio output backend that is available on the current platform.
     */
    public static AudioBackendInfo getPlatformOutputBackend() {
        return platformOutputBackendInfo;
    }
}
