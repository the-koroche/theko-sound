package org.theko.sound.codec;

import java.util.Collections;
import java.util.List;

import org.theko.sound.AudioFormat;

public class AudioEncodeResult {
    private final byte[] fileData;
    private final AudioFormat format;
    private final List<AudioTag> tags;

    public AudioEncodeResult (byte[] fileData, AudioFormat format, List<AudioTag> tags) {
        this.fileData = fileData;
        this.format = format;
        this.tags = tags;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public AudioFormat getAudioFormat() {
        return format;
    }

    public List<AudioTag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    protected String getEncoderName() {
        return "UNKNOWN";
    }

    public String getInfo() {
        StringBuilder outString = new StringBuilder();
        String tab = "  ";
        outString.append("--- ").append(getEncoderName() + " CODEC").append(" ---\n");
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
        return "AudioEncodeResult {Encoder: " + getEncoderName() + ", " + format.toString() + ", Tags: " + tags.toString() + "}";
    }
}
