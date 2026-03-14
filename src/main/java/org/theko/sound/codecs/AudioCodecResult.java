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
 * Represents the result of an audio encoding/decoding process, containing the
 * audio format, associated tags, and codec information.
 *
 * @see AudioCodec
 * @see AudioDecodeResult
 * @see AudioEncodeResult
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public class AudioCodecResult {
    private final AudioFormat audioFormat;
    private final AudioTags metadata;
    private final AudioCodecInfo codecInfo;

    /**
     * Creates an instance of {@link AudioCodecResult}.
     *
     * @param codecInfo the codec information associated with this result
     * @param audioFormat the audio format associated with this result
     * @param metadata the metadata tags associated with this result
     */
    public AudioCodecResult(AudioCodecInfo codecInfo, AudioFormat audioFormat, AudioTags metadata) {
        this.codecInfo = codecInfo;
        this.audioFormat = audioFormat;
        this.metadata = metadata;
    }

    /**
     * @return the audio format associated with this result
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * @return the metadata tags associated with this result
     */
    public AudioTags getMetadata() {
        return metadata;
    }

    /**
     * Returns the codec information associated with this result.
     *
     * @return an instance of {@link AudioCodecInfo} containing the codec's information
     */
    public AudioCodecInfo getCodecInfo() {
        return codecInfo;
    }

    @Override
    public String toString() {
        return "AudioCodecResult{" +
                "codecInfo=" + codecInfo +
                "audioFormat=" + audioFormat +
                ", metadata=" + metadata +
                '}';
    }
}
