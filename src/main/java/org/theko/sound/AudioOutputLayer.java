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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.EventDispatcher;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendInfo;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.backend.DeviceInactiveException;
import org.theko.sound.backend.DeviceInvalidatedException;
import org.theko.sound.event.OutputLayerEvent;
import org.theko.sound.event.OutputLayerEventType;
import org.theko.sound.event.OutputLayerListener;
import org.theko.sound.properties.ThreadType;
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.samples.SamplesValidation.ValidationResult;
import org.theko.sound.utility.FormatUtilities;
import org.theko.sound.utility.ThreadUtilities;

/**
 * The {@code AudioOutputLayer} class is responsible for managing an audio output.
 * It encapsulates an {@link AudioOutputBackend} and provides a unified interface for
 * audio output operations. It is possible to add {@link AudioNode} to this
 * class as a root node, which can be used to process audio data in real-time.
 * <p>
 * This class is {@link AutoCloseable}, so it can be used in a try-with-resources
 * statement. It is also thread-safe.
 * <p>
 * The class provides methods to open and close the audio output, start and stop
 * audio playback, and retrieve information about the audio line, such as buffer size,
 * frame position, and latency.
 * <p>
 * <p>
 * Also, this class adds a shutdown hook to close the audio output when the
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

    private static final int TIME_FORMAT_PRECISION = 4;

    /* Counter for instance IDs */
    private static final AtomicInteger instances = new AtomicInteger(0);
    private final int instanceId = instances.incrementAndGet();
    private final String baseName = "AudioOutputLayer" + instanceId;
    
    private boolean isOpened = false;

    /* Processing and output threads */
    private Thread processingThread;
    private Thread outputThread;
    private final Object bufferLock = new Object();
    private int processingPriority = AOL_PROCESSING_THREAD.priority;
    private int outputPriority = AOL_OUTPUT_THREAD.priority;

    /* Audio output backend and buffers */
    private final AudioOutputBackend aob;
    private AudioPort openedPort;
    private volatile byte[][] ringBuffers;
    private int buffersCount;
    private AtomicInteger writeIndex = new AtomicInteger(0);
    private AtomicInteger readIndex = new AtomicInteger(0);
    private AtomicInteger writeFailures = new AtomicInteger(0);

    /* Underrun behavior */
    private volatile UnderrunBehavior underrunBehavior = UnderrunBehavior.REPEAT_LAST;
    private AtomicLong underruns = new AtomicLong(0);
    private byte[] lastAudioBuffer = null;
    private byte[] silenceBuffer = null;

    /* Audio formats, open-computed lengths */
    private AudioFormat sourceFormat;
    private AudioFormat openedFormat;
    private int resampledLength;
    private int rawLength;
    private int renderBufferSize; // frames
    private int outputBufferSize; // frames
    private long bufferTimeMicros;
    private long latencyMicros;

    /* Playback */
    private AudioNode rootNode;
    private boolean isPlaying;
    private float resamplingFactor = 1.0f;
    private AudioResampler resampler;

    /* Shutdown hook */
    private final Runnable shutdownHook;
    private final Thread shutdownHookThread;

    /* Event dispatcher */
    private final EventDispatcher<OutputLayerEvent, OutputLayerListener, OutputLayerEventType> eventDispatcher;

    /**
     * An enumeration of underrun behavior options.
     */
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
                logger.error("Failed to stop {}.", baseName, e);
            }
            if (aob != null) {
                aob.close();
                aob.shutdown();
            }
            logger.debug("Shutted down {}.", baseName);
        };
        shutdownHookThread = new Thread(shutdownHook, baseName + "-ShutdownHook");
        aob.initialize();
        if (AOL_ENABLE_SHUTDOWN_HOOK) {
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        }

        resampler = new AudioResampler(AOL_RESAMPLER.resampleMethod, AOL_RESAMPLER.quality);

        eventDispatcher = new EventDispatcher<>();
        var eventMap = eventDispatcher.createEventMap();
        eventMap.put(OutputLayerEventType.OPENED, OutputLayerListener::onOpened);
        eventMap.put(OutputLayerEventType.CLOSED, OutputLayerListener::onClosed);
        eventMap.put(OutputLayerEventType.STARTED, OutputLayerListener::onStarted);
        eventMap.put(OutputLayerEventType.STOPPED, OutputLayerListener::onStopped);
        eventMap.put(OutputLayerEventType.FLUSHED, OutputLayerListener::onFlushed);
        eventMap.put(OutputLayerEventType.DRAINED, OutputLayerListener::onDrained);        
        eventMap.put(OutputLayerEventType.UNDERRUN, OutputLayerListener::onUnderrun);
        eventMap.put(OutputLayerEventType.PROCESSING_INTERRUPTED, OutputLayerListener::onProcessingInterrupted);
        eventMap.put(OutputLayerEventType.OUTPUT_INTERRUPTED, OutputLayerListener::onOutputInterrupted);
        eventMap.put(OutputLayerEventType.LENGTH_MISMATCH, OutputLayerListener::onLengthMismatch);
        eventMap.put(OutputLayerEventType.UNCHECKED_CLOSE, OutputLayerListener::onUncheckedClose);
        eventMap.put(OutputLayerEventType.RENDER_EXCEPTION, OutputLayerListener::onRenderException);
        eventMap.put(OutputLayerEventType.OUTPUT_EXCEPTION, OutputLayerListener::onOutputException);
        eventMap.put(OutputLayerEventType.DEVICE_INVALIDATED, OutputLayerListener::onDeviceInvalidated);
        eventMap.put(OutputLayerEventType.REOPEN_FAILED, OutputLayerListener::onReopenFailed);
        eventDispatcher.setEventMap(eventMap);
    }
    
    private OutputLayerEvent getEvent() {
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
     * Opens the audio output with the specified port, format, and buffer size.
     * If the port is null, the default output port will be used.
     * This method also tries to use best matching audio format, if the given format is not supported.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data, defined by {@link AudioMeasure}.
     * @param buffersCount The number of audio ring buffers to be used.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio port, audio format, buffer size, or buffer count are invalid.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, AudioMeasure bufferSize, int buffersCount) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioPortsNotFoundException, AudioBackendException {
        if (isOpened) {
            logger.warn("Audio output layer is already open.");
            return;
        }

        if (!aob.isInitialized()) {
            aob.initialize();
        }

        if (audioFormat == null) throw new IllegalArgumentException("Audio format cannot be null.");
        if (bufferSize == null) throw new IllegalArgumentException("Buffer size cannot be null.");
        if (buffersCount <= 0) throw new IllegalArgumentException("Buffer count must be greater than 0.");

        int bufferSizeInFrames = (int)bufferSize.onFormat(audioFormat).getFrames();
        if (bufferSizeInFrames <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0.");

        AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT).get() : port);
        if (targetPort == null) {
            throw new AudioPortsNotFoundException("No default output port was found.");
        }
        if (port == null) {
            logger.debug("Using default output port: {}.", targetPort);
        }

        this.openedPort = targetPort;
        this.sourceFormat = audioFormat;
        this.openedFormat = findFormat(targetPort, audioFormat);
        if (!openedFormat.equals(sourceFormat)) {
            resamplingFactor = (float) sourceFormat.getSampleRate() / (float) openedFormat.getSampleRate();
            logger.info(
                "Audio format conversion (details in info block). Resampling factor: '{}'.", resamplingFactor
            );
        } else {
            this.resamplingFactor = 1.0f;
        }

        this.renderBufferSize = bufferSizeInFrames;
        this.outputBufferSize = (int)(bufferSizeInFrames / resamplingFactor);
        this.resampledLength = outputBufferSize;
        this.rawLength = resampledLength * openedFormat.getFrameSize();
        this.bufferTimeMicros = AudioUnitsConverter.framesToMicroseconds(
            renderBufferSize,
            (int)(sourceFormat.getSampleRate())
        );

        String renderBufferSizeStr = AudioMeasure.ofFrames(renderBufferSize).onFormat(sourceFormat).getDetailedString();
        String outputBufferSizeStr = AudioMeasure.ofFrames(outputBufferSize).onFormat(openedFormat).getDetailedString();

        StringBuilder outputLog = new StringBuilder("Info:\n");
        outputLog.append("  Port: ").append(targetPort.toString()).append(",\n");
        outputLog.append("  Source format (requested): ").append(sourceFormat.toString()).append(",\n");
        outputLog.append("  Opened Format (output opened with): ").append(openedFormat.toString()).append(",\n");
        outputLog.append("  Resampling factor: ").append(resamplingFactor).append(",\n");
        outputLog.append("  Render buffer size (processing): ").append(renderBufferSizeStr).append(",\n");
        outputLog.append("  Output buffer size: ").append(outputBufferSizeStr).append(",\n");
        outputLog.append("  Buffer time: ").append(FormatUtilities.formatTime(bufferTimeMicros*1000, TIME_FORMAT_PRECISION)).append(",\n");
        outputLog.append("  Buffers count: ").append(buffersCount).append(".\n");
        
        try {
            aob.open(targetPort, openedFormat, rawLength);
        } catch (AudioBackendException e) {
            logger.error("Failed to open audio output. {}", outputLog.toString());
            logger.error("Audio exception stack trace", e);
            throw e;
        }

        long driverLatency = aob.getMicrosecondLatency();
        long buffersLatency = bufferTimeMicros * buffersCount;
        latencyMicros = buffersLatency + driverLatency;

        String driverLatencyStr = (driverLatency > 0 ?
                FormatUtilities.formatTime(driverLatency*1000, TIME_FORMAT_PRECISION) :
                "N/A");
        outputLog.append("Latency info:\n");
        outputLog.append("  Latency (driver-only): ").append(driverLatencyStr).append(",\n");
        outputLog.append("  Latency (computed) [one-buffer, all-buffers]: {")
                 .append(FormatUtilities.formatTime(bufferTimeMicros*1000, TIME_FORMAT_PRECISION)).append(", ")
                 .append(FormatUtilities.formatTime(buffersLatency*1000, TIME_FORMAT_PRECISION)).append("},\n");
        outputLog.append("  Effective latency: ").append(FormatUtilities.formatTime(latencyMicros*1000, TIME_FORMAT_PRECISION)).append(".");
        logger.info("Output layer opened. {}", outputLog.toString());

        this.silenceBuffer = new byte[rawLength];
        Arrays.fill(silenceBuffer, (byte) 0);

        this.buffersCount = buffersCount;
        if (buffersCount > 1) {
            ringBuffers = new byte[buffersCount][rawLength];
        }

        isOpened = true;
        eventDispatcher.dispatch(OutputLayerEventType.OPENED, getEvent());
    }

    private AudioFormat findFormat(AudioPort port, AudioFormat sourceFormat) throws UnsupportedAudioFormatException {
        AudioFormat targetFormat = sourceFormat;

        AtomicReference<AudioFormat> closestFormat = new AtomicReference<>();
        closestFormat.set(null);
        if (!aob.isFormatSupported(port, targetFormat, closestFormat)) {
            logger.debug("Audio format is not supported. Using closest supported format: {}.", closestFormat.get());
            if (closestFormat.get() != null) {
                targetFormat = closestFormat.get();
                logger.debug("Using closest supported format: {}.", targetFormat);
            } else {
                // Using targetPort's mix format
                targetFormat = port.getMixFormat();
                logger.debug("Audio format is not supported. Using target port's mix format: {}.", targetFormat);
                closestFormat.set(null);

                if (!aob.isFormatSupported(port, targetFormat, closestFormat)) {
                    logger.debug("Mix format is not supported. Using closest supported format: {}.", closestFormat.get());
                    if (closestFormat.get() != null) {
                        targetFormat = closestFormat.get();
                        logger.debug("Using closest supported format: {}.", targetFormat);
                    } else {
                        logger.error("Failed to open audio output. Unsupported audio format: {}.", targetFormat);
                        throw new UnsupportedAudioFormatException(
                            "Unsupported audio format. Source format: %s. Used format: %s.".formatted(sourceFormat, targetFormat)
                        );
                    }
                }
            }
        }

        return targetFormat;
    }

    /**
     * Opens the audio output with the specified port, format, and buffer size.
     * If the port is null, the default output port will be used.
     * It creates a double buffered audio output.
     * This method also tries to use best matching audio format, if the given format is not supported.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The size of the buffer for audio data, defined by {@link AudioMeasure}.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio port, audio format, or buffer size are invalid.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, AudioMeasure bufferSize) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioPortsNotFoundException, AudioBackendException {
        this.open(port, audioFormat, bufferSize, AOL_DEFAULT_RING_BUFFERS); 
    }

    /**
     * Opens the audio output with the specified port and format.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio format is null.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioBackendException, AudioPortsNotFoundException {
        this.open(port, audioFormat, AOL_DEFAULT_BUFFER);
    }

    /**
     * Opens the audio output with the specified format, and default output port.
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
     * Checks if the audio output is open.
     * 
     * @return True if the audio output is open, false otherwise.
     */
    public boolean isOpen() {
        return aob.isOpen() && isOpened;
    }

    /** Re-opens the audio output (may be unstable) */
    private void reOpen() throws AudioBackendException, IllegalArgumentException, 
            UnsupportedAudioFormatException, AudioPortsNotFoundException {
        
        if (!isOpen()) {
            throw new IllegalStateException("Backend is not open.");
        }
        
        final AudioPort port = this.openedPort;
        final AudioFormat audioFormat = this.sourceFormat;
        final AudioMeasure bufferSize = AudioMeasure.ofFrames(this.renderBufferSize);
        final int buffersCount = this.buffersCount;
        final boolean wasPlaying = this.isPlaying;

        logger.debug("Re-opening audio output. Port: {}, audio format: {}, buffer size: {}, buffers count: {}",
                port, audioFormat, bufferSize, buffersCount);
        
        try {
            this.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted during audio backend re-opening");
            throw new AudioBackendException("Re-opening interrupted", e);
        } catch (AudioBackendException e) {
            logger.error("Failed to close audio backend during re-opening", e);
            throw e;
        }
        
        logger.debug("Closed audio backend.");
        
        try {
            this.open(port, audioFormat, bufferSize, buffersCount);
            logger.debug("Opened audio backend.");

            if (wasPlaying) {
                this.start();
            }
        } catch (Exception e) {
            logger.error("Failed to re-open audio backend", e);
            this.isOpened = false;
            throw e;
        }
    }

    /**
     * Starts the audio output, processing audio data from the root node.
     * Creates a processing thread and starts it.
     * The thread is set as a daemon thread to not block JVM exit.
     * 
     * @throws AudioBackendException If an error occurs while starting the backend.
     * @throws RuntimeException If the processing thread cannot be started.
     */
    public void start() throws AudioBackendException {
        if (isPlaying) return;
        if (!isOpened) {
            throw new IllegalStateException("Audio output layer is not open.");
        }
        aob.start();
        boolean isOneBuffer = buffersCount == 1;

        String processingThreadName = baseName + (isOneBuffer ? "-ProcessAndOutput" : "-Processing");
        int processingThreadPriority = (isOneBuffer ? 
                Math.max(processingPriority, outputPriority) :
                processingPriority);
        ThreadType processingThreadType = (isOneBuffer ? (
                AOL_PROCESSING_THREAD.threadType == ThreadType.PLATFORM ||
                AOL_OUTPUT_THREAD.threadType == ThreadType.PLATFORM ?
                ThreadType.PLATFORM : ThreadType.VIRTUAL) : AOL_PROCESSING_THREAD.threadType);
        
        processingThread = ThreadUtilities.startThread(
            processingThreadName,
            processingThreadType,
            processingThreadPriority,
            this::process
            // Do not catch exceptions here, they will be caught in process().
            // Critical exceptions can stop the processing thread.
        );
        if (processingThread == null) 
            throw new RuntimeException(
                "Failed to start " + processingThreadName + ". Thread is null."
            );
        if (!isOneBuffer) {
            outputThread = ThreadUtilities.startThread(
                baseName + "-Output",
                AOL_OUTPUT_THREAD.threadType,
                outputPriority,
                this::output
            );
            if (outputThread == null) 
                throw new RuntimeException(
                    "Failed to start " + baseName + "-Output. Thread is null."
                );
        }
        underruns.set(0);
        writeFailures.set(0);
        isPlaying = true;
        logger.debug("Started {}. Processing thread: {}, Output thread: {}.", baseName, processingThread, outputThread);
        eventDispatcher.dispatch(OutputLayerEventType.STARTED, getEvent());
    }

    /**
     * Stops the audio output and interrupts the processing thread.
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
            if (outputThread != null && outputThread.isAlive()) outputThread.join(AOL_OUTPUT_STOP_TIMEOUT);
            if (processingThread != null && processingThread.isAlive()) processingThread.join(AOL_PROCESSING_STOP_TIMEOUT);
        } catch (InterruptedException ex) {
            logger.error("Interrupted while joining output and processing threads, in {}.", baseName, ex);
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
     * Closes the audio output, stopping the processing thread and closing the backend.
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
        if (AOL_ENABLE_SHUTDOWN_HOOK) {
            try {
                boolean result = Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
                if (!result) {
                    logger.warn("Shutdown hook failed to remove (maybe already removed or never registered?).");
                }
            } catch (IllegalStateException ex) {
                logger.warn("Shutdown hook is running.", ex);
            } catch (SecurityException ex) {
                logger.error("Cannot remove shutdown hook due to security restrictions.", ex);
            }
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
        return underruns.get();
    }

    /**
     * Returns the audio format of the audio output.
     * @return The audio format of the audio output.
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
     * Returns the current frame position of the audio output.
     * @return The current frame position.
     */
    public long getFramePosition() {
        return aob.getFramePosition();
    }

    /**
     * Returns the current microsecond position of the audio output.
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
     * Returns the current audio port of the audio output.
     * @return The current audio port.
     */
    public AudioPort getCurrentAudioPort() {
        return aob.getCurrentAudioPort();
    }

    /* Processing and Output */

    private byte[] readAudio() {
        synchronized(bufferLock) {
            if (readIndex == writeIndex) {
                logger.warn("Underrun. Total underruns: {}", underruns);
                eventDispatcher.dispatch(OutputLayerEventType.UNDERRUN, getEvent());
                underruns.incrementAndGet();
                switch (underrunBehavior) {
                    case SILENCE:
                        return silenceBuffer;
                    case REPEAT_LAST:
                        return lastAudioBuffer != null ? lastAudioBuffer : silenceBuffer;
                    default:
                        return silenceBuffer;
                }
            }
            byte[] output = ringBuffers[readIndex.get()];
            lastAudioBuffer = output;
            readIndex.set((readIndex.get() + 1) % buffersCount);
            bufferLock.notifyAll();
            return output;
        }
    }

    private void writeAudio(byte[] data) throws InterruptedException {
        synchronized(bufferLock) {
            while ((writeIndex.get() + 1) % buffersCount == readIndex.get()) {
                bufferLock.wait();
            }
            System.arraycopy(data, 0, ringBuffers[writeIndex.get()], 0, data.length);
            writeIndex.set((writeIndex.get() + 1) % buffersCount);
            bufferLock.notifyAll();
        }
    }

    private static class OutputException extends RuntimeException {
        
        public OutputException(String message) {
            super(message);
        }
    }

    private void output() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long startNs = System.nanoTime();
                byte[] data = readAudio();
                if (aob.write(data, 0, data.length) == -1) {
                    writeFailures.incrementAndGet();
                    if (writeFailures.get() < AOL_MAX_WRITE_ERRORS) {
                        logger.warn("Audio backend write failed {} times.", writeFailures);
                        waitNanos(System.nanoTime() - startNs);
                    } else {
                        logger.error("Audio backend write failed {} times. Aborting.", writeFailures);
                        throw new OutputException("Audio backend write failed " + writeFailures + " times.");
                    }
                } else if (AOL_RESET_WRITE_ERRORS && writeFailures.get() > 0) {
                    writeFailures.set(0);
                }
            } catch (BackendNotOpenException ex) {
                logger.info("Backend is closed.");
                eventDispatcher.dispatch(OutputLayerEventType.UNCHECKED_CLOSE, getEvent());
                return;  
            } catch (DeviceInvalidatedException | DeviceInactiveException ex) {
                logger.error("Device is invalidated or inactive.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.DEVICE_INVALIDATED, getEvent());
                
                Thread reopenThread = new Thread(() -> {
                    logger.info("Trying to reopen device.");
                    try {
                        reOpen();
                    } catch (Exception ex2) {
                        logger.error("Error reopening device.", ex2);
                        eventDispatcher.dispatch(OutputLayerEventType.REOPEN_FAILED, getEvent());
                        throw new RuntimeException("Error reopening device.", ex2);
                    }
                });
                reopenThread.setName("Reopen " + baseName);
                reopenThread.setDaemon(true);
                reopenThread.start();
                return;
            } catch (OutputException | AudioBackendException ex) {
                logger.error("Error writing to audio backend.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.OUTPUT_EXCEPTION, getEvent());
                throw new RuntimeException("Error writing to audio backend.", ex);
            } catch (Exception ex) {
                logger.error("Exception in output thread.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.OUTPUT_EXCEPTION, getEvent());
                // Ignore
            }
        }
    }

    private static class ProcessingException extends RuntimeException {
        
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProcessingException(String message) {
            super(message);
        }
    }

    private void process() {
        float[][] sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
        float[][] resampled = new float[openedFormat.getChannels()][resampledLength];
        byte[] rawBytes = new byte[rawLength];

        int lengthMismatchCounter = 0;
        boolean isOneBuffer = buffersCount == 1;

        // Use shorter buffer to prevent underruns
        long bufferNsWait = Math.min(bufferTimeMicros - 1000, 100) * 1000L;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                long startRenderNs = System.nanoTime();
                AudioNode snapshot = rootNode;
                if (snapshot == null) {
                    writeAudio(silenceBuffer);
                    waitNanos(bufferNsWait);
                    continue;
                }

                try {
                snapshot.render(sampleBuffer, (int)(sourceFormat.getSampleRate()));
                } catch (MixingException ex) {
                    logger.error("Mixing error caught.", ex);
                }
                // if changed the channels or samples length
                if (SamplesValidation.isValidSamples(sampleBuffer) != ValidationResult.VALID || !SamplesValidation.checkLength(sampleBuffer, renderBufferSize)) {
                    logger.error("Length mismatch in {}-Output thread. Expected {} got {}. Counter: {}",
                            baseName, renderBufferSize, sampleBuffer[0].length, lengthMismatchCounter);
                    lengthMismatchCounter++;
                    eventDispatcher.dispatch(OutputLayerEventType.LENGTH_MISMATCH, getEvent());
                    // Initialize buffers again
                    if (lengthMismatchCounter < AOL_MAX_LENGTH_MISMATCHES) {
                        sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
                        long endRenderNs = System.nanoTime();
                        long durationRenderNs = endRenderNs - startRenderNs;
                        waitNanos(bufferNsWait - durationRenderNs);
                        continue;
                    } else {
                        logger.error("Too many length mismatches. Aborting.");
                        throw new LengthMismatchException("Length mismatch. Expected " + renderBufferSize + " got " + sampleBuffer[0].length);
                    }
                }

                if (AOL_RESET_LENGTH_MISMATCHES && lengthMismatchCounter > 0) {
                    lengthMismatchCounter = 0;
                }

                try {
                    resampler.resample(sampleBuffer, resampled, resamplingFactor);
                    SamplesConverter.fromSamples(resampled, rawBytes, openedFormat);
                } catch (IllegalArgumentException ex) {
                    logger.error("Passed wrong arguments to the resamping or conversion methods (internal error?).", ex);
                    throw new ProcessingException("Internal error", ex);
                }

                if (isOneBuffer) {
                    if (aob.write(rawBytes, 0, rawBytes.length) == -1) { // Write directly
                        writeFailures.incrementAndGet();
                        if (writeFailures.get() < AOL_MAX_WRITE_ERRORS) {
                            logger.warn("Audio backend write failed {} times.", writeFailures);
                            waitNanos(bufferNsWait - startRenderNs);
                        } else {
                            logger.error("Audio backend write failed {} times. Aborting.", writeFailures);
                            throw new ProcessingException("Audio backend write failed " + writeFailures + " times.");
                        }
                    } else if (AOL_RESET_WRITE_ERRORS && writeFailures.get() > 0) {
                        writeFailures.set(0);
                    }
                } else {
                    writeAudio(rawBytes);

                    long endRenderNs = System.nanoTime();
                    long durationRenderNs = endRenderNs - startRenderNs;
                    waitNanos(bufferNsWait - durationRenderNs);
                }
            } catch (BackendNotOpenException ex) {
                logger.info("Backend is closed.");
                eventDispatcher.dispatch(OutputLayerEventType.UNCHECKED_CLOSE, getEvent());
                return;  
            } catch (DeviceInvalidatedException | DeviceInactiveException ex) {
                logger.error("Device is invalidated or inactive.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                
                Thread reopenThread = new Thread(() -> {
                    logger.info("Trying to reopen device.");
                    try {
                        reOpen();
                    } catch (Exception ex2) {
                        logger.error("Error reopening device.", ex2);
                        eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                        throw new RuntimeException("Error reopening device.", ex2);
                    }
                });
                reopenThread.setName("Reopen " + baseName);
                reopenThread.setDaemon(true);
                reopenThread.start();
                return;
            } catch (AudioBackendException ex) {
                logger.error("Error writing to audio backend.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw new RuntimeException("Error writing to audio backend.", ex);
            } catch (LengthMismatchException ex) {
                // Already logged
                throw new RuntimeException(ex);
            } catch (InterruptedException ex) {
                logger.debug("Thread interrupted.");
                eventDispatcher.dispatch(OutputLayerEventType.PROCESSING_INTERRUPTED, getEvent());
                Thread.currentThread().interrupt();  
            } catch (ProcessingException ex) { 
                logger.error("Processing error.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw ex;
            } catch (Exception ex) {
                logger.error("Exception in processing thread.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                // Ignore
            }
        }
    }

    private void waitNanos(long nanos) {
        if (nanos <= 0) return;
        final long deadline = System.nanoTime() + nanos;
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
