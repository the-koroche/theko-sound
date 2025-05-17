package org.theko.sound;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.event.AudioLineEvent;
import org.theko.sound.event.AudioLineListener;
import org.theko.sound.event.AudioOutputLineListener;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;

/**
 * The {@code AudioOutputLine} class implements the {@link AudioLine} interface and provides
 * functionality for managing audio output lines. It interacts with an {@link AudioOutputBackend}
 * to handle audio playback and supports listener notifications for various audio line events.
 * 
 * <p>This class allows for opening, closing, flushing, draining, starting, and stopping audio
 * output lines. It also supports writing audio data and managing listeners for audio events.
 * 
 * <p>Listeners can be added or removed to receive notifications for events such as opening,
 * closing, flushing, draining, starting, stopping, and writing audio data. Additionally, this
 * class supports attaching an input {@link DataLine} for handling available data.
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * AudioOutputLine audioOutputLine = new AudioOutputLine();
 * audioOutputLine.open(audioFormat);
 * audioOutputLine.start();
 * audioOutputLine.write(audioData, 0, audioData.length);
 * audioOutputLine.stop();
 * audioOutputLine.close();
 * }
 * </pre>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Manages audio output using an {@link AudioOutputBackend}.</li>
 *   <li>Supports listener notifications for various audio line events.</li>
 *   <li>Allows attaching and detaching an input {@link DataLine}.</li>
 *   <li>Provides methods for writing audio data and querying audio line state.</li>
 * </ul>
 * 
 * <p>Note: This class requires proper exception handling for audio backend creation and
 * unsupported audio formats.
 * 
 * @see AudioLine
 * @see AudioOutputBackend
 * @see AudioLineListener
 * @see AudioOutputLineListener
 * @see DataLine
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
public class AudioOutputLine implements AudioLine {
    private static final Logger logger = LoggerFactory.getLogger(AudioOutputLine.class);

    private final AudioOutputBackend aob;
    private List<AudioLineListener> listeners;

    private DataLineAdapter onAvailableData = new DataLineAdapter() {
        @Override
        public void onSend(DataLineEvent e) {
            byte[] bytes = SampleConverter.fromSamples(e.getDataLine().forceReceive(), e.getAudioFormat());
            dwrite(bytes, 0, bytes.length);
        }
    };

    private enum NotifyAction {
        OPEN, CLOSE, FLUSH, DRAIN, START, STOP, WRITE
    }

    private boolean isOpen;
    private DataLine inputLine;

    public AudioOutputLine () {
        try {
            this.aob = AudioBackends.getOutputBackend(AudioBackends.getPlatformBackend());
            this.listeners = new ArrayList<>();
            logger.debug("AudioOutputLine created using " + aob.getClass().getSimpleName() + " backend.");
        } catch (AudioBackendCreationException | AudioBackendNotFoundException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public AudioOutputLine (AudioOutputBackend aob) {
        this.aob = aob;
        this.listeners = new ArrayList<>();
        logger.debug("AudioOutputLine created using " + aob.getClass().getSimpleName() + " backend.");
    }

    public void addAudioOutputLineListener(AudioOutputLineListener listener) {
        listeners.add(listener);
        refreshListeners();
    }

    public void removeAudioOutputLineListener(AudioOutputLineListener listener) {
        listeners.remove(listener);
        refreshListeners();
    }

    public void addAudioLineListener(AudioLineListener listener) {
        listeners.add(listener);
        refreshListeners();
    }

    public void removeAudioLineListener(AudioLineListener listener) {
        listeners.remove(listener);
        refreshListeners();
    }

    private void notifyListeners(NotifyAction type, AudioLineEvent e) {
        for (AudioLineListener listener : listeners) {
            if (listener == null) continue;
            switch (type) {
                case CLOSE -> listener.onClose(e);
                case DRAIN -> listener.onDrain(e);
                case FLUSH -> listener.onFlush(e);
                case OPEN -> listener.onOpen(e);
                case START -> listener.onStart(e);
                case STOP -> listener.onStop(e);
                case WRITE -> {
                    if (listener instanceof AudioOutputLineListener) {
                        ((AudioOutputLineListener) listener).onWrite(e);
                    }
                }
            }
        }
    }

    private void refreshListeners() {
        listeners.removeIf(listener -> listener == null);
    }

    @Override
    public void open(AudioPort audioPort, AudioFormat audioFormat, int bufferSize) {
        if (!isOpen()) {
            aob.open(audioPort, audioFormat, bufferSize);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void open(AudioPort audioPort, AudioFormat audioFormat) {
        if (!isOpen()) {
            aob.open(audioPort, audioFormat);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void open(AudioFormat audioFormat) throws AudioBackendException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        if (!isOpen()) {
            aob.open(aob.getDefaultPort(AudioFlow.OUT, audioFormat).get(), audioFormat);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void close() {
        if (isOpen()) isOpen = false;
        aob.close();
        if (inputLine != null) {
            inputLine.removeDataLineListener(onAvailableData);
            inputLine = null;
        }
        notifyListeners(NotifyAction.CLOSE, new AudioLineEvent(this));
    }

    @Override
    public void flush() {
        aob.flush();
        notifyListeners(NotifyAction.FLUSH, new AudioLineEvent(this));
    }

    @Override
    public void drain() {
        aob.drain();
        notifyListeners(NotifyAction.DRAIN, new AudioLineEvent(this));
    }

    @Override
    public void start() {
        aob.start();
        notifyListeners(NotifyAction.START, new AudioLineEvent(this));
    }

    @Override
    public void stop() {
        aob.stop();
        notifyListeners(NotifyAction.STOP, new AudioLineEvent(this));
    }

    public int write(byte[] data, int offset, int length) {
        if (inputLine == null) {
            return dwrite(data, offset, length);
        } else {
            logger.warn("Write operation is not available, when DataLine is attached!");
            return -1;
        }
    }

    private int dwrite(byte[] data, int offset, int length) {
        int writed = aob.write(data, offset, length);
        notifyListeners(NotifyAction.WRITE, new AudioLineEvent(this));
        return writed;
    }

    @Override
    public boolean isOpen() {
        return isOpen && aob.isOpen();
    }

    @Override
    public int available() {
        return aob.available();
    }

    @Override
    public int getBufferSize() {
        return aob.getBufferSize();
    }

    @Override
    public long getFramePosition() {
        return aob.getFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return aob.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() {
        return aob.getMicrosecondLatency();
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return aob.getCurrentAudioPort();
    }

    public void setInputLine(DataLine line) {
        inputLine = line;
        inputLine.addDataLineListener(onAvailableData);
    }

    public void removeInputLine() {
        inputLine.removeDataLineListener(onAvailableData);
        inputLine = null;
    }

    public DataLine getInputLine() {
        return inputLine;
    }

    public AudioOutputBackend getAudioOutputBackend() {
        return aob;
    }
}
