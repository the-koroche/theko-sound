package org.theko.sound.codec;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.theko.sound.AudioData;
import org.theko.sound.AudioFormat;

public class AudioDecodeResult {
    private final byte[] data;
    private final AudioFormat format;
    private final List<AudioTag> tags;

    public AudioDecodeResult (byte[] data, AudioFormat format, List<AudioTag> tags) {
        this.data = data;
        this.format = format;
        this.tags = tags;
    }

    public AudioData getAudioData() throws IOException {
        return new AudioData(data, format);
    }

    public byte[] getBytes() {
        return data;
    }

    public AudioFormat getAudioFormat() {
        return format;
    }

    public List<AudioTag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    protected String getDecoderName() {
        return "UNKNOWN";
    }

    public String getInfo() {
        StringBuilder outString = new StringBuilder();
        String tab = "  ";
        outString.append("--- ").append(getDecoderName() + " CODEC").append(" ---\n");
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
        return "AudioDecodeResult {Decoder: " + getDecoderName() + ", " + format.toString() + ", Tags: " + tags.toString() + "}";
    }
}
