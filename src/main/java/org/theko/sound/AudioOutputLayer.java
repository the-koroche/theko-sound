package org.theko.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.samples.SampleConverter;
import org.theko.sound.utility.ThreadUtilities;

/**
 * The {@code AudioOutputLayer} class provides a high-level interface for playing audio.
 * It acts as a wrapper around an {@link AudioOutputBackend}, handling the opening, starting, stopping,
 * and closing of audio output, as well as buffer management and audio data writing.
 * <p>
 * This class supports both direct audio data writing and audio processing via a root {@link AudioNode}.
 * When a root node is set, audio data is rendered through the node graph before being sent to the backend.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 *     AudioOutputLayer line = new AudioOutputLayer();
 *     line.open(audioFormat);
 *     line.start();
 *     // Write audio data or set a root node for processing
 *     line.stop();
 *     line.close();
 * </pre>
 *
 * <h2>Threading:</h2>
 * <p>
 * Audio processing is performed in a dedicated thread when a root node is set.
 * </p>
 *
 * <h2>Buffer Management:</h2>
 * <p>
 * The buffer size can be specified when opening the line. The buffer size affects latency and throughput.
 * </p>
 *
 * <h2>Exceptions:</h2>
 * <ul>
 *     <li>{@link AudioBackendCreationException} - Thrown if the backend cannot be created or opened.</li>
 *     <li>{@link AudioBackendNotFoundException} - Thrown if no suitable backend is found.</li>
 *     <li>{@link AudioBackendException} - Thrown for backend-specific errors during operation.</li>
 *     <li>{@link UnsupportedAudioFormatException} - Thrown if the audio format is not supported.</li>
 * </ul>
 * 
 * @see AudioNode
 * @see AudioOutputBackend
 *
 * @author Theko
 * @since v2.0.0
 */
