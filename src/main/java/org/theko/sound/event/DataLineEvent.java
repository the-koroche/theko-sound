package org.theko.sound.event;

import java.util.concurrent.TimeUnit;

import org.theko.sound.AudioData;
import org.theko.sound.DataLine;

public class DataLineEvent {
    private final DataLine line;
    private final long timeout;
    private final TimeUnit timeUnit;
    private final AudioData data;
    
    public DataLineEvent (DataLine line, AudioData data, long timeout, TimeUnit timeUnit) {
        this.line = line;
        this.data = data;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }
    
    public DataLineEvent (DataLine line, AudioData data) {
        this(line, data, -1, null);
    }

    public DataLine getDataLine() {
        return line;
    }

    public AudioData getAudioData() {
        return data;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
