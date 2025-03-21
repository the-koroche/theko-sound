package org.theko.sound.codec.formats;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.theko.sound.*;
import org.theko.sound.codec.*;

@AudioCodecType(name = "ZWAV", extension = "zwv", version = "1.0")
public class ZWAVCodec implements AudioCodec {

    @Override
    public AudioDecodeResult decode(InputStream is) throws AudioCodecException {
        try {
            DataInputStream dis = new DataInputStream(is);

            byte[] riffHeader = new byte[4];
            dis.readFully(riffHeader);
            if (!"RIFF".equals(new String(riffHeader, "US-ASCII"))) {
                throw new AudioCodecException("Not a valid RIFF file.");
            }

            WAVECodec.readLittleEndianInt(dis);

            byte[] waveHeader = new byte[4];
            dis.readFully(waveHeader);
            if (!"ZWAV".equals(new String(waveHeader, "US-ASCII"))) {
                throw new AudioCodecException("Not a valid ZWAV file.");
            }

            AudioFormat format = null;
            byte[] compressedData = null;
            List<AudioTag> tags = new ArrayList<>();

            while (true) {
                byte[] chunkIdBytes = new byte[4];
                int read = dis.read(chunkIdBytes);
                if (read == -1) break;

                String chunkId = new String(chunkIdBytes, "US-ASCII");
                int chunkSize = WAVECodec.readLittleEndianInt(dis);

                if ("FRMT".equals(chunkId)) {
                    byte[] fmtData = new byte[chunkSize];
                    dis.readFully(fmtData);
                    format = WAVECodec.parseFormatChunk(fmtData);
                    WAVECodec.skipPadding(dis, chunkSize);
                } else if ("COMP".equals(chunkId)) {
                    compressedData = new byte[chunkSize];
                    dis.readFully(compressedData);
                    WAVECodec.skipPadding(dis, chunkSize);
                } else if ("MTDT".equals(chunkId)) {
                    byte[] listData = new byte[chunkSize];
                    dis.readFully(listData);
                    WAVECodec.parseListChunk(listData, tags);
                    WAVECodec.skipPadding(dis, chunkSize);
                } else {
                    WAVECodec.skipChunkData(dis, chunkSize);
                }
            }

            if (format == null) {
                throw new AudioCodecException("Missing 'FRMT' chunk.");
            }

            if (compressedData == null) {
                throw new AudioCodecException("Missing 'COMP' chunk.");
            }

            byte[] audioData = decompressData(compressedData);

            return new AudioDecodeResult(audioData, format, Collections.unmodifiableList(tags));

        } catch (IOException ex) {
            throw new AudioCodecException("Error while decoding ZWAV", ex);
        }
    }

    @Override
    public AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(data.length + 36));
            dos.writeBytes("ZWAV");

            dos.writeBytes("FRMT");
            byte[] formatData = WAVECodec.createFmtChunk(WAVECodec.getAudioFormatCode(format.getEncoding()), format);
            dos.writeInt(Integer.reverseBytes(formatData.length));
            dos.write(formatData);

            dos.writeBytes("COMP");
            byte[] compressedData = compressData(data);
            dos.writeInt(Integer.reverseBytes(compressedData.length));
            dos.write(compressedData);

            if (tags != null && !tags.isEmpty()) {
                byte[] tagData = WAVECodec.createListChunk(tags);
                dos.writeBytes("MTDT");
                dos.writeInt(Integer.reverseBytes(tagData.length));
                dos.write(tagData);
            }

            dos.flush();
            return new AudioEncodeResult(baos.toByteArray(), format, tags);

        } catch (IOException ex) {
            throw new AudioCodecException("Error while encoding ZWAV", ex);
        }
    }

    private byte[] decompressData(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             InflaterInputStream inflater = new InflaterInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = inflater.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    private byte[] compressData(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(baos)) {

            deflater.write(data);
            deflater.close();

            return baos.toByteArray();
        }
    }
}
