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

import org.theko.sound.AudioFormat;

/**
 * Represents an audio encoding configuration, which defines the target audio format and metadata to be associated with the encoded audio data.
 *
 * @see AudioFormat
 * @see AudioTags
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public class AudioEncodeConfig {
    private final AudioFormat targetFormat;
    private final AudioTags metadata;

    /**
     * Creates a new audio encoding configuration with the given target audio format and metadata.
     *
     * @param targetFormat the target audio format
     * @param metadata the audio metadata tags
     */
    public AudioEncodeConfig(AudioFormat targetFormat, AudioTags metadata) {
        this.targetFormat = targetFormat;
        this.metadata = metadata;
    }

    /**
     * Returns the target audio format that this configuration is meant to encode for.
     *
     * @return the target audio format
     */
    public AudioFormat getTargetFormat() {
        return targetFormat;
    }

    /**
     * Retrieves the audio metadata tags associated with this configuration.
     *
     * @return the audio metadata tags associated with this configuration
     */
    public AudioTags getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "AudioEncodeConfig{" +
                "targetFormat=" + targetFormat +
                ", metadata=" + metadata +
                '}';
    }
}
