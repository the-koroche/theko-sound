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

package org.theko.sound.codecs;

/**
 * Represents metadata information about an audio codec. This class extracts
 * codec details such as name, file extensions, and version from the provided
 * codec class, which must be annotated with {@link AudioCodecType}.
 * 
 * If the provided codec class is not annotated with {@link AudioCodecType},
 * an {@link IllegalArgumentException} is thrown.
 * 
 * @since 0.1.3-beta
 * @author Theko
 */
public class AudioCodecInfo {

    private final String name;
    private final String[] extensions;
    private final String version;
    private final Class<? extends AudioCodec> codecClass;

    /**
     * Creates an instance of {@link AudioCodecInfo} based on the provided codec class.
     * 
     * @param codecClass The codec class to extract information from.
     * @throws IllegalArgumentException If the provided codec class is not annotated with {@link AudioCodecType}.
     */
    public AudioCodecInfo(Class<? extends AudioCodec> codecClass) throws IllegalArgumentException {
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

    /**
     * @return The name of the audio codec.
     */
    public String getName() {
        return name;
    }

    /**
     * @return An array of file extensions associated with this audio codec.
     */
    public String[] getExtensions() {
        return extensions;
    }

    /**
     * Returns the primary file extension for this audio codec.
     * @return The primary file extension.
     */
    public String getExtension() {
        return extensions[0];
    }

    /**
     * @return The version of the audio codec.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return The class of the audio codec that this info is associated with.
     */
    public Class<? extends AudioCodec> getCodecClass() {
        return codecClass;
    }

    @Override
    public String toString() {
        return "AudioCodecInfo{Class: " + codecClass.getSimpleName() + ", Name: " + name + ", Extensions: [" + String.join(", ", extensions) + "], Version: " + version + "}";
    }
}
