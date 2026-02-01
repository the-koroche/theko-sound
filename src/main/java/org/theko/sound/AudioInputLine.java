package org.theko.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.utility.ArrayUtilities;

/**
 * Represents an audio input line in the audio system.
 * It provides methods for opening, closing, and rendering audio data.
 * 
 * @since v2.3.1
 * @author Theko
 */
public class AudioInputLine implements AudioNode {

    private static final Logger logger = LoggerFactory.getLogger(AudioInputLine.class);

    private final AudioInputBackend aib;
    private AudioFormat audioFormat;
    private int bufferSize;

    public AudioInputLine(AudioInputBackend aib) {
        if (aib == null) throw new IllegalArgumentException("Audio input backend cannot be null.");
        this.aib = aib;
        logger.debug("Created audio input line with backend: {}", aib.getClass().getSimpleName());
    }

    public AudioInputLine () throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(AudioBackends.getInputBackend(AudioBackends.getPlatformBackend()));
    }

    /**
     * Opens the audio input line with the specified port, format, and buffer size.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The buffer size as an {@link AudioMeasure}.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    public void open(AudioPort port, AudioFormat audioFormat, AudioMeasure bufferSize) throws AudioBackendException {
        try {
            int bufferSizInFrames = (int) bufferSize.onFormat(audioFormat).getFrames();
            AudioPort targetPort = (port == null ? aib.getPort(AudioFlow.IN, audioFormat).get() : port);
            aib.open(targetPort, audioFormat, (int)bufferSize.getBytes());
            this.audioFormat = audioFormat;
            this.bufferSize = bufferSizInFrames;
            logger.debug("Opened audio input line with {} port, {} format, and {} buffer size",
                    targetPort, audioFormat, bufferSize);
        } catch (AudioBackendException | AudioPortsNotFoundException ex) {
            throw new AudioBackendException("Failed to open audio input line.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            throw new AudioBackendException("Unsupported audio format.", ex);
        }
    }

    /** 
     * Opens the audio input line with the specified port and format.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    public void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        this.open(port, audioFormat, AudioMeasure.ofFrames(2048));
    }

    /**
     * Opens the audio input line with the specified format.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are available.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     */
    public void open (AudioFormat audioFormat) throws AudioBackendException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        this.open(null, audioFormat);
    }

    /**
     * Checks if the audio input line is open.
     * @return True if the audio input line is open, false otherwise.
     */
    public boolean isOpen () {
        return aib.isOpen();
    }

    /**
     * Starts the audio input line.
     * @throws AudioBackendException If an error occurs while starting the backend.
     */
    public void start () throws AudioBackendException {
        aib.start();
    }

    /**
     * Stops the audio input line.
     * @throws AudioBackendException If an error occurs while stopping the backend.
     */
    public void stop () throws AudioBackendException {
        aib.stop();
    }

    /**
     * Flushes the audio input buffer, discarding any buffered data.
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    public void flush () throws AudioBackendException {
        aib.flush();
    }

    /**
     * Drains the audio input buffer, ensuring all buffered data is processed.
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    public void drain () throws AudioBackendException {
        aib.drain();
    }

    /**
     * Renders the specified audio data into the audio input line.
     * 
     * @param samples The audio data to be rendered.
     * @param sampleRate The sample rate of the audio data.
     * @throws AudioBackendException If an error occurs while rendering the audio data.
     */
    @Override
    public void render (float[][] samples, int sampleRate) throws AudioBackendException {
        int length = samples[0].length;
        int buffLength = length * audioFormat.getFrameSize();
        byte[] buffer = new byte[buffLength];
        aib.read(buffer, 0, buffLength);
        float[][] bufferSamp = SamplesConverter.toSamples(buffer, audioFormat);
        try {
            ArrayUtilities.copyArray(bufferSamp, samples);
        } catch (LengthMismatchException | ChannelsCountMismatchException e) {
            logger.error("Length or channels mismatch.", e);
            throw new RuntimeException("Length or channels mismatch: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the audio input line.
     * @throws AudioBackendException If an error occurs while closing the backend.
     */
    public void close () throws AudioBackendException {
        aib.close();
    }

    /**
     * Gets the audio format of the audio input line.
     * @return The audio format of the audio input line.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Gets the buffer size of the audio input line.
     * @return The buffer size of the audio input line.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Gets the audio input backend associated with the audio input line.
     * @return The audio input backend associated with the audio input line.
     */
    public AudioInputBackend getAudioInputBackend() {
        return aib;
    }
}
