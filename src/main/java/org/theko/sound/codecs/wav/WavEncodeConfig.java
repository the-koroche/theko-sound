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

package org.theko.sound.codecs.wav;

import org.theko.sound.AudioFormat;
import org.theko.sound.codecs.AudioEncodeConfig;
import org.theko.sound.codecs.AudioTags;

/**
 * Represents an audio encoding configuration for the WAVE audio format.
 *
 * @author Theko
 * @since 0.3.0-beta
 */
public class WavEncodeConfig extends AudioEncodeConfig {
    private final WavAudioEncoding encoding;

    /**
     * Constructs a new WAVE audio encoding configuration.
     *
     * @param targetFormat the target audio format
     * @param metadata the audio metadata tags
     * @param encoding the WAVE audio encoding
     */
    public WavEncodeConfig(AudioFormat targetFormat, AudioTags metadata, WavAudioEncoding encoding) {
        super(targetFormat, metadata);
        this.encoding = encoding;
    }

    /**
     * @return the WAVE audio encoding associated with this configuration
     */
    public WavAudioEncoding getEncoding() {
        return encoding;
    }
}
