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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.EventDispatcher;
import org.theko.events.ListenersManager;
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
import org.theko.sound.resampling.AudioResampler;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.samples.SamplesValidation.ValidationResult;
import org.theko.sound.utility.FormatUtilities;
import org.theko.sound.utility.ThreadUtilities;
import org.theko.sound.utility.TimeUtilities;

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
    
    private boolean isOpened = false;
    private final AtomicBoolean reopenInProgress = new AtomicBoolean(false);

    /* Processing and output threads */
    private Thread playbackThread;
    private int processingPriority = AOL_PLAYBACK_THREAD.priority;

    /* Audio output backend and buffers */
    private final AudioOutputBackend aob;
    private AudioPort openedPort;
    private AtomicInteger writeFailures = new AtomicInteger(0);

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
    private boolean isPlaybackInterrupted;
    private float resamplingFactor = 1.0f;
    private AudioResampler resampler;

    /* Shutdown hook */
    private final Runnable shutdownHook;
    private final Thread shutdownHookThread;

    /* Event dispatcher */
    private final EventDispatcher<OutputLayerEvent, OutputLayerListener, OutputLayerEventType> eventDispatcher;

    /**
     * Constructs an {@code AudioOutputLayer} with the specified audio output backend.
     * 
     * @param aobInfo The {@link AudioBackendInfo} of the audio output backend to use.
     * @throws IllegalArgumentException If the audio output backend info is null.
     * @throws AudioBackendCreationException If an error occurs while creating the audio output backend.
     * @throws AudioBackendNotFoundException If the specified audio output backend is not found.
     */
    public AudioOutputLayer(AudioBackendInfo aobInfo) throws AudioBackendCreationException, AudioBackendNotFoundException, IllegalArgumentException {
        if (aobInfo == null) throw new IllegalArgumentException("AudioOutputBackend cannot be null.");
        this.aob = AudioBackends.getOutputBackend(aobInfo);
        logger.debug("Created AudioOutputLayer with backend: {}.", aob.getClass().getSimpleName());

        shutdownHook = () -> {
            try {
                stop();
            } catch (Exception e) {
                logger.error("Failed to stop.", e);
            }
            if (aob != null) {
                aob.close();
                aob.shutdown();
            }
            logger.debug("Shutted down.");
        };
        shutdownHookThread = new Thread(shutdownHook, "AOL-ShutdownHook");
        aob.initialize();
        if (AOL_ENABLE_SHUTDOWN_HOOK) {
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        }

        resampler = new AudioResampler(AOL_RESAMPLER.resampleMethod, AOL_RESAMPLER.quality);

        eventDispatcher = new EventDispatcher<>();
        var eventMap = eventDispatcher.createEventMap();
        eventMap.put(OutputLayerEventType.OPENED, OutputLayerListener::onOpened);
        eventMap.put(OutputLayerEventType.REOPENED, OutputLayerListener::onReopened);
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
        return new OutputLayerEvent(this, outBufferSize);
    }
    
    /**
     * Constructs an {@code AudioOutputLayer} with the default audio output backend for the platform.
     * 
     * @throws IllegalArgumentException If the default audio output backend is null.
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
     * @param reopen If true, the audio output will be reopened if it is already open.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio port, audio format, buffer size, or buffer count are invalid.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, AudioMeasure bufferSize, boolean reopen) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioPortsNotFoundException, AudioBackendException {
        if (isOpened && !reopen) {
            logger.warn("Audio output layer is already open.");
            return;
        }

        if (!aob.isInitialized())
            aob.initialize();

        final boolean wasPlaying = this.isPlaying;

        if (isOpened) {
            if (reopen)
                logger.debug("Reopen flag is set. Closing previous backend...");
            try {
                close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while closing previous backend", e);
                throw new AudioBackendException("Interrupted while closing previous backend", e);
            }
        }

        if (audioFormat == null) throw new IllegalArgumentException("Audio format cannot be null.");
        if (bufferSize == null) throw new IllegalArgumentException("Buffer size cannot be null.");

        int bufferSizeInFrames = (int)bufferSize.onFormat(audioFormat).getFrames();
        if (bufferSizeInFrames <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0.");

        AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT).get() : port);
        if (targetPort == null) {
            throw new AudioPortsNotFoundException("No default output port was found.");
        }
        if (port == null) {
            logger.debug("Using default output port: {}.", targetPort);
        }

        this.sourceFormat = audioFormat;
        AudioFormat selectedFormat = findFormat(targetPort, audioFormat);
        if (!selectedFormat.equals(sourceFormat)) {
            resamplingFactor = (float) sourceFormat.getSampleRate() / (float) selectedFormat.getSampleRate();
            logger.info(
                "Audio format conversion (details in info block). Resampling factor: '{}'.", resamplingFactor
            );
        } else {
            this.resamplingFactor = 1.0f;
        }

        calculateLengths(sourceFormat, selectedFormat, bufferSizeInFrames);

        String renderBufferSizeStr; 
        String outputBufferSizeStr;
        
        renderBufferSizeStr = AudioMeasure.ofFrames(renderBufferSize).onFormat(sourceFormat).getDetailedString();
        outputBufferSizeStr = AudioMeasure.ofFrames(outputBufferSize).onFormat(selectedFormat).getDetailedString();

        StringBuilder outputLog = new StringBuilder("Info:\n");
        outputLog.append("  Port: ").append(targetPort.toString()).append(",\n");
        outputLog.append("  Source format (requested): ").append(sourceFormat.toString()).append(",\n");
        outputLog.append("  Selected Format: ").append(selectedFormat.toString()).append(",\n");
        outputLog.append("  Resampling factor: ").append(resamplingFactor).append(",\n");
        outputLog.append("  Render buffer size (playback): ").append(renderBufferSizeStr).append(",\n");
        outputLog.append("  Output buffer size: ").append(outputBufferSizeStr).append(",\n");
        outputLog.append("  Buffer time: ").append(FormatUtilities.formatTime(bufferTimeMicros*1000, TIME_FORMAT_PRECISION)).append(".\n");
        
        try {
            this.openedFormat = aob.open(targetPort, selectedFormat, rawLength);
            if (openedFormat == null) {
                throw new AudioBackendException("Failed to open audio output. Port: " + targetPort + ", format: " + openedFormat);
            }
        } catch (Exception e) {
            logger.error("Failed to open audio output. {}", outputLog.toString());
            logger.error("Exception stack trace", e);
            throw e;
        }

        if (!openedFormat.equals(selectedFormat)) {
            logger.warn("Selected format ({}) does not match opened format ({}).", selectedFormat, openedFormat);
            logger.info("Re-calculating lengths.");
            calculateLengths(sourceFormat, selectedFormat, bufferSizeInFrames);
        }
        
        renderBufferSizeStr = AudioMeasure.ofFrames(renderBufferSize).onFormat(sourceFormat).getDetailedString();
        outputBufferSizeStr = AudioMeasure.ofFrames(outputBufferSize).onFormat(selectedFormat).getDetailedString();

        outputLog.append("  Opened Format: ").append(openedFormat.toString()).append(",\n");

        long driverLatency = aob.getMicrosecondLatency();
        latencyMicros = bufferTimeMicros + driverLatency;

        String driverLatencyStr = (driverLatency > 0 ?
                FormatUtilities.formatTime(driverLatency*1000, TIME_FORMAT_PRECISION) :
                "N/A");
        outputLog.append("Latency info:\n");
        outputLog.append("  Latency (driver-only): ").append(driverLatencyStr).append(",\n");
        outputLog.append("  Latency (buffer): ").append(FormatUtilities.formatTime(bufferTimeMicros*1000, TIME_FORMAT_PRECISION)).append(", ");
        outputLog.append("  Effective latency: ").append(FormatUtilities.formatTime(latencyMicros*1000, TIME_FORMAT_PRECISION)).append(".");
        logger.info("Output layer opened. {}", outputLog.toString());

        isOpened = true;
        eventDispatcher.dispatch(OutputLayerEventType.OPENED, getEvent());

        if (reopen) {
            eventDispatcher.dispatch(OutputLayerEventType.REOPENED, getEvent());
            if (wasPlaying) {
                logger.debug("Starting playback after re-opening.");
                start();
            }
        }
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
                // Using mix format from target port
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

    private void calculateLengths(AudioFormat sourceFormat, AudioFormat targetFormat, int bufferSizeInFrames) {
        double resamplingFactor = (double)sourceFormat.getSampleRate() / (double)targetFormat.getSampleRate();
        this.renderBufferSize = bufferSizeInFrames;
        this.outputBufferSize = (int)(bufferSizeInFrames / resamplingFactor);
        this.resampledLength = outputBufferSize;
        this.rawLength = resampledLength * targetFormat.getFrameSize();
        this.bufferTimeMicros = AudioUnitsConverter.framesToMicroseconds(
            renderBufferSize,
            (int)(sourceFormat.getSampleRate())
        );
    }

    /**
     * Opens the audio output with the specified port, format, and buffer size.
     * 
     * @param port The {@link AudioPort} to be used.
     * @param audioFormat The {@link AudioFormat} for audio data.
     * @param bufferSize The {@link AudioMeasure} representing the buffer size.
     * @throws UnsupportedAudioFormatException If the specified audio format is not supported.
     * @throws IllegalArgumentException If the audio format is null.
     * @throws AudioBackendException If an error occurs while opening the backend.
     * @throws AudioPortsNotFoundException If no compatible audio ports are found for the default output.
     */
    public void open(AudioPort port, AudioFormat audioFormat, AudioMeasure bufferSize) throws UnsupportedAudioFormatException, IllegalArgumentException, AudioBackendException, AudioPortsNotFoundException {
        this.open(port, audioFormat, bufferSize, false /* don't reopen */);
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
        this.open(null /* default output port */, audioFormat);
    }

    public void reopen() throws UnsupportedAudioFormatException, AudioBackendException, AudioPortsNotFoundException {
        // Using sourceFormat to get correct resampling factor
        this.open(openedPort, sourceFormat, AudioMeasure.ofFrames(outputBufferSize), true /* reopen */);
    }

    /**
     * Checks if the audio output is open.
     * 
     * @return True if the audio output is open, false otherwise.
     */
    public boolean isOpen() {
        return aob.isOpen() && isOpened;
    }

    /**
     * Starts the audio output, processing audio data from the root node.
     * Creates a playback thread and starts it.
     * The thread is set as a daemon thread to not block JVM exit.
     * 
     * @throws AudioBackendException If an error occurs while starting the backend.
     * @throws RuntimeException If the playback thread cannot be started.
     */
    public void start() throws AudioBackendException {
        if (isPlaying) return;
        if (!isOpened) {
            throw new BackendNotOpenException("Audio output layer is not open.");
        }
        aob.start();
        
        playbackThread = ThreadUtilities.startThread(
            "AudioOutputLayer-Playback",
            AOL_PLAYBACK_THREAD.threadType,
            processingPriority,
            this::playback
            // Do not catch exceptions here, they will be caught in playback().
            // Critical exceptions can stop the playback thread.
        );
        if (playbackThread == null) 
            throw new RuntimeException(
                "Failed to start AudioOutputLayer-Playback. Thread is null."
            );
        writeFailures.set(0);
        isPlaying = true;
        logger.debug("Started AudioOutputLayer. Playback thread: {}.", playbackThread);
        eventDispatcher.dispatch(OutputLayerEventType.STARTED, getEvent());
    }

    /**
     * Stops the audio output and interrupts the playback thread.
     * @throws AudioBackendException If an error occurs while stopping the backend.
     * @throws InterruptedException If the threads join operation is interrupted.
     */
    public void stop() throws AudioBackendException, InterruptedException {
        if (!isPlaying) return;
        isPlaybackInterrupted = true;
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.interrupt();
        }
        try {
            if (playbackThread != null && playbackThread.isAlive()) {
                logger.debug("Waiting for playback thread to stop.");
                playbackThread.join(AOL_PLAYBACK_STOP_TIMEOUT);
            }
        } catch (InterruptedException ex) {
            logger.error("Interrupted while joining output thread.", ex);
            throw ex;
        }
        if (playbackThread != null && playbackThread.isAlive()) {
            logger.warn("Cannot close output thread. Stopping output backend.");
        }
        aob.stop();
        isPlaying = false;
        isPlaybackInterrupted = false;
        logger.debug("Stopped.");
        eventDispatcher.dispatch(OutputLayerEventType.STOPPED, getEvent());
    }

    /**
     * Closes the audio output, stopping the playback thread and closing the backend.
     * Stop can take some time, so it is recommended to call it in a separate thread.
     * 
     * @throws InterruptedException If the threads join operation is interrupted while stopping.
     * @throws AudioBackendException If an error occurs while closing the backend
     */
    @Override
    public void close() throws AudioBackendException, InterruptedException {
        if (!isOpened) {
            logger.info("Audio output layer is already closed.");
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
        logger.debug("Closed");
        eventDispatcher.dispatch(OutputLayerEventType.CLOSED, getEvent());
    }

    /**
     * Flushes the audio output buffer, discarding any buffered data.
     * @throws AudioBackendException If an error occurs while flushing the buffer.
     */
    public void flush() throws AudioBackendException {
        aob.flush();
        logger.trace("Flushed.");
        eventDispatcher.dispatch(OutputLayerEventType.FLUSHED, getEvent());
    }

    /**
     * Drains the audio output buffer, ensuring all buffered data is processed.
     * @throws AudioBackendException If an error occurs while draining the buffer.
     */
    public void drain() throws AudioBackendException {
        aob.drain();
        logger.trace("Drained.");
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
     * Returns the priority of the playback thread.
     * @return The priority of the playback thread.
     */
    public int getThreadPriority() {
        return processingPriority;
    }

    /**
     * Sets the priority of the playback thread.
     * @param priority The new priority of the thread.
     */
    public void setThreadPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalArgumentException("Priority must be between " + Thread.MIN_PRIORITY + " and " + Thread.MAX_PRIORITY + ".");
        this.processingPriority = priority;
        if (playbackThread != null && playbackThread.isAlive()) {
            playbackThread.setPriority(priority);
        }
    }

    /**
     * Sets the root node for audio playback.
     * <p>If the root node is null, the output layer will wait buffer-time, until a new root node is set.
     * @param rootNode The new root node for audio playback (can be null).
     */
    public void setRootNode(AudioNode rootNode) {
        if (rootNode == null) {
            logger.info("Root node is null.");
        }
        this.rootNode = rootNode;
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

    /**
     * Returns a listeners manager, to add/remove listeners for this output layer events.
     * The listeners manager returned by this method can be used to register and unregister
     * listeners for output layer events, such as opened, closed, started, stopped, flushed, drained,
     * underrun, overrun, length mismatch, unchecked close, output interrupted, and processing
     * interrupted events.
     * 
     * @return The listeners manager for this output layer events.
     */
    public ListenersManager<OutputLayerEvent, OutputLayerListener, OutputLayerEventType> getListenersManager() {
        return eventDispatcher.getListenersManager();
    }

    private static class ProcessingException extends RuntimeException {
        
        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProcessingException(String message) {
            super(message);
        }
    }

    private void playback() {
        float[][] sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
        float[][] resampled = new float[openedFormat.getChannels()][resampledLength];
        byte[] rawBytes = new byte[rawLength];

        int lengthMismatchCounter = 0;

        long bufferNsWait = Math.min(bufferTimeMicros - 1000, 100) * 1000L;

        while (!Thread.currentThread().isInterrupted() && !isPlaybackInterrupted) {
            try {
                long startRenderNs = System.nanoTime();
                AudioNode snapshot = rootNode;
                if (snapshot == null) {
                    TimeUtilities.waitNanosPrecise(bufferNsWait);
                    continue;
                }

                try {
                    snapshot.render(sampleBuffer, (int)(sourceFormat.getSampleRate()));
                } catch (MixingException ex) {
                    logger.error("Mixing error caught.", ex);
                }
                // if changed the channels or samples length
                if (SamplesValidation.isValidSamples(sampleBuffer) != ValidationResult.VALID || !SamplesValidation.checkLength(sampleBuffer, renderBufferSize)) {
                    logger.error("Length mismatch in output thread. Expected {} got {}. Counter: {}",
                            renderBufferSize, sampleBuffer[0].length, lengthMismatchCounter);
                    lengthMismatchCounter++;
                    eventDispatcher.dispatch(OutputLayerEventType.LENGTH_MISMATCH, getEvent());
                    // Initialize buffers again
                    if (lengthMismatchCounter < AOL_MAX_LENGTH_MISMATCHES) {
                        sampleBuffer = new float[openedFormat.getChannels()][renderBufferSize];
                        long endRenderNs = System.nanoTime();
                        long durationRenderNs = endRenderNs - startRenderNs;
                        TimeUtilities.waitNanosPrecise(bufferNsWait - durationRenderNs);
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

                if (aob.write(rawBytes, 0, rawBytes.length) == -1) {
                    writeFailures.incrementAndGet();
                    if (writeFailures.get() < AOL_MAX_WRITE_ERRORS) {
                        logger.warn("Audio backend write failed {} times.", writeFailures);
                        TimeUtilities.waitNanosPrecise(bufferNsWait - startRenderNs);
                    } else {
                        logger.error("Audio backend write failed {} times. Aborting.", writeFailures);
                        throw new ProcessingException("Audio backend write failed " + writeFailures + " times.");
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
                
                if (reopenInProgress.compareAndSet(false, true)) {
                    Thread reopenThread = new Thread(() -> {
                        try {
                            logger.warn("Trying to reopen audio device...");
                            reopen();
                        } catch (Exception ex2) {
                            logger.error("Error reopening device.", ex2);
                            eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                        } finally {
                            reopenInProgress.set(false);
                        }
                    });
                    reopenThread.setName("AudioOutputLayer-ReopenThread");
                    reopenThread.setDaemon(true);
                    reopenThread.start();
                } else {
                    logger.warn("Reopen already in progress, skipping...");
                }
                return;
            } catch (AudioBackendException ex) {
                logger.error("Error writing to audio backend.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw new RuntimeException("Error writing to audio backend.", ex);
            } catch (LengthMismatchException ex) {
                // Already logged
                throw new RuntimeException(ex);
            } catch (ProcessingException ex) { 
                logger.error("Processing error.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                throw ex;
            } catch (InterruptedException ex) {
                logger.debug("Playback thread interrupted.");
                return;
            } catch (Exception ex) {
                logger.error("Exception in playback thread.", ex);
                eventDispatcher.dispatch(OutputLayerEventType.RENDER_EXCEPTION, getEvent());
                // Ignore
            }
        }
    }
}
