
package org.theko.sound.codec.formats;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFormat;
import org.theko.sound.SampleConverter;
import org.theko.sound.UnsupportedAudioEncodingException;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecType;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioEncodeResult;
import org.theko.sound.codec.AudioTag;

/**
 * The {@code WAVECodec} class provides functionality for encoding and decoding
 * audio data in the WAVE file format. It supports reading and writing WAVE files
 * with various audio formats, including PCM (signed/unsigned), IEEE Float, ALAW,
 * and ULAW. The class also handles metadata stored in the WAVE INFO chunk.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Decoding WAVE files to extract audio data, format, and metadata.</li>
 *   <li>Encoding audio data into WAVE format with optional metadata.</li>
 *   <li>Support for common audio encodings such as PCM, IEEE Float, ALAW, and ULAW.</li>
 *   <li>Parsing and mapping metadata tags from WAVE INFO chunks.</li>
 *   <li>Validation of WAVE file structure and error handling for unsupported formats.</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <ul>
 *   <li>To decode a WAVE file, use the {@link #innerDecode(InputStream)} method.</li>
 *   <li>To encode audio data into a WAVE file, use the {@link #innerEncode(byte[], AudioFormat, List)} method.</li>
 *   <li>To retrieve codec information, use the {@link #getInfo()} method.</li>
 * </ul>
 * 
 * <p>Example:</p>
 * <pre>
 * {@code
 * WAVECodec codec = new WAVECodec();
 * InputStream inputStream = new FileInputStream("example.wav");
 * AudioDecodeResult result = codec.decode(inputStream);
 * 
 * byte[] audioData = result.getAudioData();
 * AudioFormat format = result.getFormat();
 * List<AudioTag> tags = result.getTags();
 * 
 * // Process audio data, format, and tags...
 * }
 * </pre>
 * 
 * <p>Note:</p>
 * <ul>
 *   <li>The class assumes that the input WAVE file conforms to the RIFF specification.</li>
 *   <li>Unsupported audio formats or encodings will result in an {@link AudioCodecException}.</li>
 *   <li>Metadata tags are cleaned and normalized to ensure compatibility.</li>
 * </ul>
 * 
 * @see AudioCodec
 * @see AudioDecodeResult
 * @see AudioEncodeResult
 * @see AudioFormat
 * @see AudioTag
 * @see AudioCodecException
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
@AudioCodecType(name = "WAVE", extension = "wav", version = "1.2")
public class WAVECodec extends AudioCodec {

    private static final Logger logger = LoggerFactory.getLogger(WAVECodec.class);

    private static final byte[] RIFF_BYTES = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WAVE_BYTES = "WAVE".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FORMAT_BYTES = "fmt ".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] DATA_BYTES = "data".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LIST_BYTES = "LIST".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] INFO_BYTES = "INFO".getBytes(StandardCharsets.US_ASCII);

    private static final AudioCodecInfo CODEC_INFO = new AudioCodecInfo(WAVECodec.class);

    /**
     * Decodes a WAVE file from the given input stream.
     * 
     * @param is
     *            the input stream containing the WAVE file
     * @return the decoded audio data, format, and metadata
     * @throws AudioCodecException
     *             if the input stream is not a valid WAVE file
     */
    @Override
    public AudioDecodeResult innerDecode (InputStream is) throws AudioCodecException {
        try {
            logger.debug("Parsing WAVE...");
            DataInputStream dis = new DataInputStream(is);

            // Check RIFF header
            byte[] riffHeader = new byte[4];
            dis.readFully(riffHeader);
            if (!Arrays.equals(RIFF_BYTES, riffHeader)) {
                logger.error("Not a valid RIFF file.");
                throw new AudioCodecException("Not a valid RIFF file.");
            }

            // Skip file size (not used)
            readLittleEndianInt(dis);

            // Check WAVE header
            byte[] waveHeader = new byte[4];
            dis.readFully(waveHeader);
            if (!Arrays.equals(WAVE_BYTES, waveHeader)) {
                logger.error("Not a valid WAVE file.");
                throw new AudioCodecException("Not a valid WAVE file.");
            }
            logger.debug("WAVE signatures is valid.");

            AudioFormat format = null;
            byte[] audioData = null;
            List<AudioTag> tags = new ArrayList<>();

            // Read chunks
            while (true) {
                byte[] chunkIdBytes = new byte[4];
                int read = dis.read(chunkIdBytes);
                if (read == -1) break; // EOF

                int chunkSize = readLittleEndianInt(dis);

                if (Arrays.equals(FORMAT_BYTES, chunkIdBytes)) {
                    // Audio format
                    logger.debug("Reading audio format...");
                    byte[] fmtData = new byte[chunkSize];
                    dis.readFully(fmtData);
                    format = parseFormatChunk(fmtData);
                    skipPadding(dis, chunkSize);
                } else if (Arrays.equals(DATA_BYTES, chunkIdBytes)) {
                    // Audio data
                    logger.debug("Reading audio data...");
                    audioData = new byte[chunkSize];
                    dis.readFully(audioData);
                    skipPadding(dis, chunkSize);
                } else if (Arrays.equals(LIST_BYTES, chunkIdBytes)) {
                    // Metadata
                    logger.debug("Reading metadata...");
                    byte[] listData = new byte[chunkSize];
                    dis.readFully(listData);
                    parseListChunk(listData, tags);
                    skipPadding(dis, chunkSize);
                } else {
                    // Skip unknown chunks
                    logger.info("Skip chunk \"" + new String(chunkIdBytes, StandardCharsets.US_ASCII) + "\" with size " + chunkSize + " bytes.");
                    skipChunkData(dis, chunkSize);
                }
            }

            if (format == null) {
                logger.error("Invalid WAV file: missing 'fmt' chunk.");
                throw new AudioCodecException("Invalid WAV file: missing 'fmt' chunk.");
            }

            if (audioData == null) {
                logger.error("Invalid WAV file: missing 'data' chunk.");
                throw new AudioCodecException("Invalid WAV file: missing 'data' chunk.");
            }
            logger.debug("Parsing complete.");
            logger.debug("Audio format: " + format);
            logger.debug("Audio data size (bytes): " + audioData.length);
            logger.debug("Metadata: " + tags);

            float[][] pcm = SampleConverter.toSamples(audioData, format);
            return new AudioDecodeResult(CODEC_INFO, pcm, format, Collections.unmodifiableList(tags));
        } catch (IOException ex) {
            throw new AudioCodecException(ex);
        }
    }

    /**
     * Parses the 'fmt ' chunk of a WAVE file and returns the audio format.
     * 
     * @param fmtData
     *            the 'fmt ' chunk data
     * @return the audio format
     * @throws AudioCodecException
     *             if the audio format is not supported
     */
    protected static AudioFormat parseFormatChunk (byte[] fmtData) throws AudioCodecException {
        ByteBuffer bb = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN);
        int audioFormat = bb.getShort() & 0xffff;
        int channels = bb.getShort() & 0xffff;
        int sampleRate = bb.getInt();
        int byteRate = bb.getInt();
        int blockAlign = bb.getShort() & 0xffff;
        int bitsPerSample = bb.getShort() & 0xffff;
        AudioFormat.Encoding encoding;

        switch (audioFormat) {
            case 1: // PCM
                if (bitsPerSample == 8) {
                    encoding = AudioFormat.Encoding.PCM_UNSIGNED;
                } else {
                    encoding = AudioFormat.Encoding.PCM_SIGNED;
                }
                break;
            case 3: // IEEE Float
                encoding = AudioFormat.Encoding.PCM_FLOAT;
                break;
            case 7: // ULAW (8-bit)
                encoding = AudioFormat.Encoding.ULAW;
                break;
            case 6: // ALAW (8-bit)
                encoding = AudioFormat.Encoding.ALAW;
                break;
            default:
                logger.error("Not supported audio format.");
                throw new AudioCodecException(new UnsupportedAudioEncodingException("Not supported audio format."));
        }

        return new AudioFormat(
            sampleRate,
            bitsPerSample,
            channels,
            encoding,
            false, 
            blockAlign,
            byteRate
        );
    }

    /**
     * Maps a WAVE INFO chunk ID to a corresponding audio tag ID.
     * 
     * @param id
     *            the WAVE INFO chunk ID
     * @return the audio tag ID, or null if not supported
     */
    protected static String mapInfoIdToTag (String id) {
            switch (id) {
                case "INAM": case "TITL": return "Title";
                case "IART": return "Artist";
                case "IPRD": case "ALBM": return "Album";
                case "ICRD": return "Year";
                case "ITRK": return "Track";
                case "ICMT": return "Comment";
                case "IGNR": return "Genre";
                case "IENG": return "Engineer";
                case "ISRC": return "SRC";
                case "ICOP": return "Copyright";
                case "ISFT": return "Software";
                case "ITCH": return "Technician";
                default: return id.startsWith("I") ? id.substring(1) : null;
            }
        }
    
    /**
     * Parses a WAVE LIST chunk and adds the contained tags to the specified list.
     * 
     * @param listData
     *            the LIST chunk data
     * @param tags
     *            the list of tags to add to
     */
    protected static void parseListChunk (byte[] listData, List<AudioTag> tags) {
        ByteBuffer bb = ByteBuffer.wrap(listData).order(ByteOrder.LITTLE_ENDIAN);
        byte[] listType = new byte[4];
        bb.get(listType);
        if (!Arrays.equals(INFO_BYTES, listType)) return;
    
        while (bb.remaining() >= 8) {
            byte[] idBytes = new byte[4];
            bb.get(idBytes);
            String id = new String(idBytes, StandardCharsets.US_ASCII);
            int size = bb.getInt();
    
            if (size < 0 || size > bb.remaining()) break;
    
            byte[] valueBytes = new byte[size];
            bb.get(valueBytes);
    
            // Skip padding (1 byte, if size isn't odd)
            if (size % 2 != 0 && bb.remaining() > 0) {
                bb.get();
            }
    
            String value = cleanText(new String(valueBytes, StandardCharsets.UTF_8));
            String tag = mapInfoIdToTag(id);
            if (tag != null) {
                tags.add(new AudioTag(tag, value));
            } else {
                logger.info("Unknown tag: " + id + " = " + value);
            }
        }
    }

    /**
     * Cleans a string of text by removing:
     * - null characters
     * - control characters (except for tab, line feed, and carriage return)
     * - leading and trailing whitespace
     * - repeated whitespace
     * - non-ASCII characters
     * 
     * @param text
     *            the text to clean
     * @return the cleaned text
     */
    protected static String cleanText (String text) {
        text = text.replaceAll("\0", "")
                    .replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "")
                    .trim();
    
        text = text.replaceAll("\\s+", " "); 
        text = text.replaceAll("[^\\x20-\\x7E\\n\\r]", "");
    
        return text;
    }

    /**
     * Skips over a chunk of data in the input stream, also skipping any
     * required padding (1 byte if the chunk size is odd).
     * 
     * @param dis
     *            the input stream to read from
     * @param chunkSize
     *            the size of the chunk to skip
     * @throws IOException
     *             if there is an error reading from the stream
     */
    protected static void skipChunkData (DataInputStream dis, int chunkSize) throws IOException {
        long skipped = 0;
        while (skipped < chunkSize) {
            skipped += dis.skip(chunkSize - skipped);
        }
        skipPadding(dis, chunkSize);
    }

    /**
     * Skips over padding (1 byte) if the chunk size is odd.
     * 
     * @param dis
     *            the input stream to read from
     * @param chunkSize
     *            the size of the chunk to skip padding for
     * @throws IOException
     *             if there is an error reading from the stream
     */
    protected static void skipPadding (DataInputStream dis, int chunkSize) throws IOException {
        if (chunkSize % 2 != 0) {
            dis.skipBytes(1);
        }
    }

    /**
     * Reads a 4-byte little-endian integer from the given DataInputStream.
     * 
     * @param dis
     *            the input stream to read from
     * @return the value read from the stream
     * @throws IOException
     *             if there is an error reading from the stream
     */
    protected static int readLittleEndianInt (DataInputStream dis) throws IOException {
        byte[] bytes = new byte[4];
        dis.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Encodes audio data into the WAVE format.
     * 
     * @param data
     *            the audio data to encode
     * @param format
     *            the audio format specifying encoding parameters
     * @param tags
     *            a list of audio tags to include in the encoded output
     * @return an AudioEncodeResult containing the encoded WAVE data, format, and tags
     * @throws AudioCodecException
     *             if an error occurs during encoding, such as an I/O issue
     */
    @Override
    public AudioEncodeResult innerEncode (byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        try {
            // Check supported encodings
            int audioFormatCode = getAudioFormatCode(format.getEncoding());

            byte[] fmtChunkData = createFormatChunk(audioFormatCode, format);
            byte[] listChunkData = createListChunk(tags);

            // Build all chunks
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeRiffHeader(outputStream);
            writeChunk(outputStream, FORMAT_BYTES, fmtChunkData);
            writeChunk(outputStream, DATA_BYTES, data);
            if (listChunkData.length > 4) {
                writeChunk(outputStream, LIST_BYTES, listChunkData);
            }

            updateRiffSize(outputStream);

            return new AudioEncodeResult(CODEC_INFO, outputStream.toByteArray(), format, tags);
        } catch (IOException ex) {
            throw new AudioCodecException(ex);
        }
    }

    /**
     * Creates a format chunk for a WAVE file containing audio format information.
     *
     * @param audioFormatCode
     *            the audio format code (e.g. WAVE_FORMAT_PCM, WAVE_FORMAT_IEEE_FLOAT)
     * @param format
     *            the audio format containing encoding parameters
     * @return a byte array containing the format chunk data
     */
    protected static byte[] createFormatChunk (int audioFormatCode, AudioFormat format) {
        return ByteBuffer.allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) audioFormatCode)
                .putShort((short) format.getChannels())
                .putInt(format.getSampleRate())
                .putInt(format.getByteRate())
                .putShort((short) format.getFrameSize())
                .putShort((short) format.getBitsPerSample())
                .array();
    }

    protected static byte[] createListChunk (List<AudioTag> tags) throws IOException {
        ByteArrayOutputStream listChunkStream = new ByteArrayOutputStream();
        if (!tags.isEmpty()) {
            listChunkStream.write(INFO_BYTES);
            for (AudioTag tag : tags) {
                String tagKey = tag.getKey();
                String tagValue = cleanText(tag.getValue()) + "\0";
                String infoId = mapTagToInfoId(tagKey);
                if (infoId == null || infoId.length() != 4) continue;

                byte[] valueBytes = tagValue.getBytes(StandardCharsets.UTF_8);
                int size = valueBytes.length;

                ByteBuffer subChunk = ByteBuffer.allocate(8 + size + (size % 2))
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .put(infoId.getBytes(StandardCharsets.US_ASCII))
                        .putInt(size)
                        .put(valueBytes);
                if (size % 2 != 0) subChunk.put((byte) 0);
                listChunkStream.write(subChunk.array());
            }
        }
        return listChunkStream.toByteArray();
    }

    /**
     * Returns the audio format code for the given encoding, based on the WAVE file
     * format specification. The returned value is one of the following constants:
     * <ul>
     * <li>1 for PCM (signed/unsigned)</li>
     * <li>3 for IEEE Float</li>
     * <li>6 for ALAW (8-bit)</li>
     * <li>7 for ULAW (8-bit)</li>
     * </ul>
     * 
     * @param encoding
     *            the audio format encoding
     * @return the audio format code
     * @throws AudioCodecException
     *             if the encoding is not supported
     */
    protected static int getAudioFormatCode (AudioFormat.Encoding encoding) throws AudioCodecException {
        switch (encoding) {
            case PCM_UNSIGNED: case PCM_SIGNED: // PCM
                return 1;
            case PCM_FLOAT: // IEEE Float
                return 3;
            case ULAW: // ULAW (8-bit)
                return 7;
            case ALAW: // ALAW (8-bit)
                return 6;
            default:
                throw new AudioCodecException("Not supported encoding.");
        }
    }

    /**
     * Writes the RIFF header to the given output stream, with the size
     * initially set to 0. The size will be updated later by calling
     * {@link #updateRiffSize(ByteArrayOutputStream)}.
     * 
     * @param outputStream
     *            the output stream to write to
     * @throws IOException
     *             if an I/O error occurs
     */
    protected static void writeRiffHeader (ByteArrayOutputStream outputStream) throws IOException {
        outputStream.write(RIFF_BYTES);
        writeLittleEndianInt(outputStream, 0); // Placeholder for RIFF size
        outputStream.write(WAVE_BYTES);
    }

    /**
     * Updates the RIFF size in the RIFF header of the given output stream.
     * This method should be called after all the audio data has been written
     * to the output stream, as the RIFF size is calculated based on the
     * position of the output stream.
     * @param outputStream the output stream containing the RIFF header
     */
    protected static void updateRiffSize (ByteArrayOutputStream outputStream) {
        byte[] wavData = outputStream.toByteArray();
        int riffSize = wavData.length - 8;
        byte[] riffSizeBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(riffSize)
                .array();
        System.arraycopy(riffSizeBytes, 0, wavData, 4, 4);
    }

    /**
     * Writes a WAVE chunk to the given output stream, with the given chunk ID,
     * chunk data, and optional padding. The chunk size is written in little-
     * endian byte order. If the chunk data is of odd length, a padding byte of
     * 0 is appended to make the total length even.
     * 
     * @param outputStream
     *            the output stream to write to
     * @param chunkId
     *            the chunk ID (e.g. "fmt ", "data", etc.)
     * @param chunkData
     *            the chunk data
     * @throws IOException
     *             if an I/O error occurs
     */
    protected static void writeChunk (ByteArrayOutputStream outputStream, byte[] chunkId, byte[] chunkData) throws IOException {
        outputStream.write(chunkId);
        writeLittleEndianInt(outputStream, chunkData.length);
        outputStream.write(chunkData);
        if (chunkData.length % 2 != 0) {
            outputStream.write(0); // Padding
        }
    }

    /**
     * Writes a 4-byte integer to the given ByteArrayOutputStream in little-endian byte order.
     *
     * @param stream
     *            the output stream to write to
     * @param value
     *            the integer value to write
     * @throws IOException
     *             if an I/O error occurs while writing to the stream
     */
    protected static void writeLittleEndianInt (ByteArrayOutputStream stream, int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
        stream.write(bytes);
    }

    /**
     * Maps an audio tag to a corresponding WAVE INFO chunk ID.
     * 
     * @param tag
     *            the audio tag to map
     * @return the corresponding WAVE INFO chunk ID, or null if not supported
     */
    protected static String mapTagToInfoId (String tag) {
        switch (tag) {
            case "Title": return "INAM";
            case "Artist": return "IART";
            case "Album": return "IPRD";
            case "Year": return "ICRD";
            case "Track": return "ITRK";
            case "Comment": return "ICMT";
            case "Genre": return "IGNR";
            case "Engineer": return "IENG";
            case "SRC": return "ISRC";
            case "Copyright": return "ICOP";
            case "Software": return "ISFT";
            case "Technician": return "ITCH";
            default:
                if (tag.length() == 3) return "I" + tag;
                if (tag.length() == 4 && tag.startsWith("I")) return tag;
                return null;
        }
    }

    /**
     * Returns the audio codec information for this codec.
     * 
     * @return the audio codec information
     */
    @Override
    public AudioCodecInfo getInfo () {
        return CODEC_INFO;
    }
}