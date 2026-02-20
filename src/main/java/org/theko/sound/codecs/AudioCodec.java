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

import java.io.InputStream;
import java.util.List;

import org.theko.sound.AudioFormat;

/**
 * The {@code AudioCodec} class serves as an abstract base for audio codec implementations.
 * It provides methods for encoding and decoding audio data, as well as utility methods
 * to measure and log the elapsed time for these operations.
 *
 * <p>Subclasses must implement the {@link #decode(InputStream)} and {@link #encode(byte[], AudioFormat, List)}
 * methods to provide specific codec functionality. Additionally, subclasses must provide
 * codec-specific information via the {@link #getInfo()} method.
 *
 * <p>Example usage:
 * <pre>{@code
 * AudioCodec codec = new MyAudioCodec();
 * try (InputStream is = new FileInputStream("audiofile.wav")) {
 *     AudioDecodeResult result = codec.decode(is);
 *     // Process decoded audio data
 * } catch (AudioCodecException | IOException ex) {
 *     e.printStackTrace();
 * }
 * }</pre>
 *
 * @since 1.3.0
 * @author Theko
 */
public abstract class AudioCodec {

    /**
     * Calls the decode method.
     * 
     * @param is the input stream containing the audio data to decode
     * @return the decoded audio data, format, and metadata
     * @throws AudioCodecException if the input stream is not a valid audio file for this codec
     */
    public abstract AudioDecodeResult decode(InputStream is) throws AudioCodecException;

    /**
     * Calls the encode method.
     *
     * @param data the audio data to encode
     * @param format the format of the data
     * @param tags the tags to add to the encoded file
     * @return the encoded audio data
     * @throws AudioCodecException if there is an error encoding the data
     */
    protected abstract AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException;

    /**
     * Returns codec-specific information such as name, version, extension, etc.
     *
     * @return an instance of {@link AudioCodecInfo} containing codec details
     */
    public abstract AudioCodecInfo getInfo();
}