public class AudioOutputLayer implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioOutputLayer.class);

    private final AudioOutputBackend aob;
    private AudioFormat audioFormat;
    private AudioNode rootNode;

    private Thread processingThread;
    private int bufferSize;

    /**
     * Constructs an {@code AudioOutputLayer} with the specified {@link AudioOutputBackend} backend.
     * 
     * @param aob The audio output backend to use.
     */
    public AudioOutputLayer (AudioOutputBackend aob) {
        if (aob == null) throw new IllegalArgumentException("AudioOutputBackend cannot be null");
        this.aob = aob;
        logger.debug("Created audio output line with backend: " + aob.getClass().getSimpleName());
    }

    /**
     * Constructs an {@code AudioOutputLayer} with the specified {@link AudioOutputBackend} backend.
     * 
     * @param aob The audio output backend to use.
     */
    public AudioOutputLayer () throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(AudioBackends.getOutputBackend(AudioBackends.getPlatformBackend()));
    }

    /**
     * Opens the audio output line with the specified port, format, and buffer size.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSizeInSamples The size of the buffer for audio data in samples.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    public void open (AudioPort port, AudioFormat audioFormat, int bufferSizeInSamples) throws AudioBackendException {
        try {
            AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT, audioFormat).get() : port);
            aob.open(targetPort, audioFormat, bufferSizeInSamples * audioFormat.getFrameSize());
            this.audioFormat = audioFormat;
            this.bufferSize = bufferSizeInSamples;
            logger.debug("Opened audio output line with {} port, {} format, and {} buffer size", targetPort, audioFormat, bufferSizeInSamples);
        } catch (AudioBackendException | AudioPortsNotFoundException ex) {
            throw new AudioBackendException("Failed to open audio output line.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            throw new AudioBackendException("Unsupported audio format.", ex);
        }
    }

    /**
     * Opens the audio output line with the specified port and format.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    public void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        this.open(port, audioFormat, AudioSystemProperties.AUDIO_OUTPUT_LAYER_BUFFER_SIZE);
    }

    /**
     * Opens the audio output line with the specified format.
     * 
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws AudioBackendException If an error occurs while opening the backend.
     */
    public void open (AudioFormat audioFormat) throws AudioBackendException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        this.open(null, audioFormat);
    }

    /**
     * Checks if the audio output line is open.
     * 
     * @return True if the audio output line is open, false otherwise.
     */
    public boolean isOpen () {
        return aob.isOpen();
    }

    /**
     * Starts the audio output line, processing audio data from the root node.
     * Creates a processing thread and starts it.
     * The thread is set as a daemon thread to not block JVM exit.
     * 
     * @throws AudioBackendException If an error occurs while starting the backend.
     */
    public void start () throws AudioBackendException {
        aob.start();
        processingThread = ThreadUtilities.createThread(
            "AudioOutputLayer-ProcessingThread",
            AudioSystemProperties.AUDIO_OUTPUT_LINE_THREAD_TYPE,
            AudioSystemProperties.AUDIO_OUTPUT_LINE_THREAD_PRIORITY,
            () -> {
                try {
                    process();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("Audio output line processing thread interrupted");
                } catch (Exception e) {
                    logger.error("Error in audio output line processing thread", e);
                }
            }
        );
        processingThread.setDaemon(true);
        processingThread.start();
        logger.debug("Started audio output line");
    }

    /**
     * Stops the audio output line and interrupts the processing thread.
     * @throws AudioBackendException If an error occurs while stopping the backend.
     */
    public void stop () throws AudioBackendException {
        aob.stop();
        if (processingThread != null && processingThread.isAlive()) {
            processingThread.interrupt();
        }
        logger.debug("Stopped audio output line");
    }

    /**
     * Flushes the audio output buffer, discarding any buffered data.
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    public void flush () throws AudioBackendException {
        aob.flush();
    }

    /**
     * Drains the audio output buffer, ensuring all buffered data is processed.
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    public void drain () throws AudioBackendException {
        aob.drain();
    }

    /**
     * Returns the number of available frames in the audio output buffer.
     * @return The number of available frames.
     */
    public int available () {
        return aob.available();
    }

    /**
     * Returns the size of the audio output buffer.
     * @return The size of the audio output buffer.
     */
    public int getLineBufferSize () {
        return bufferSize;
    }

    /**
     * Returns the size of the audio output backend's buffer.
     * @return The size of the audio output backend's buffer.
     */
    public int getBackendBufferSize () {
        return aob.getBufferSize();
    }

    /**
     * Sets the size of the audio output buffer.
     * @param bufferSize The new size of the audio output buffer.
     */
    public void setBufferSize (int bufferSize) {
        // This method doesn't update the audio output backend's buffer size
        this.bufferSize = bufferSize;
        logger.debug("Updated audio output layer buffer size to {}", bufferSize);
    }

    /**
     * Sets the root node for audio processing.
     * @param rootNode The new root node for audio processing.
     */
    public void setRootNode (AudioNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Closes the audio output line, stopping the processing thread and closing the backend.
     */
    @Override
    public void close () {
        stop();
        aob.close();
    }

    /**
     * Returns the audio format of the audio output line.
     * @return The audio format of the audio output line.
     */
    public AudioFormat getAudioFormat () {
        return audioFormat;
    }

    /**
     * Returns the current frame position of the audio output line.
     * @return The current frame position.
     */
    public long getFramePosition () {
        return aob.getFramePosition();
    }

    /**
     * Returns the current microsecond position of the audio output line.
     * @return The current microsecond position.
     */
    public long getMicrosecondPosition () {
        return aob.getMicrosecondPosition();
    }

    /**
     * Returns the latency of the audio output line in microseconds.
     * @return The latency in microseconds.
     */
    public long getMicrosecondLatency () {
        return aob.getMicrosecondLatency();
    }

    /**
     * Returns the current audio port of the audio output line.
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort () {
        return aob.getCurrentAudioPort();
    }

    /**
     * Processes audio data from the root node and writes it to the audio output backend.
     * @throws InterruptedException If the processing thread is interrupted.
     */
    private void process () throws InterruptedException {
        float[][] sampleBuffer = new float[audioFormat.getChannels()][bufferSize];
        long bufferMs = AudioConverter.samplesToMicrosecond(sampleBuffer, audioFormat.getSampleRate()) / 1000;
        while (!processingThread.isInterrupted()) {
            try {
                AudioNode snapshot = rootNode;
                if (snapshot == null) {
                    Thread.sleep(bufferMs);
                    continue;
                }
                snapshot.render(sampleBuffer, audioFormat.getSampleRate());
                if (sampleBuffer[0].length != bufferSize) {
                    throw new RuntimeException(new LengthMismatchException());
                }
                aob.write(SampleConverter.fromSamples(sampleBuffer, audioFormat), 0, bufferSize * audioFormat.getFrameSize());
            } catch (BackendNotOpenException ex) {
                logger.debug("Audio output line is closed");
                return;
            } catch (Exception ex) {
                logger.error("Error in audio output line processing thread", ex);
            }
        }
    }

    /**
     * Returns the audio output backend used by the audio output line.
     * @return The audio output backend.
     */
    public AudioOutputBackend getAudioOutputBackend () {
        return aob;
    }
}
