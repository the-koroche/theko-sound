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

package org.theko.sound.codecs.ffmpeg;

import org.theko.sound.AudioFormat;
import org.theko.sound.codecs.AudioEncodeConfig;
import org.theko.sound.codecs.AudioTags;

/**
 * Represents a configuration for encoding audio data using the FFmpeg library.
 *
 * This class extends the base {@link AudioEncodeConfig} class with additional
 * settings specific to the FFmpeg library.
 *
 * @author Theko
 * @since 0.3.0-beta
 */
public class FFmpegEncodeConfig extends AudioEncodeConfig {

    private final String outFormat;
    private Integer bitrate;              // for mp3/aac/opus/wma
    private Integer flacCompressionLevel; // for flac (0–12)

    /**
     * Creates a new instance of FFmpegEncodeConfig.
     *
     * @param targetFormat the target audio format
     * @param metadata the audio metadata tags
     * @param outFormat the output format
     */
    public FFmpegEncodeConfig(AudioFormat targetFormat, AudioTags metadata, String outFormat) {
        super(targetFormat, metadata);
        this.outFormat = outFormat;
    }

    /**
     * Returns the output format for this configuration, which is used to determine the container format of the output file.
     *
     * @return the output format for this configuration
     */
    public String getOutFormat() {
        return outFormat;
    }

    /**
     * Sets the bitrate for the output file in kbps. This is applicable for the following formats:
     * MP3, AAC, Opus, WMA.
     *
     * @param bitrate the bitrate in kbps
     */
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * Sets the compression level for the FLAC encoder. This value is used to set the libFLAC compression level, which ranges from 0 (fastest) to 12 (best).
     *
     * @param level the compression level, between 0 and 12
     */
    public void setFlacCompressionLevel(Integer level) {
        this.flacCompressionLevel = level;
    }

    /**
     * Returns the bitrate in kbps for the output file, which is applicable for the following formats:
     * MP3, AAC, Opus, WMA.
     *
     * @return the bitrate in kbps, or null if not set
     */
    public Integer getBitrate() {
        return bitrate;
    }

    /**
     * Returns the compression level for the FLAC encoder. This value is used to set the libFLAC compression level, which ranges from 0 (fastest) to 12 (best).
     *
     * @return the compression level, between 0 and 12, or null if not set
     */
    public Integer getFlacCompressionLevel() {
        return flacCompressionLevel;
    }
}