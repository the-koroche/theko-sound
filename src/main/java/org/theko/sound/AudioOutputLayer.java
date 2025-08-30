/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendInfo;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.utility.ThreadUtilities;

/**
 * The {@code AudioOutputLayer} class is responsible for managing an audio output line.
 * It encapsulates an {@link AudioOutputBackend} and provides a unified interface for
 * audio output operations. It is possible to add {@link AudioNode} to this
 * class as a root node, which can be used to process audio data in real-time.
 * <p>
 * This class is {@link AutoCloseable}, so it can be used in a try-with-resources
 * statement. It is also thread-safe.
 * <p>
 * The class provides methods to open and close the audio output line, start and stop
 * audio playback, and retrieve information about the audio line, such as buffer size,
 * frame position, and latency.
 * <p>
 * <p>
 * Also, this class adds a shutdown hook to close the audio output line when the
 * JVM is shut down, ensuring proper cleanup.
 * <p>
 * Usage example:
 * <pre>{@code
 * try (AudioOutputLayer aol = new AudioOutputLayer()) {
 *     aol.open(audioPort, audioFormat);
 *     aol.setRootNode(audioNode); // Mixers, Sources, Generators
 *     aol.start();
 *     // ...
 * }
 * }</pre>
 * <p>
 * Note: This class throws {@link AudioBackendCreationException},
 * {@link AudioBackendException}, and {@link BackendNotOpenException} for various error
 * conditions, such as unsupported audio formats or attempting to operate on a closed
 * backend.
 * 
 * @see AudioOutputBackend
 * @see AudioNode
 * 
 * @since 2.0.0
 * @author Theko
 */
