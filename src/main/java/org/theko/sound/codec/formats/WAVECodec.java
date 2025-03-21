package org.theko.sound.codec.formats;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.theko.sound.AudioFormat;
import org.theko.sound.UnsupportedAudioEncodingException;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecType;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioEncodeResult;
import org.theko.sound.codec.AudioTag;


@AudioCodecType(name = "WAVE", extension = "wav", version = "1.0")
public class WAVECodec implements AudioCodec {

    // ======== DECODING ========
    @Override
    public AudioDecodeResult decode(InputStream is) throws AudioCodecException {
        try {
            DataInputStream dis = new DataInputStream(is);

            // Проверяем заголовок RIFF
            byte[] riffHeader = new byte[4];
            dis.readFully(riffHeader);
            if (!"RIFF".equals(new String(riffHeader, "US-ASCII"))) {
                throw new AudioCodecException("Not a valid RIFF file.");
            }

            // Пропускаем размер файла (не используется)
            readLittleEndianInt(dis);

            // Проверяем формат WAVE
            byte[] waveHeader = new byte[4];
            dis.readFully(waveHeader);
            if (!"WAVE".equals(new String(waveHeader, "US-ASCII"))) {
                throw new AudioCodecException("Not a valid WAVE file.");
            }

            AudioFormat format = null;
            byte[] audioData = null;
            List<AudioTag> tags = new ArrayList<>();

            // Читаем чанки
            while (true) {
                byte[] chunkIdBytes = new byte[4];
                int read = dis.read(chunkIdBytes);
                if (read == -1) break; // Конец файла

                String chunkId = new String(chunkIdBytes, "US-ASCII");
                int chunkSize = readLittleEndianInt(dis);

                if ("fmt ".equals(chunkId)) {
                    // Чанк с форматом аудио
                    byte[] fmtData = new byte[chunkSize];
                    dis.readFully(fmtData);
                    format = parseFormatChunk(fmtData);
                    skipPadding(dis, chunkSize);
                } else if ("data".equals(chunkId)) {
                    // Чанк с аудиоданными
                    audioData = new byte[chunkSize];
                    dis.readFully(audioData);
                    skipPadding(dis, chunkSize);
                } else if ("LIST".equals(chunkId)) {
                    // Чанк с метаданными
                    byte[] listData = new byte[chunkSize];
                    dis.readFully(listData);
                    parseListChunk(listData, tags);
                    skipPadding(dis, chunkSize);
                } else {
                    // Пропускаем неизвестные чанки
                    skipChunkData(dis, chunkSize);
                }
            }

            if (format == null) {
                throw new AudioCodecException("Invalid WAV file: missing 'fmt' chunk.");
            }

            if (audioData == null) {
                throw new AudioCodecException("Invalid WAV file: missing 'data' chunk.");
            }
            return new AudioDecodeResult(audioData, format, Collections.unmodifiableList(tags));
        } catch (IOException ex) {
            throw new AudioCodecException(ex);
        }
    }

    @SuppressWarnings("unused")
    protected static AudioFormat parseFormatChunk(byte[] fmtData) throws AudioCodecException {
        ByteBuffer bb = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN);
        int audioFormat = bb.getShort() & 0xffff; // формат аудио
        int channels = bb.getShort() & 0xffff; // количество каналов
        int sampleRate = bb.getInt(); // частота дискретизации
        int byteRate = bb.getInt(); // байтовая скорость
        int blockAlign = bb.getShort() & 0xffff; // выравнивание блока
        int bitsPerSample = bb.getShort() & 0xffff; // биты на сэмпл
    
        AudioFormat.Encoding encoding;
    
        // Определяем кодировку (encoding) на основе audioFormat и bitsPerSample
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
                throw new AudioCodecException(new UnsupportedAudioEncodingException("Не поддерживаемый формат аудио"));
        }
    
        // Возвращаем новый AudioFormat с указанными параметрами
        return new AudioFormat(
            sampleRate,
            bitsPerSample,
            channels,
            encoding,
            false
        );
    }

    protected static String mapInfoIdToTag(String id) {
            switch (id) {
                case "INAM": return "Title";
                case "IART": return "Artist";
                case "IPRD": return "Album";
                case "ICRD": return "Year"; // Для даты можно использовать "Year"
                case "ITRK": return "Track";
                case "ICMT": return "Comment";
                case "IGNR": return "Genre";
                case "IENG": return "Engineer";  // Добавлено
                case "ISRC": return "SRC";      // Добавлено
                case "ICOP": return "Copyright"; // Добавить поддержку авторских прав
                case "ISFT": return "Software"; // Информация о программе (например, FL Studio)
                case "ITCH": return "Technician"; 
                case "ALBM": return "Album";   // Добавление вариаций
                case "TITL": return "Title";   // Добавить титул (в случае вариации)
                default: return id.startsWith("I") ? id.substring(1) : null;
            }
        }
    
        protected static void parseListChunk(byte[] listData, List<AudioTag> tags) {
            ByteBuffer bb = ByteBuffer.wrap(listData).order(ByteOrder.LITTLE_ENDIAN);
            byte[] listType = new byte[4];
            bb.get(listType);
            if (!"INFO".equals(new String(listType, StandardCharsets.US_ASCII))) return;
        
            while (bb.remaining() >= 8) {
                byte[] idBytes = new byte[4];
                bb.get(idBytes);
                String id = new String(idBytes, StandardCharsets.US_ASCII);
                int size = bb.getInt();
        
                if (size < 0 || size > bb.remaining()) break;
        
                byte[] valueBytes = new byte[size];
                bb.get(valueBytes);
        
                // Пропустить паддинг (1 байт, если размер нечетный)
                if (size % 2 != 0 && bb.remaining() > 0) {
                    bb.get();
                }
        
                String value = cleanText(new String(valueBytes, StandardCharsets.UTF_8));
                            String tag = mapInfoIdToTag(id);
                        if (tag != null) {
                            tags.add(new AudioTag(tag, value));
                        } else {
                            System.out.println("Unknown tag: " + id + " = " + value);
                        }
                    }
                }
                
                protected static String cleanText(String text) {
        // Удаляем нулевые байты и непечатаемые символы
        text = text.replaceAll("\0", "")
                   .replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "")
                   .trim();
    
        // Удаляем лишние пробелы и нежелательные символы
        text = text.replaceAll("\\s+", " ")
                   .replaceAll("[^\\x20-\\x7E]", ""); // Удаляем не-ASCII символы*.
    
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
        return null;
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
            listChunkStream.write("INFO".getBytes(StandardCharsets.US_ASCII));
            for (AudioTag tag : tags) {
                String tagKey = tag.getKey();
                String tagValue = cleanText(tag.getValue());
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
                throw new AudioCodecException("Не поддерживаемый формат ");
        }
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
}