package org.theko.sound.codec.formats;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.theko.sound.AudioFormat;
import org.theko.sound.UnsupportedAudioEncodingException;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioDecodeResult;

@Deprecated
public class WAVECodec_DEPRECATED implements AudioCodec {

    // ======== DECODING ========
    @Override
    public WAVECodecResult decode(InputStream is) throws AudioCodecException {
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
            Map<String, String> tags = new HashMap<>();

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
            return new WAVECodecResult(audioData, format, Collections.unmodifiableMap(tags));
        } catch (IOException ex) {
            throw new AudioCodecException(ex);
        }
    }

    @SuppressWarnings("unused")
    private AudioFormat parseFormatChunk(byte[] fmtData) throws AudioCodecException {
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

    private String mapInfoIdToTag(String id) {
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

    private void parseListChunk(byte[] listData, Map<String, String> tags) {
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
                tags.put(tag, value);
            } else {
                System.out.println("Unknown tag: " + id + " = " + value);
            }
        }
    }
    
    private String cleanText(String text) {
        // Удаляем нулевые байты и непечатаемые символы
        text = text.replaceAll("\0", "")
                   .replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "")
                   .trim();
    
        // Удаляем лишние пробелы и нежелательные символы
        text = text.replaceAll("\\s+", " ")
                   .replaceAll("[^\\x20-\\x7E]", ""); // Удаляем не-ASCII символы*.
    
        return text;
    }

    private void skipChunkData(DataInputStream dis, int chunkSize) throws IOException {
        long skipped = 0;
        while (skipped < chunkSize) {
            skipped += dis.skip(chunkSize - skipped);
        }
        skipPadding(dis, chunkSize);
    }

    private void skipPadding(DataInputStream dis, int chunkSize) throws IOException {
        if (chunkSize % 2 != 0) {
            dis.skipBytes(1);
        }
    }

    private static int readLittleEndianInt(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[4];
        dis.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public byte[] encode(byte[] data, AudioFormat format, Map<String, String> tags) throws AudioCodecException {
        return null;
    }

    
    // ======== ENCODING ========

    
/*
	@Override
    public byte[] encode(byte[] data, AudioFormat format, Map<String, String> tags) throws AudioCodecException {
        try {
            // Проверка поддерживаемых форматов
            int audioFormatCode = getAudioFormatCode(format.getEncoding());

            int channels = format.getChannels();
            int sampleRate = (int) format.getSampleRate();
            int bitsPerSample = format.getSampleSizeInBits();
            int blockAlign = channels * (bitsPerSample / 8);
            int byteRate = sampleRate * blockAlign;

            // Формирование fmt чанка
            byte[] fmtChunkData = createFmtChunk(audioFormatCode, channels, sampleRate, byteRate, blockAlign, bitsPerSample);

            // Подготовка LIST чанка с тегами
            byte[] listChunkData = createListChunk(tags);

            // Сборка всех чанков
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeRiffHeader(outputStream);
            writeChunk(outputStream, "fmt ", fmtChunkData);
            writeChunk(outputStream, "data", data);
            if (listChunkData.length > 4) {
                writeChunk(outputStream, "LIST", listChunkData);
            }

            // Обновление размера RIFF чанка
            updateRiffSize(outputStream);

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new AudioCodecException(e);
        }
    }

    private int getAudioFormatCode(AudioFormat.Encoding encoding) throws AudioCodecException {
        if (encoding == AudioFormat.Encoding.PCM_SIGNED || encoding == AudioFormat.Encoding.PCM_UNSIGNED) {
            return 1;
        } else if (encoding == AudioFormat.Encoding.PCM_FLOAT) {
            return 3;
        }
        throw new AudioCodecException("Unsupported encoding: " + encoding);
    }

    private byte[] createFmtChunk(int audioFormatCode, int channels, int sampleRate, int byteRate, int blockAlign, int bitsPerSample) {
        return ByteBuffer.allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) audioFormatCode)
                .putShort((short) channels)
                .putInt(sampleRate)
                .putInt(byteRate)
                .putShort((short) blockAlign)
                .putShort((short) bitsPerSample)
                .array();
    }

    private byte[] createListChunk(Map<String, String> tags) throws IOException {
        ByteArrayOutputStream listChunkStream = new ByteArrayOutputStream();
        if (!tags.isEmpty()) {
            listChunkStream.write("INFO".getBytes(StandardCharsets.US_ASCII));
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                String tagKey = entry.getKey();
                String tagValue = cleanText(entry.getValue());
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

    private void writeRiffHeader(ByteArrayOutputStream outputStream) throws IOException {
        outputStream.write("RIFF".getBytes(StandardCharsets.US_ASCII));
        writeLittleEndianInt(outputStream, 0); // Placeholder for RIFF size
        outputStream.write("WAVE".getBytes(StandardCharsets.US_ASCII));
    }

    private void updateRiffSize(ByteArrayOutputStream outputStream) {
        byte[] wavData = outputStream.toByteArray();
        int riffSize = wavData.length - 8;
        byte[] riffSizeBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(riffSize)
                .array();
        System.arraycopy(riffSizeBytes, 0, wavData, 4, 4);
    }

    private String mapTagToInfoId(String tag) {
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

    private void writeChunk(ByteArrayOutputStream outputStream, String chunkId, byte[] chunkData) throws IOException {
        outputStream.write(chunkId.getBytes(StandardCharsets.US_ASCII));
        writeLittleEndianInt(outputStream, chunkData.length);
        outputStream.write(chunkData);
        if (chunkData.length % 2 != 0) {
            outputStream.write(0); // Паддинг
        }
    }

    private void writeLittleEndianInt(ByteArrayOutputStream stream, int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(value)
                .array();
        stream.write(bytes);
    }*/

    public static class WAVECodecResult extends AudioDecodeResult {
        public WAVECodecResult(byte[] data, AudioFormat format, Map<String, String> tags) {
            super(data, format, tags);
        }
        
        @Override
        protected String getDecoderName() {
            return "WAV CODEC";
        }
    }
}