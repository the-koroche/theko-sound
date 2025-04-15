
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
import org.theko.sound.UnsupportedAudioEncodingException;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecType;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioEncodeResult;
import org.theko.sound.codec.AudioTag;

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

    @Override
    public AudioDecodeResult decode(InputStream is) throws AudioCodecException {
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
            logger.debug("Pasing complete.");
            logger.debug("Audio format: " + format);
            logger.debug("Audio data size: " + audioData.length);
            logger.debug("Metadata: " + tags);
            return new AudioDecodeResult(CODEC_INFO, audioData, format, Collections.unmodifiableList(tags));
        } catch (IOException ex) {
            throw new AudioCodecException(ex);
        }
    }

    protected static AudioFormat parseFormatChunk(byte[] fmtData) throws AudioCodecException {
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

    protected static String mapInfoIdToTag(String id) {
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
    
    protected static void parseListChunk(byte[] listData, List<AudioTag> tags) {
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

    protected static String cleanText(String text) {
        text = text.replaceAll("\0", "")
                    .replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "")
                    .trim();
    
        text = text.replaceAll("\\s+", " "); 
        text = text.replaceAll("[^\\x20-\\x7E\\n\\r]", "");
    
        return text;
    }

    protected static void skipChunkData(DataInputStream dis, int chunkSize) throws IOException {
        long skipped = 0;
        while (skipped < chunkSize) {
            skipped += dis.skip(chunkSize - skipped);
        }
        skipPadding(dis, chunkSize);
    }

    protected static void skipPadding(DataInputStream dis, int chunkSize) throws IOException {
        if (chunkSize % 2 != 0) {
            dis.skipBytes(1);
        }
    }

    protected static int readLittleEndianInt(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[4];
        dis.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    @Override
    public AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        try {
            // Check supported encodings
            int audioFormatCode = getAudioFormatCode(format.getEncoding());

            byte[] fmtChunkData = createFmtChunk(audioFormatCode, format);
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
        } catch (IOException e) {
            throw new AudioCodecException(e);
        }
    }

    protected static byte[] createFmtChunk(int audioFormatCode, AudioFormat format) {
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

    protected static byte[] createListChunk(List<AudioTag> tags) throws IOException {
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

    protected static int getAudioFormatCode(AudioFormat.Encoding encoding) throws AudioCodecException {
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

    protected static void writeRiffHeader(ByteArrayOutputStream outputStream) throws IOException {
        outputStream.write(RIFF_BYTES);
        writeLittleEndianInt(outputStream, 0); // Placeholder for RIFF size
        outputStream.write(WAVE_BYTES);
    }

    protected static void updateRiffSize(ByteArrayOutputStream outputStream) {
        byte[] wavData = outputStream.toByteArray();
        int riffSize = wavData.length - 8;
        byte[] riffSizeBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(riffSize)
                .array();
        System.arraycopy(riffSizeBytes, 0, wavData, 4, 4);
    }

    protected static void writeChunk(ByteArrayOutputStream outputStream, byte[] chunkId, byte[] chunkData) throws IOException {
        outputStream.write(chunkId);
        writeLittleEndianInt(outputStream, chunkData.length);
        outputStream.write(chunkData);
        if (chunkData.length % 2 != 0) {
            outputStream.write(0); // Padding
        }
    }

    protected static void writeLittleEndianInt(ByteArrayOutputStream stream, int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
        stream.write(bytes);
    }

    protected static String mapTagToInfoId(String tag) {
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

    @Override
    public AudioCodecInfo getInfo() {
        return CODEC_INFO;
    }
}