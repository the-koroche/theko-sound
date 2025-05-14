package org.theko.sound.event;

import java.util.concurrent.TimeUnit;

import org.theko.sound.AudioFormat;
import org.theko.sound.DataLine;

/**
 * Represents an event associated with a {@link DataLine}, containing audio data and metadata.
 * This class provides information about the audio format, the audio data, and an optional timeout.
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public class DataLineEvent {
    private final DataLine line;
    private final long timeout;
    private final TimeUnit timeUnit;
    private final AudioFormat audioFormat;
    private final float[][] data;
    
    public DataLineEvent (DataLine line, AudioFormat audioFormat, float[][] data, long timeout, TimeUnit timeUnit) {
        this.line = line;
        this.audioFormat = audioFormat;
        this.data = data;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }
    
    public DataLineEvent (DataLine line, AudioFormat audioFormat, float[][] data) {
        this(line, audioFormat, data, -1, null);
    }

    public DataLine getDataLine() {
        return line;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public float[][] getAudioData() {
        return data;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
