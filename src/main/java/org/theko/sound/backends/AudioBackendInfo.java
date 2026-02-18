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

package org.theko.sound.backends;

/**
 * Represents information about an audio backend, including its name, description, supported input/output capabilities,
 * and the class type of the audio backend. This class extracts metadata from 
 * the provided audio backend class, which must be annotated with {@link AudioBackendType}.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * AudioBackendInfo info = new AudioBackendInfo(MyAudioBackend.class);
 * System.out.println(info.getName());
 * System.out.println(info.getDescription());
 * System.out.println(info); // Prints: AudioBackendInfo{name, description, input/output capabilities}
 * }
 * </pre>
 * 
 * @since 1.0.0
 * @author Theko
 */
public class AudioBackendInfo {

    private final String name, description;
    private final boolean input, output;
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
            this.description = audioBackendType.description();
            this.input = audioBackendType.input();
            this.output = audioBackendType.output();
            this.audioBackend = audioBackend;
        } else {
            // Throw an exception if the annotation is not present
            throw new IllegalArgumentException("The provided audio backend class doesn't provide info about itself.");
        }
    }

    /**
     * @return the name of the audio backend, as specified in the {@link AudioBackendType} annotation.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description of the audio backend, as specified in the {@link AudioBackendType} annotation.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return true if the audio backend supports input, false otherwise.
     */
    public boolean supportsInput() {
        return input;
    }

    /**
     * @return true if the audio backend supports output, false otherwise.
     */
    public boolean supportsOutput() {
        return output;
    }

    /**
     * @return the class of the audio backend this info object represents.
     */
    public Class<? extends AudioBackend> getBackendClass() {
        return audioBackend;
    }

    @Override
    public String toString() {
        String io = (input && output) ? "Input/Output" : (input ? "Input" : (output ? "Output" : "None"));
        return "AudioBackendInfo{%s, %s, %s}".formatted(name, description, io);
    }
}
