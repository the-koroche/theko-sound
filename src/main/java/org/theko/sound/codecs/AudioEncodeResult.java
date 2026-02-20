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

import java.util.Collections;
import java.util.List;

import org.theko.sound.AudioFormat;

/**
 * Represents the result of an audio encoding process, containing the encoded file data,
 * audio format, associated tags, and codec information.
 * 
 * @see AudioDecodeResult
 * @see AudioCodec
 * 
 * @since 0.1.3-beta
 * @author Theko
 */
public class AudioEncodeResult {

    private final byte[] encoded;
    private final AudioFormat format;
    private final List<AudioTag> tags;
    private final AudioCodecInfo codecInfo;

    /**
     * Constructs an AudioEncodeResult with the specified codec information, encoded data,
     * audio format, and tags.
     * @param codecInfo Information about the audio codec used for encoding.
     * @param encoded The encoded audio data as a byte array.
     * @param format The format of the encoded audio.
     * @param tags The list of audio tags associated with the encoded audio.
     */
    public AudioEncodeResult(AudioCodecInfo codecInfo, byte[] encoded, AudioFormat format, List<AudioTag> tags) {
        this.codecInfo = codecInfo;
        this.encoded = encoded;
        this.format = format;
        this.tags = tags;
    }

    /**
     * @return the encoded audio data as a byte array.
     */
    public byte[] getEncodedData() {
        return encoded;
    }

    /**
     * @return The audio format of the encoded audio data.
     */
    public AudioFormat getAudioFormat() {
        return format;
    }

    /**
     * @return an unmodifiable list of audio tags associated with the encoded audio.
     */
    public List<AudioTag> getTagsList() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * @return The list of audio tags as {@link AudioTags}.
     */
    public AudioTags getTags() {
        return new AudioTags(tags);
    }

    /**
     * @return Information about the audio codec used for encoding.
     */
    public AudioCodecInfo getCodecInfo() {
        return codecInfo;
    }

    @Override
    public String toString() {
        return "AudioEncodeResult{Encoder: " + codecInfo.getName() + ", " + format.toString() + ", Tags: " + tags.toString() + "}";
    }
}
