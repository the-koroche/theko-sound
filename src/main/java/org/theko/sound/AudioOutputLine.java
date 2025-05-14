package org.theko.sound;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.direct.AudioOutputDevice;
import org.theko.sound.event.AudioLineEvent;
import org.theko.sound.event.AudioLineListener;
import org.theko.sound.event.AudioOutputLineListener;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;

/**
 * The {@code AudioOutputLine} class implements the {@link AudioLine} interface and provides
 * functionality for managing audio output lines. It interacts with an {@link AudioOutputDevice}
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
 *   <li>Manages audio output using an {@link AudioOutputDevice}.</li>
 *   <li>Supports listener notifications for various audio line events.</li>
 *   <li>Allows attaching and detaching an input {@link DataLine}.</li>
 *   <li>Provides methods for writing audio data and querying audio line state.</li>
 * </ul>
 * 
 * <p>Note: This class requires proper exception handling for audio device creation and
 * unsupported audio formats.
 * 
 * @see AudioLine
 * @see AudioOutputDevice
 * @see AudioLineListener
 * @see AudioOutputLineListener
 * @see DataLine
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioOutputLine implements AudioLine {
    private static final Logger logger = LoggerFactory.getLogger(AudioOutputLine.class);

    private final AudioOutputDevice aod;
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
            this.aod = AudioDevices.getOutputDevice(AudioDevices.getPlatformDevice());
            this.listeners = new ArrayList<>();
            logger.debug("AudioOutputLine created using " + aod.getClass().getSimpleName() + " device.");
        } catch (AudioDeviceCreationException | AudioDeviceNotFoundException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public AudioOutputLine (AudioOutputDevice aod) {
        this.aod = aod;
        this.listeners = new ArrayList<>();
        logger.debug("AudioOutputLine created using " + aod.getClass().getSimpleName() + " device.");
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
            aod.open(audioPort, audioFormat, bufferSize);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void open(AudioPort audioPort, AudioFormat audioFormat) {
        if (!isOpen()) {
            aod.open(audioPort, audioFormat);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void open(AudioFormat audioFormat) throws AudioDeviceException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        if (!isOpen()) {
            aod.open(aod.getDefaultPort(AudioFlow.OUT, audioFormat).get(), audioFormat);
            notifyListeners(NotifyAction.OPEN, new AudioLineEvent(this));
        }
    }

    @Override
    public void close() {
        if (isOpen()) isOpen = false;
        aod.close();
        if (inputLine != null) {
            inputLine.removeDataLineListener(onAvailableData);
            inputLine = null;
        }
        notifyListeners(NotifyAction.CLOSE, new AudioLineEvent(this));
    }

    @Override
    public void flush() {
        aod.flush();
        notifyListeners(NotifyAction.FLUSH, new AudioLineEvent(this));
    }

    @Override
    public void drain() {
        aod.drain();
        notifyListeners(NotifyAction.DRAIN, new AudioLineEvent(this));
    }

    @Override
    public void start() {
        aod.start();
        notifyListeners(NotifyAction.START, new AudioLineEvent(this));
    }

    @Override
    public void stop() {
        aod.stop();
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
        int writed = aod.write(data, offset, length);
        notifyListeners(NotifyAction.WRITE, new AudioLineEvent(this));
        return writed;
    }

    @Override
    public boolean isOpen() {
        return isOpen && aod.isOpen();
    }

    @Override
    public int available() {
        return aod.available();
    }

    @Override
    public int getBufferSize() {
        return aod.getBufferSize();
    }

    @Override
    public long getFramePosition() {
        return aod.getFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return aod.getMicrosecondPosition();
    }

    @Override
    public long getMicrosecondLatency() {
        return aod.getMicrosecondLatency();
    }

    @Override
    public AudioPort getCurrentAudioPort() {
        return aod.getCurrentAudioPort();
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

    public AudioOutputDevice getAudioOutputDevice() {
        return aod;
    }
}
