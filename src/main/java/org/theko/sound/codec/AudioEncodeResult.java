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
 * @since v1.3.1
 * @author Theko
 */
public class AudioEncodeResult {

    private final byte[] fileData;
    private final AudioFormat format;
    private final List<AudioTag> tags;
    private final AudioCodecInfo codecInfo;

    public AudioEncodeResult(AudioCodecInfo codecInfo, byte[] fileData, AudioFormat format, List<AudioTag> tags) {
        this.codecInfo = codecInfo;
        this.fileData = fileData;
        this.format = format;
        this.tags = tags;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public AudioFormat getAudioFormat() {
        return format;
    }

    public List<AudioTag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public String getInfo() {
        StringBuilder outString = new StringBuilder();
        String tab = "  ";
        outString.append("--- ").append(codecInfo.getName() + " CODEC").append(" ---\n");
        outString.append(tab).append("--- AUDIO FORMAT ---\n");
        outString.append(tab).append(format.toString()).append('\n');
        outString.append(tab).append("--- TAGS ---\n");
        for (AudioTag tag : tags) {
            outString.append(tab).append(tag.getKey() + ": " + tag.getValue()).append("\n");
        }
        return outString.toString();
    }

    @Override
    public String toString() {
        return "AudioEncodeResult {Encoder: " + codecInfo.getName() + ", " + format.toString() + ", Tags: " + tags.toString() + "}";
    }
}
