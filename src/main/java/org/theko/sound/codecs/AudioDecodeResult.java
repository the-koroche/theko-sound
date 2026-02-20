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
 * Represents the result of decoding an audio file, including the decoded audio data,
 * its format, associated tags, and codec information.
 * 
 * @see AudioCodec
 * 
 * @since 0.1.3-beta
 * @author Theko
 */
public class AudioDecodeResult {

    private final float[][] pcm;
    private final AudioFormat format;
    private final List<AudioTag> tags;
    private final AudioCodecInfo codecInfo;

    /**
     * Constructs an AudioDecodeResult with the specified codec information, decoded samples,
     * audio format, and tags.
     * @param codecInfo Information about the audio codec used for decoding.
     * @param pcm The decoded audio samples as a 2D float array (channels x samples).
     * @param format The format of the decoded audio.
     * @param tags The list of audio tags associated with the decoded audio.
     */
    public AudioDecodeResult(AudioCodecInfo codecInfo, float[][] pcm, AudioFormat format, List<AudioTag> tags) {
        this.codecInfo = codecInfo;
        this.pcm = pcm;
        this.format = format;
        this.tags = tags;
    }

    /**
     * Returns the decoded audio samples as a 2D float array ([channels][samples]).
     *
     * @return The decoded audio samples.
     */
    public float[][] getSamples() {
        return pcm;
    }

    /**
     * @return The audio format of the decoded audio data.
     */
    public AudioFormat getAudioFormat() {
        return format;
    }

    /**
     * @return The unmodifiable list of audio tags associated with the decoded audio.
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
     * @return Information about the audio codec used for decoding.
     */
    public AudioCodecInfo getCodecInfo() {
        return codecInfo;
    }

    @Override
    public String toString() {
        return "AudioDecodeResult{Decoder: " + codecInfo.getName() + ", " + format.toString() + ", Tags: " + tags.toString() + "}";
    }
}
