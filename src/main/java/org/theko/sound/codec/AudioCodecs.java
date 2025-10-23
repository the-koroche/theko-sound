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

package org.theko.sound.codec;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioClassRegister;

/**
 * The {@code AudioCodecs} class is responsible for managing and providing access to audio codec information.
 * It maintains a collection of registered audio codecs and provides methods to retrieve codec information
 * by name or file extension, as well as to instantiate codec objects.
 * 
 * <p>This class uses reflection to discover and register all available audio codecs that are annotated
 * with {@code AudioCodecType}. It ensures thread-safe access to the collection of codecs.
 * 
 * <p>Key functionalities include:
 * <ul>
 *   <li>Registering all available audio codecs annotated with {@code AudioCodecType}.</li>
 *   <li>Retrieving codec information by name or file extension.</li>
 *   <li>Instantiating codec objects from their respective {@code AudioCodecInfo}.</li>
 *   <li>Providing access to all registered codecs.</li>
 * </ul>
 * 
 * <p>Usage of this class requires the presence of the following components:
 * <ul>
 *   <li>{@code AudioCodec}: The interface or base class for audio codecs.</li>
 *   <li>{@code AudioCodecType}: Annotation used to mark audio codec classes.</li>
 *   <li>{@code AudioCodecInfo}: A class encapsulating metadata about an audio codec.</li>
 *   <li>{@code AudioClassScanner}: A utility class for discovering available codecs.</li>
 *   <li>Custom exceptions such as {@code AudioCodecNotFoundException} and {@code AudioCodecCreationException}.</li>
 * </ul>
 * 
 * <p>This class is not meant to be instantiated and provides all its functionality through static methods.
 * 
 * <p><strong>Thread Safety:</strong> The collection of codecs is synchronized to ensure thread-safe access.
 * 
 * @since 1.3.1
 * @author Theko
 */
public final class AudioCodecs {

    private static final Logger logger = LoggerFactory.getLogger(AudioCodecs.class);

    // A collection to hold all registered audio codecs
    private static final Collection<AudioCodecInfo> audioCodecs = Collections.synchronizedSet(new LinkedHashSet<>());

    static {
        registerCodecs();
    }

    private AudioCodecs() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Register all audio codecs that are annotated with AudioCodecType
     */
    public static void registerCodecs() {
        logger.debug("Registering audio codecs...");
        // Use Reflections to find all classes implementing AudioCodec
        audioCodecs.clear();
        Set<Class<? extends AudioCodec>> allAudioCodecs = AudioClassRegister.getCodecClasses();

        for (Class<? extends AudioCodec> audioCodecClass : allAudioCodecs) {
            if (audioCodecClass.isAnnotationPresent(AudioCodecType.class)) {
                AudioCodecInfo codecInfo = new AudioCodecInfo(audioCodecClass);
                audioCodecs.add(codecInfo);
                logger.info("Found audio codec: " + codecInfo);
            } else {
                logger.info("Found audio codec without information: " + audioCodecClass.getSimpleName());
            }
        }
    }

    /**
     * Get an AudioCodecInfo by its name
     */
    public static AudioCodecInfo fromName(String name) throws AudioCodecNotFoundException {
        for (AudioCodecInfo audioCodec : audioCodecs) {
            if (audioCodec.getName().equalsIgnoreCase(name)) {
                return audioCodec;
            }
        }
        logger.error("No audio codecs found by name: '" + name + "'.");
        throw new AudioCodecNotFoundException("No audio codecs found by name: '" + name + "'.");
    }

    /**
     * Get an AudioCodecInfo by its exception
     * @param name File extension without a dot or an asterisk.
     */
    public static AudioCodecInfo fromExtension(String extension) throws AudioCodecNotFoundException {
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        for (AudioCodecInfo audioCodec : audioCodecs) {
            String[] codecExtensions = audioCodec.getExtensions();
            for (String codecExtension : codecExtensions) {
                if (codecExtension.equalsIgnoreCase(extension)) {
                    return audioCodec;
                }
            }
        }
        logger.error("No audio codecs found by extension: '" + extension + "'.");
        throw new AudioCodecNotFoundException("No audio codecs found by extension: '" + extension + "'.");
    }

    public static AudioCodec getCodec(AudioCodecInfo codecInfo) throws AudioCodecCreationException {
        try {
            return codecInfo.getCodecClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException ex) {
            logger.error(ex.getMessage());
            throw new AudioCodecCreationException(ex);
        }
    }

    /**
     * Get all registered audio codecs
     */
    public static Collection<AudioCodecInfo> getAllCodecs() {
        return audioCodecs;
    }
}
