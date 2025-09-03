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

import static org.theko.sound.properties.AudioSystemProperties.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendInfo;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.event.EventDispatcher;
import org.theko.sound.event.EventHandler;
import org.theko.sound.event.EventType;
import org.theko.sound.event.OutputLayerEvent;
import org.theko.sound.event.OutputLayerListener;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.samples.SamplesValidation.ValidationResult;
import org.theko.sound.utility.FormatUtilities;
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

    /* Counter for instance IDs */
    private static final AtomicInteger instances = new AtomicInteger(0);
    private final int instanceId = instances.incrementAndGet();
    private final String baseName = "AudioOutputLayer" + instanceId;
    
    /* AOL opened state */
    private boolean isOpened = false;

    /* Processing and output threads */
    private Thread processingThread;
    private Thread outputThread;
    private final Object bufferLock = new Object();
    private int processingPriority = OUTPUT_LAYER_PROCESSING_THREAD_PRIORITY;
    private int outputPriority = OUTPUT_LAYER_OUTPUT_THREAD_PRIORITY;

    /* Audio output backend and buffers */
    private final AudioOutputBackend aob;
    private volatile byte[][] ringBuffers;
    private volatile int writeIndex, readIndex;

    /* Underrun behavior */
    private volatile UnderrunBehavior underrunBehavior = UnderrunBehavior.REPEAT_LAST;
    private volatile int underruns = 0;
    private byte[] lastAudioBuffer = null;
    private byte[] silenceBuffer = null;

    /* Audio formats, open-computed lengths */
    private AudioFormat sourceFormat;
    private AudioFormat openedFormat;
    private int resampledLength;
    private int rawLength;
    private int renderBufferSize;
    private long bufferTimeMicros;
    private long latencyMicros;

    /* Playback */
    private AudioNode rootNode;
    private boolean isPlaying;
    private float resamplingFactor = 1.0f;
    private AudioResampler resampler;
    private volatile boolean processedFirstBuffer = false; // To prevent underruns on the first frame

    /* Shutdown hook */
    private final Runnable shutdownHook;
    private final Thread shutdownHookThread;

    /* Event dispatcher */
    private final EventDispatcher<OutputLayerEvent, OutputLayerListener, OutputLayerEventType> eventDispatcher;

    protected enum OutputLayerEventType implements EventType<OutputLayerEvent> {
        OPENED, CLOSED,
        STARTED, STOPPED,
        FLUSHED, DRAINED,
        UNDERRUN, OVERRUN,
        PROCESSING_INTERRUPTED, OUTPUT_INTERRUPTED,
        LENGTH_MISMATCH,
        UNCHECKED_CLOSE,
        RENDER_EXCEPTION
    }

    public enum UnderrunBehavior {
        SILENCE,
        REPEAT_LAST
    }

    /**
     * Constructs an {@code AudioOutputLayer} with the specified {@link AudioOutputBackend} backend.
     * 
     * @param aob The audio output backend to use.
     * @throws IllegalArgumentException If the audio output backend is null.
     * @throws AudioBackendCreationException If an error occurs while creating the audio output backend.
     * @throws AudioBackendNotFoundException If the specified audio output backend is not found.
     */
    public AudioOutputLayer(AudioBackendInfo aobInfo) throws AudioBackendCreationException, AudioBackendNotFoundException, IllegalArgumentException {
        if (aobInfo == null) throw new IllegalArgumentException("AudioOutputBackend cannot be null.");
        this.aob = AudioBackends.getOutputBackend(aobInfo);
        logger.debug("Created {} with backend: {}.", baseName, aob.getClass().getSimpleName());

        shutdownHook = () -> {
            try {
                stop();
            } catch (Exception e) {
                logger.error("Failed to stop " + baseName + ".", e);
            }
            if (aob != null) {
                aob.close();
                aob.shutdown();
            }
            logger.debug("Shutted down {}.", baseName);
        };
        shutdownHookThread = new Thread(shutdownHook, baseName + "-ShutdownHook");
        aob.initialize();
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);

        resampler = new AudioResampler(OUTPUT_LAYER_RESAMPLE_METHOD, OUTPUT_LAYER_RESAMPLE_QUALITY);

        eventDispatcher = new EventDispatcher<>();
        Map<OutputLayerEventType, EventHandler<OutputLayerListener, OutputLayerEvent>> eventHandlers = new HashMap<>();
        eventHandlers.put(OutputLayerEventType.OPENED, OutputLayerListener::onOpened);
        eventHandlers.put(OutputLayerEventType.CLOSED, OutputLayerListener::onClosed);
        eventHandlers.put(OutputLayerEventType.STARTED, OutputLayerListener::onStarted);
        eventHandlers.put(OutputLayerEventType.STOPPED, OutputLayerListener::onStopped);
        eventHandlers.put(OutputLayerEventType.FLUSHED, OutputLayerListener::onFlushed);
        eventHandlers.put(OutputLayerEventType.DRAINED, OutputLayerListener::onDrained);        
        eventHandlers.put(OutputLayerEventType.UNDERRUN, OutputLayerListener::onUnderrun);
        eventHandlers.put(OutputLayerEventType.OVERRUN, OutputLayerListener::onOverrun);
        eventHandlers.put(OutputLayerEventType.PROCESSING_INTERRUPTED, OutputLayerListener::onProcessingInterrupted);
        eventHandlers.put(OutputLayerEventType.OUTPUT_INTERRUPTED, OutputLayerListener::onOutputInterrupted);
        eventHandlers.put(OutputLayerEventType.LENGTH_MISMATCH, OutputLayerListener::onLengthMismatch);
        eventHandlers.put(OutputLayerEventType.UNCHECKED_CLOSE, OutputLayerListener::onUncheckedClose);
        eventHandlers.put(OutputLayerEventType.RENDER_EXCEPTION, OutputLayerListener::onRenderException);
        eventDispatcher.setEventMap(eventHandlers);
    }
    
    private final OutputLayerEvent getEvent() {
        int outBufferSize = (aob != null && aob.isOpen() ? aob.getBufferSize() : -1);
        int buffersCount = (ringBuffers != null ? ringBuffers.length : -1);
        return new OutputLayerEvent(
            sourceFormat, openedFormat,
            renderBufferSize, outBufferSize,
            buffersCount);
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
     * @param buffersCount The number of audio ring buffers to be used.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio port, audio format, buffer size, or buffer count are invalid.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, int bufferSizeInSamples, int buffersCount) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioPortsNotFoundException, AudioBackendException {
        if (isOpened) {
            logger.warn("Audio output line is already open.");
            return;
        }

        if (audioFormat == null) throw new IllegalArgumentException("Audio format cannot be null.");
        if (bufferSizeInSamples <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0.");
        if (buffersCount <= 0) throw new IllegalArgumentException("Buffer count must be greater than 0.");
        if (buffersCount > 4) throw new IllegalArgumentException("Buffer count must be less than or equal to 4, due to inefficiency.");

        logger.debug("Trying to open with port: {}, format: {}, buffer size: {}.",
                port != null ? port.toString() : "Default", audioFormat, bufferSizeInSamples);
        AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT).get() : port);
        if (targetPort == null) {
            throw new AudioPortsNotFoundException("No default output port was found.");
        }
        if (port == null) {
            logger.debug("Using default output port: {}.", targetPort);
        }

        AudioFormat targetFormat = audioFormat;

        AtomicReference<AudioFormat> closestFormat = new AtomicReference<>();
        closestFormat.set(null);
        if (!aob.isFormatSupported(targetPort, targetFormat, closestFormat)) {
            logger.debug("Audio format is not supported. Using closest supported format: {}.", closestFormat.get());
            if (closestFormat.get() != null) {
                targetFormat = closestFormat.get();
                logger.debug("Using closest supported format: {}.", targetFormat);
            } else {
                // Using targetPort's mix format
                targetFormat = targetPort.getMixFormat();
                logger.debug("Audio format is not supported. Using target port's mix format: {}.", targetFormat);
                closestFormat.set(null);

                if (!aob.isFormatSupported(targetPort, targetFormat, closestFormat)) {
                    logger.debug("Mix format is not supported. Using closest supported format: {}.", closestFormat.get());
                    if (closestFormat.get() != null) {
                        targetFormat = closestFormat.get();
                        logger.debug("Using closest supported format: {}.", targetFormat);
                    } else {
                        logger.error("Failed to open audio output line. Unsupported audio format: {}.", targetFormat);
                        throw new UnsupportedAudioFormatException(
                            "Unsupported audio format. Source format: %s. Used format: %s.".formatted(audioFormat, targetFormat)
                        );
                    }
                }
            }
        }

        if (!targetFormat.equals(audioFormat)) {
            resamplingFactor = (float) audioFormat.getSampleRate() / (float) targetFormat.getSampleRate();
            logger.info(
                "Audio format conversion: {} -> {}. Resampling factor: '{}'.",
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

        resampledLength = (int)(renderBufferSize / resamplingFactor);
        rawLength = resampledLength * openedFormat.getChannels() * openedFormat.getBytesPerSample();
        bufferTimeMicros = AudioUnitsConverter.framesToMicroseconds(
            renderBufferSize,
            (int)(sourceFormat.getSampleRate())
        );
        latencyMicros = aob.getMicrosecondLatency() + bufferTimeMicros * buffersCount;
        logger.debug("Render buffer size: {}. Buffer time: {}. Latency (computed): {}.",
                renderBufferSize,
                FormatUtilities.formatTime(bufferTimeMicros*1000, 4),
                FormatUtilities.formatTime(latencyMicros*1000, 4)
            );

        processedFirstBuffer = false;

        silenceBuffer = new byte[rawLength];
        Arrays.fill(silenceBuffer, (byte) 0);

        ringBuffers = new byte[buffersCount][rawLength];

        isOpened = true;
        eventDispatcher.dispatch(OutputLayerEventType.OPENED, getEvent());

        logger.debug(
            "Opened with port {}, format {}, buffer size {}.",
            targetPort,
            targetFormat,
            bufferSizeInSamples
        );
    }

    /**
     * Opens the audio output line with the specified port, format, and buffer size.
     * If the port is null, the default output port will be used.
     * It creates a double buffered audio output line.
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
        this.open(port, audioFormat, bufferSizeInSamples, 2 /* Double buffering */); 
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
        this.open(port, audioFormat, AudioSystemProperties.OUTPUT_LAYER_BUFFER_SIZE);
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
        return aob.isOpen() && isOpened;
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
        processingThread = ThreadUtilities.startThread(
            baseName + "-Processing",
            AudioSystemProperties.OUTPUT_LAYER_PROCESSING_THREAD_TYPE,
            processingPriority,
            () -> {
                try {
                    process();
                } catch (Exception ex) {
                    logger.error("Error in AudioOutputLayer{}-Processing.", instanceId, ex);
                }
            }
        );
        if (processingThread == null) 
            throw new RuntimeException(
                "Failed to start " + baseName + "-Processing. Thread is null."
            );
        outputThread = ThreadUtilities.startThread(
            baseName + "-Output",
            AudioSystemProperties.OUTPUT_LAYER_OUTPUT_THREAD_TYPE,
            outputPriority,
            () -> {
                try {
                    output();
                } catch (Exception ex) {
                    logger.error("Error in AudioOutputLayer{}-Output.", instanceId, ex);
                }
            }
        );
        if (outputThread == null) 
            throw new RuntimeException(
                "Failed to start " + baseName + "-Output. Thread is null."
            );
        underruns = 0;
        isPlaying = true;
        logger.debug("Started {}. Processing thread: {}, Output thread: {}.", baseName, processingThread, outputThread);
        eventDispatcher.dispatch(OutputLayerEventType.STARTED, getEvent());
    }

    /**
     * Stops the audio output line and interrupts the processing thread.
     * @throws AudioBackendException If an error occurs while stopping the backend.
     * @throws InterruptedException If the threads join operation is interrupted.
     */
    public void stop() throws AudioBackendException, InterruptedException {
        if (!isPlaying) return;
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
        }
        if (processingThread != null && processingThread.isAlive()) {
            processingThread.interrupt();
        }
        try {
            if (outputThread != null && outputThread.isAlive()) outputThread.join(1000);
            if (processingThread != null && processingThread.isAlive()) processingThread.join(1000);
        } catch (InterruptedException ex) {
            logger.error("Interrupted while joining output and processing threads, in " + baseName + ".", ex);
            throw ex;
        }
        if (outputThread != null && outputThread.isAlive()) {
            logger.warn("Cannot close output thread in {}. Stopping output backend.", baseName);
        }
        aob.stop();
        isPlaying = false;
        logger.debug("Stopped {}.", baseName);
        eventDispatcher.dispatch(OutputLayerEventType.STOPPED, getEvent());
    }

    /**
     * Closes the audio output line, stopping the processing thread and closing the backend.
     * Stop can take some time, so it is recommended to call it in a separate thread.
     * 
     * @throws InterruptedException If the threads join operation is interrupted while stopping.
     * @throws AudioBackendException If an error occurs while closing the backend
     */
    @Override
    public void close() throws AudioBackendException, InterruptedException {
        if (!isOpened) {
            logger.info("{} is already closed.", baseName);
            return;
        }
        stop();
        aob.close();
        aob.shutdown();
        try {
            boolean result = Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            if (!result) {
                logger.warn("Shutdown hook was not removed (maybe already removed or never registered?).");
            }
        } catch (IllegalStateException ex) {
            logger.warn("Shutdown hook is running.", ex);
        } catch (SecurityException ex) {
            logger.error("Cannot remove shutdown hook due to security restrictions.", ex);
        }
        isOpened = false;
        logger.debug("Closed {}.", baseName);
        eventDispatcher.dispatch(OutputLayerEventType.CLOSED, getEvent());
    }

    /**
     * Flushes the audio output buffer, discarding any buffered data.
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    public void flush() throws AudioBackendException {
        aob.flush();
        logger.trace("Flushed {}.", baseName);
        eventDispatcher.dispatch(OutputLayerEventType.FLUSHED, getEvent());
    }

    /**
     * Drains the audio output buffer, ensuring all buffered data is processed.
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    public void drain() throws AudioBackendException {
        aob.drain();
        logger.trace("Drained {}.", baseName);
        eventDispatcher.dispatch(OutputLayerEventType.DRAINED, getEvent());
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
     * Returns the priority of the processing thread.
     * @return The priority of the processing thread.
     */
    public int getProcessingThreadPriority() {
        return processingPriority;
    }

    /**
     * Returns the priority of the output thread.
     * @return The priority of the output thread.
     */
    public int getOutputThreadPriority() {
        return outputPriority;
    }

    /**
     * Sets the priority of the processing thread.
     * @param priority The new priority of the processing thread.
     */
    public void setProcessingThreadPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalArgumentException("Priority must be between " + Thread.MIN_PRIORITY + " and " + Thread.MAX_PRIORITY + ".");
        this.processingPriority = priority;
        if (processingThread != null && processingThread.isAlive()) {
            processingThread.setPriority(priority);
        }
    }

    /**
     * Sets the priority of the output thread.
     * @param priority The new priority of the output thread.
     */
    public void setOutputThreadPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalArgumentException("Priority must be between " + Thread.MIN_PRIORITY + " and " + Thread.MAX_PRIORITY + ".");
        this.outputPriority = priority;
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.setPriority(priority);
        }
    }

    /**
     * Sets the root node for audio processing.
     * <p>If the root node is null, the output layer will wait buffer-time, until a new root node is set.</p>
     * @param rootNode The new root node for audio processing (can be null).
     */
    public void setRootNode(AudioNode rootNode) {
        if (rootNode == null) {
            logger.info("Root node is null.");
        }
        this.rootNode = rootNode;
    }

    /**
     * Sets the underrun behavior for the output layer.
     * @param behavior The new underrun behavior.
     */
    public void setUnderrunBehavior(UnderrunBehavior behavior) {
        this.underrunBehavior = behavior;
    }

    /**
     * Returns the underrun behavior for the output layer.
     * @return The underrun behavior.
     */
    public UnderrunBehavior getUnderrunBehavior() {
        return underrunBehavior;
    }

    /**
     * Returns the number of underruns that have occurred since the last start.
     * @return The number of underruns.
     */
    public long getUnderrunCount() {
        return underruns;
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
     * Returns the latency of this output layer in microseconds.
     * @return The latency in microseconds.
     */
    public long getMicrosecondLatency() {
        return latencyMicros;
    }

    /**
     * Returns the current audio port of the audio output line.
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort() {
        return aob.getCurrentAudioPort();
    }

    /* Processing and Output */

    private byte[] readAudio() {
        synchronized(bufferLock) {
            if (readIndex == writeIndex) {
                if (processedFirstBuffer) {
                    logger.warn("Underrun in {}. Total underruns: {}", baseName, underruns);
                    eventDispatcher.dispatch(OutputLayerEventType.UNDERRUN, getEvent());
                } else {
                    return silenceBuffer;
                }
                underruns++;
                switch (underrunBehavior) {
                    case SILENCE:
                        return silenceBuffer;
                    case REPEAT_LAST:
                        return lastAudioBuffer != null ? lastAudioBuffer : silenceBuffer;
                    default:
                        return silenceBuffer;
                }
            }
            byte[] output = ringBuffers[readIndex];
            lastAudioBuffer = output;
            readIndex = (readIndex + 1) % ringBuffers.length;
            bufferLock.notifyAll();
            return output;
        }
    }

    private void writeAudio(byte[] data) throws InterruptedException {
        synchronized(bufferLock) {
            /*if ((writeIndex + 1) % buffersList.size() == readIndex) {
                logger.info("Overrun in {}", baseName);
                eventDispatcher.dispatch(OutputLayerEventType.OVERRUN, getEvent());
            }*/
            while ((writeIndex + 1) % ringBuffers.length == readIndex) {
                bufferLock.wait();
            }
            System.arraycopy(data, 0, ringBuffers[writeIndex], 0, data.length);
            writeIndex = (writeIndex + 1) % ringBuffers.length;
            bufferLock.notifyAll();
        }
    }

    private void output() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] data = readAudio();
                aob.write(data, 0, data.length);
            } catch (BackendNotOpenException e) {
                logger.info("{} is closed", baseName);
                eventDispatcher.dispatch(OutputLayerEventType.UNCHECKED_CLOSE, getEvent());
                return;
            } catch (AudioBackendException e) {
                logger.error("Error writing to audio backend.", e);
                break;
            }/* catch (InterruptedException e) {
                logger.debug("{}-Output thread is interrupted.", baseName);
                eventDispatcher.dispatch(OutputLayerEventType.OUTPUT_INTERRUPTED, getEvent());
                Thread.currentThread().interrupt();
                break;
            } */catch (Exception e) {
                logger.error("Error in " + baseName + "-Output thread.", e);
                break;
            }
        }
    }

    private static class ProcessingException extends RuntimeException {
        
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProcessingException(Throwable cause) {
            super(cause);
        }
    }

    private void process() {
        float[][] sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
        float[][] resampled = new float[openedFormat.getChannels()][resampledLength];
        byte[] rawBytes = new byte[rawLength];

        int lengthMismatchCounter = 0;

        // Use shorter buffer to prevent underruns
        long bufferMcsWait = Math.min(bufferTimeMicros - 1000, 100);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                long startRenderNs = System.nanoTime();
                AudioNode snapshot = rootNode;
                if (snapshot == null) {
                    waitMicros(bufferMcsWait);
                    continue;
                }

                try {
                snapshot.render(sampleBuffer, (int)(sourceFormat.getSampleRate()));
                } catch (IllegalArgumentException ex) {
                    logger.error("Passed wrong arguments to the render method (invalid sample rate?).", ex);
                } catch (MixingException ex) {
                    logger.error("Mixing error in " + baseName + "-Processing thread.", ex);
                }
                // if changed the channels or samples length
                if (SamplesValidation.isValidSamples(sampleBuffer) != ValidationResult.VALID || !SamplesValidation.checkLength(sampleBuffer, renderBufferSize)) {
                    logger.error("Length mismatch in {}-Output thread. Expected {} got {}. Counter: {}",
                            baseName, renderBufferSize, sampleBuffer[0].length, lengthMismatchCounter);
                    lengthMismatchCounter++;
                    eventDispatcher.dispatch(OutputLayerEventType.LENGTH_MISMATCH, getEvent());
                    // Initialize buffers again
                    if (lengthMismatchCounter < 10) {
                        sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
                        waitMicros(bufferMcsWait);
                        continue;
                    } else {
                        logger.error("Too many length mismatches in {}-Processing thread. Aborting.", baseName);
                        throw new LengthMismatchException("Length mismatch in " + baseName + "-Processing thread. Expected " + renderBufferSize + " got " + sampleBuffer[0].length);
                    }
                }

                try {
                    resampler.resample(sampleBuffer, resampled, resamplingFactor);
                    SamplesConverter.fromSamples(resampled, rawBytes, openedFormat);
                } catch (IllegalArgumentException ex) {
                    logger.error("Passed wrong arguments to the resamping or conversion methods (internal error?).", ex);
                    throw new ProcessingException("Internal error", ex);
                }

                writeAudio(rawBytes);
                processedFirstBuffer = true;

                long endRenderNs = System.nanoTime();
                long durationRenderNs = endRenderNs - startRenderNs;
                waitMicros(bufferMcsWait - (int)(durationRenderNs * 0.001));
            } catch (LengthMismatchException ex) {
                // Already logged
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                logger.debug(baseName + "-Processing thread interrupted.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.PROCESSING_INTERRUPTED, getEvent());
                Thread.currentThread().interrupt();  
            } catch (ProcessingException ex) { 
                logger.error("Error in " + baseName + "-Processing thread.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw ex;
            } catch (Exception ex) {
                logger.error("Error in " + baseName + "-Processing thread.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw new RuntimeException(ex);
            }
        }
    }

    private void waitMicros(long micros) {
        if (micros <= 0) return;
        final long deadline = System.nanoTime() + micros * 1000;
        long remaining;

        while ((remaining = deadline - System.nanoTime()) > 0) {
            if (remaining > 100_000) { 
                LockSupport.parkNanos(remaining - 50_000);
            } else if (remaining > 10_000) { 
                Thread.onSpinWait();
            } else if (remaining > 0) {
                long target = System.nanoTime() + remaining;
                while (System.nanoTime() < target) {
                    Thread.onSpinWait();
                }
                break;
            }
        }
    }
}
