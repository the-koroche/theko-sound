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
    private int bufferSize = 2048;

    public AudioOutputLayer (AudioOutputBackend aob) {
        this.aob = aob;
        logger.debug("Created audio output line: " + aob);
    }

    public AudioOutputLayer () throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(AudioBackends.getOutputBackend(AudioBackends.getPlatformBackend()));
    }

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

    public void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendException {
        this.open(port, audioFormat, AudioSystemProperties.AUDIO_OUTPUT_LAYER_BUFFER_SIZE);
    }

    public void open (AudioFormat audioFormat) throws AudioBackendException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        this.open(null, audioFormat);
    }

    public boolean isOpen () {
        return aob.isOpen();
    }

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

    public void stop () throws AudioBackendException {
        aob.stop();
        processingThread.interrupt();
        logger.debug("Stopped audio output line");
    }

    public void flush () throws AudioBackendException {
        aob.flush();
    }

    public void drain () throws AudioBackendException {
        aob.drain();
    }

    public int available () {
        return aob.available();
    }

    public int getLineBufferSize () {
        return bufferSize;
    }

    public int getBackendBufferSize () {
        return aob.getBufferSize();
    }

    public void setBufferSize (int bufferSize) {
        // This method doesn't update the audio output backend's buffer size
        this.bufferSize = bufferSize;
        logger.debug("Updated audio output layer buffer size to {}", bufferSize);
    }

    public void setRootNode (AudioNode rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public void close () {
        aob.close();
    }

    public AudioFormat getAudioFormat () {
        return audioFormat;
    }

    public long getFramePosition () {
        return aob.getFramePosition();
    }

    public long getMicrosecondPosition () {
        return aob.getMicrosecondPosition();
    }

    public long getMicrosecondLatency () {
        return aob.getMicrosecondLatency();
    }

    public AudioPort getCurrentAudioPort () {
        return aob.getCurrentAudioPort();
    }

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

    public AudioOutputBackend getAudioOutputBackend () {
        return aob;
    }
}