public class AudioOutputLayer implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioOutputLayer.class);

    private final AudioOutputBackend aob;
    private AudioFormat sourceFormat;
    private AudioFormat openedFormat;
    private AudioNode rootNode;

    private Thread outputThread;
    private boolean isPlaying;
    private int renderBufferSize;

    private float resamplingFactor = 1.0f;

    private final Runnable shutdownHook;
    private final Thread shutdownHookThread;

    /**
     * Constructs an {@code AudioOutputLayer} with the specified {@link AudioOutputBackend} backend.
     * 
     * @param aob The audio output backend to use.
     * @throws IllegalArgumentException If the audio output backend is null.
     * @throws AudioBackendCreationException If an error occurs while creating the audio output backend.
     * @throws AudioBackendNotFoundException If the specified audio output backend is not found.
     */
    public AudioOutputLayer(AudioBackendInfo aobInfo) throws AudioBackendCreationException, AudioBackendNotFoundException, IllegalArgumentException {
        if (aobInfo == null) throw new IllegalArgumentException("AudioOutputBackend cannot be null");
        this.aob = AudioBackends.getOutputBackend(aobInfo);
        logger.debug("Created audio output line with backend: " + aob.getClass().getSimpleName());

        shutdownHook = () -> {
            stop();
            if (aob != null) {
                aob.close();
                aob.shutdown();
            }
            logger.debug("Shutting down audio output line");
        };
        shutdownHookThread = new Thread(shutdownHook, "AudioOutputLayer-ShutdownHook");
        aob.initialize();
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    /**
     * Constructs an {@code AudioOutputLayer} with the specified {@link AudioOutputBackend} backend.
     * 
     * @param aob The audio output backend to use.
     * @throws AudioBackendCreationException If an error occurs while creating the audio output backend.
     * @throws AudioBackendNotFoundException If the specified audio output backend is not found.
     */
    public AudioOutputLayer() throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(AudioBackends.getPlatformBackend());
    }

    
    /**
     * Opens the audio output line with the specified port, format, and buffer size.
     * If the port is null, the default output port will be used.
     * This method also tries to use best matching audio format, if the given format is not supported.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSizeInSamples The size of the buffer for audio data in samples.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio port, audio format, or buffer size are invalid.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSizeInSamples) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioPortsNotFoundException, AudioBackendException {
        if (audioFormat == null) throw new IllegalArgumentException("Audio format cannot be null.");
        if (bufferSizeInSamples <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0.");

        logger.debug("Trying to open with port: {}, format: {}, buffer size: {}.",
                port != null ? port.toString() : "Default", audioFormat, bufferSizeInSamples);
        AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT).get() : port);
        if (targetPort == null) {
            throw new AudioPortsNotFoundException("No default output port was found.");
        }
        if (port == null) {
            logger.debug("Using default output port: {}", targetPort);
        }

        AudioFormat targetFormat = audioFormat;

        AtomicReference<AudioFormat> closestFormat = new AtomicReference<>();
        closestFormat.set(null);
        if (!aob.isFormatSupported(targetPort, targetFormat, closestFormat)) {
            logger.debug("Audio format is not supported. Using closest supported format: {}", closestFormat.get());
            if (closestFormat.get() != null) {
                targetFormat = closestFormat.get();
                logger.debug("Using closest supported format: {}", targetFormat);
            } else {
                // Using targetPort's mix format
                targetFormat = targetPort.getMixFormat();
                logger.debug("Audio format is not supported. Using target port's mix format: {}", targetFormat);
                closestFormat.set(null);

                if (!aob.isFormatSupported(targetPort, targetFormat, closestFormat)) {
                    logger.debug("Mix format is not supported. Using closest supported format: {}", closestFormat.get());
                    if (closestFormat.get() != null) {
                        targetFormat = closestFormat.get();
                        logger.debug("Using closest supported format: {}", targetFormat);
                    } else {
                        logger.error("Failed to open audio output line. Unsupported audio format: {}", targetFormat);
                        throw new UnsupportedAudioFormatException(
                            "Unsupported audio format. Source format: %s. Used format: %s".formatted(audioFormat, targetFormat)
                        );
                    }
                }
            }
        }

        if (!targetFormat.equals(audioFormat)) {
            resamplingFactor = (float) audioFormat.getSampleRate() / (float) targetFormat.getSampleRate();
            logger.info(
                "Audio format conversion: {} -> {}. Resampling factor: {}",
                audioFormat,
                targetFormat,
                resamplingFactor
            );
        } else {
            resamplingFactor = 1.0f;
        }

        aob.open(targetPort, targetFormat, bufferSizeInSamples * targetFormat.getFrameSize());
        this.sourceFormat = audioFormat;
        this.openedFormat = targetFormat;
        this.renderBufferSize = bufferSizeInSamples;

        logger.debug(
            "Opened with port {}, format {}, buffer size {}.",
            targetPort,
            targetFormat,
            bufferSizeInSamples
        );
    }

    /**
     * Opens the audio output line with the specified port and format.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio format is null.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioBackendException, AudioPortsNotFoundException {
        this.open(port, audioFormat, AudioSystemProperties.AUDIO_OUTPUT_LAYER_BUFFER_SIZE);
    }

    /**
     * Opens the audio output line with the specified format.
     * 
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio format is null.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioFormat audioFormat) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioBackendException, AudioPortsNotFoundException {
        this.open(null, audioFormat);
    }

    /**
     * Checks if the audio output line is open.
     * 
     * @return True if the audio output line is open, false otherwise.
     */
    public boolean isOpen() {
        return aob.isOpen();
    }

    /**
     * Starts the audio output line, processing audio data from the root node.
     * Creates a processing thread and starts it.
     * The thread is set as a daemon thread to not block JVM exit.
     * 
     * @throws AudioBackendException If an error occurs while starting the backend.
     * @throws RuntimeException If the processing thread cannot be started.
     */
    public void start() throws AudioBackendException {
        if (isPlaying) return;
        aob.start();
        outputThread = ThreadUtilities.startThread(
            "AudioOutputLayer-OutputThread",
            AudioSystemProperties.OUTPUT_LAYER_THREAD_TYPE,
            AudioSystemProperties.OUTPUT_LAYER_THREAD_PRIORITY,
            () -> {
                try {
                    process();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.debug("Audio output line processing thread interrupted");
                } catch (Exception ex) {
                    logger.error("Error in audio output line processing thread", ex);
                }
            }
        );
        if (outputThread == null) 
            throw new RuntimeException(
                "Failed to start audio output line processing thread. Processing thread is null.");
        isPlaying = true;
        logger.debug("Started audio output line. Processing thread: {}", outputThread);
    }

    /**
     * Stops the audio output line and interrupts the processing thread.
     * @throws AudioBackendException If an error occurs while stopping the backend.
     */
    public void stop() throws AudioBackendException {
        if (!isPlaying) return;
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        aob.stop();
        isPlaying = false;
        logger.debug("Stopped audio output line");
    }

    /**
     * Flushes the audio output buffer, discarding any buffered data.
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    public void flush() throws AudioBackendException {
        aob.flush();
    }

    /**
     * Drains the audio output buffer, ensuring all buffered data is processed.
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    public void drain() throws AudioBackendException {
        aob.drain();
    }

    /**
     * Returns the number of available frames in the audio output buffer.
     * @return The number of available frames.
     */
    public int available() {
        return aob.available();
    }

    /**
     * Returns the size of the audio output buffer.
     * @return The size of the audio output buffer.
     */
    public int getRenderBufferSize() {
        return renderBufferSize;
    }

    /**
     * Returns the size of the audio output backend's buffer.
     * @return The size of the audio output backend's buffer.
     */
    public int getBackendBufferSize() {
        return aob.getBufferSize();
    }

    /**
     * Sets the root node for audio processing.
     * @param rootNode The new root node for audio processing.
     */
    public void setRootNode(AudioNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Closes the audio output line, stopping the processing thread and closing the backend.
     */
    @Override
    public void close() {
        stop();
        aob.close();
        aob.shutdown();
        Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
        logger.debug("Closed audio output line");
    }

    /**
     * Returns the audio format of the audio output line.
     * @return The audio format of the audio output line.
     */
    public AudioFormat getSourceFormat() {
        return sourceFormat;
    }

    /**
     * Returns the audio format which is used to open the line.
     * @return The opened audio format.
     */
    public AudioFormat getOpenedFormat() {
        return openedFormat;
    }

    /**
     * Returns the current frame position of the audio output line.
     * @return The current frame position.
     */
    public long getFramePosition() {
        return aob.getFramePosition();
    }

    /**
     * Returns the current microsecond position of the audio output line.
     * @return The current microsecond position.
     */
    public long getMicrosecondPosition() {
        return aob.getMicrosecondPosition();
    }

    /**
     * Returns the latency of the audio output line in microseconds.
     * @return The latency in microseconds.
     */
    public long getMicrosecondLatency() {
        return aob.getMicrosecondLatency();
    }

    /**
     * Returns the current audio port of the audio output line.
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort() {
        return aob.getCurrentAudioPort();
    }

    /**
     * Processes audio data from the root node and writes it to the audio output backend.
     * @throws InterruptedException If the processing thread is interrupted.
     */
    private void process() throws InterruptedException {
        float[][] sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
        long bufferMs = AudioUnitsConverter.framesToMicroseconds(
            renderBufferSize,
            (int)(openedFormat.getSampleRate())
        ) / 1000;

        logger.debug("Render buffer size: {}. Buffer ms: {}", renderBufferSize, bufferMs);
        
        int resampledLength = (int)(renderBufferSize / resamplingFactor);
        int rawLength = resampledLength * openedFormat.getChannels() * openedFormat.getBytesPerSample();
        float[][] resampled = new float[openedFormat.getChannels()][resampledLength];
        byte[] rawBytes = new byte[rawLength];

        while (!Thread.currentThread().isInterrupted()) {
            try {
                AudioNode snapshot = rootNode;
                if (snapshot == null) {
                    Thread.sleep(bufferMs);
                    continue;
                }

                snapshot.render(sampleBuffer, (int)(sourceFormat.getSampleRate()));
                if (sampleBuffer.length == 0 || sampleBuffer[0] == null || sampleBuffer[0].length != renderBufferSize) {
                    logger.error("Length mismatch in audio output line processing thread. Expected {} got {}", renderBufferSize, sampleBuffer[0].length);
                    throw new LengthMismatchException("Buffer size mismatch. Expected " + renderBufferSize + ", got " + sampleBuffer[0].length);
                }

                AudioResampler.SHARED.resample(sampleBuffer, resampled, resamplingFactor);
                SamplesConverter.fromSamples(resampled, rawBytes, openedFormat);

                aob.write(rawBytes, 0, rawBytes.length);
            } catch (BackendNotOpenException ex) {
                logger.info("Audio output line is closed");
                return;
            } catch (LengthMismatchException ex) {
                logger.error("Length mismatch in audio output line processing thread", ex);
                throw new RuntimeException(ex);
            } catch (Exception ex) {
                logger.error("Error in audio output line processing thread", ex);
            }
        }
    }
}
