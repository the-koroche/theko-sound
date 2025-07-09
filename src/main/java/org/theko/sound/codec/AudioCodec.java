package org.theko.sound.codec;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFormat;

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
 * @since v1.3.0
 * @author Theko
 */
public abstract class AudioCodec {

    private static final Logger logger = LoggerFactory.getLogger(AudioCodec.class);

    protected abstract AudioDecodeResult innerDecode (InputStream is) throws AudioCodecException;
    protected abstract AudioEncodeResult innerEncode (byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException;

    /**
     * Calls the decode method and logs the elapsed time.
     * 
     * @param is
     *            the input stream containing the audio data to decode
     * @return the decoded audio data, format, and metadata
     * @throws AudioCodecException
     *             if the input stream is not a valid audio file for this codec
     */
    public AudioDecodeResult decode (InputStream is) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioDecodeResult adr = innerDecode(is);
        long endNs = System.nanoTime();
        logger.debug("Elapsed decoding time: " + (endNs - startNs) + " ns.");
        return adr;
    }

    /**
     * Calls the encode method and logs the elapsed time.
     *
     * @param data the audio data to encode
     * @param format the format of the data
     * @param tags the tags to add to the encoded file
     *
     * @return the encoded audio data
     *
     * @throws AudioCodecException if there is an error encoding the data
     */
    public AudioEncodeResult encode (byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioEncodeResult aer = innerEncode(data, format, tags);
        long endNs = System.nanoTime();
        logger.debug("Elapsed encoding time: " + (endNs - startNs) + " ns.");
        return aer;
    }

    /**
     * Returns codec-specific information such as name, version, extension, etc.
     *
     * @return an instance of {@link AudioCodecInfo} containing codec details
     */
    public abstract AudioCodecInfo getInfo ();
}