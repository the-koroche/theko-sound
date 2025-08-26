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

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFormat;
import org.theko.sound.utility.FormatUtilities;

/**
 * The {@code AudioCodec} class serves as an abstract base for audio codec implementations.
 * It provides methods for encoding and decoding audio data, as well as utility methods
 * to measure and log the elapsed time for these operations.
 *
 * <p>Subclasses must implement the {@link #innerDecode(InputStream)} and {@link #innerEncode(byte[], AudioFormat, List)}
 * methods to provide specific codec functionality. Additionally, subclasses must provide
 * codec-specific information via the {@link #getInfo()} method.</p>
 *
 * <p>The {@code callDecode} and {@code callEncode} methods wrap the abstract methods
 * with timing and logging functionality to measure performance.</p>
 *
 * <p>Example usage:</p>
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

    private static final Logger logger = LoggerFactory.getLogger(AudioCodec.class);

    protected abstract AudioDecodeResult innerDecode(InputStream is) throws AudioCodecException;
    protected abstract AudioEncodeResult innerEncode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException;

    /**
     * Calls the decode method and logs the elapsed time.
     * 
     * @param is the input stream containing the audio data to decode
     * @return the decoded audio data, format, and metadata
     * @throws AudioCodecException if the input stream is not a valid audio file for this codec
     */
    public AudioDecodeResult decode(InputStream is) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioDecodeResult adr = innerDecode(is);
        long endNs = System.nanoTime();
        long elapsedNs = endNs - startNs;
        String elapsedFormatted = FormatUtilities.formatTime(elapsedNs, 3);
        logger.debug("Elapsed decoding time: {}", elapsedFormatted);
        return adr;
    }

    /**
     * Calls the encode method and logs the elapsed time.
     *
     * @param data the audio data to encode
     * @param format the format of the data
     * @param tags the tags to add to the encoded file
     * @return the encoded audio data
     * @throws AudioCodecException if there is an error encoding the data
     */
    public AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioEncodeResult aer = innerEncode(data, format, tags);
        long endNs = System.nanoTime();
        long elapsedNs = endNs - startNs;
        String elapsedFormatted = FormatUtilities.formatTime(elapsedNs, 3);
        logger.debug("Elapsed encoding time: {}", elapsedFormatted);
        return aer;
    }

    /**
     * Returns codec-specific information such as name, version, extension, etc.
     *
     * @return an instance of {@link AudioCodecInfo} containing codec details
     */
    public abstract AudioCodecInfo getInfo();
}