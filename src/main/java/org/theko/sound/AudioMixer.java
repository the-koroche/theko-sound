package org.theko.sound;

import java.lang.ref.Cleaner;
import java.time.InstantSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.NonFixedSizeEffect;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;
import org.theko.sound.monitor.GlobalPerformanceMonitor;
import org.theko.sound.util.InstanceCounter;
import org.theko.sound.util.ThreadsFactory;
import org.theko.sound.util.ThreadsFactory.ThreadType;

/**
 * The {@code AudioMixer} class provides real-time audio mixing capabilities, supporting multiple input and output data lines,
 * audio effects processing, and gain/pan controls. It can operate in two modes: THREAD (continuous background processing)
 * and EVENT (processing triggered by input events). The mixer collects audio samples from all inputs, applies pre-gain,
 * mixes them, processes through a chain of real-time audio effects, applies post-gain and pan controls, and outputs the
 * mixed audio to all registered outputs.
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports multiple input and output {@link DataLine} objects.</li>
 *   <li>Applies a chain of real-time {@link AudioEffect} instances to the mixed audio.</li>
 *   <li>Provides pre-gain, post-gain, and pan controls for flexible audio manipulation.</li>
 *   <li>Operates in either THREAD or EVENT mode for flexible integration.</li>
 *   <li>Manages resources and background threads automatically, supporting {@link AutoCloseable} for safe shutdown.</li>
 *   <li>Tracks instance count for monitoring and debugging.</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 *     AudioMixer mixer = new AudioMixer(AudioMixer.Mode.THREAD, audioFormat);
 *     mixer.addInput(inputLine);
 *     mixer.addOutput(outputLine);
 *     mixer.addEffect(new ReverbEffect());
 *     // ...
 *     mixer.close(); // Properly shuts down the mixer and releases resources
 * </pre>
 *
 * @see DataLine
 * @see AudioEffect
 * @see FloatControl
 * @see AutoCloseable
 */
public class AudioMixer implements AudioObject, InstanceCounter, Controllable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AudioMixer.class);

    /** Defines the two available modes for audio mixing. */
    public enum Mode {
        THREAD, EVENT
    }

    protected final Mode mixerMode;
    protected final List<DataLine> inputs;  // List of input data lines (audio sources)
    protected final List<DataLine> outputs; // List of output data lines (audio sinks)
    protected final List<AudioEffect> effects; // List of audio effects to apply
    private transient Thread mixerThread; // Thread used for audio processing in THREAD mode

    protected AudioFormat processAudioFormat; // The format to process the audio data

    private final Map<DataLine, DataLineAdapter> inputListeners = new ConcurrentHashMap<>(); // Listeners for input data lines

    protected final FloatControl preGainControl;  // Pre-gain control
    protected final FloatControl postGainControl; // Post-gain control
    protected final FloatControl panControl; // Pan control for stereo balance

    private static int mixerInstances;
    private final int thisMixerInstance = ++mixerInstances;

    private static transient final Cleaner cleaner = ThreadsFactory.createCleaner(); // Virtual thread cleaner for resource management

    /**
     * Constructs an AudioMixer with the specified mode and audio format.
     * 
     * @param mixerMode The mode to use for processing audio (THREAD or EVENT).
     * @param processAudioFormat The audio format to process.
     */
    public AudioMixer(Mode mixerMode, AudioFormat processAudioFormat) {
        this.mixerMode = mixerMode;
        this.processAudioFormat = processAudioFormat;
        inputs = new CopyOnWriteArrayList<>();
        outputs = new CopyOnWriteArrayList<>();
        effects = new CopyOnWriteArrayList<>();

        mixerThread = (mixerMode == Mode.THREAD) ? ThreadsFactory.createThread(ThreadType.MIXER, this::processLoop, "Mixing Thread-" + thisMixerInstance) : null;
        if (mixerThread != null) {
            mixerThread.start();
            logger.debug("Mixer thread started.");
        }

        this.preGainControl = new FloatControl("Pre-Gain", 0.0f, 1.0f, 1.0f);
        this.postGainControl = new FloatControl("Post-Gain", 0.0f, 1.0f, 1.0f);
        this.panControl = new FloatControl("Pan", -1.0f, 1.0f, 0.0f);
        logger.debug("Controls initialized.");

        // Register a cleaner to shut down the mixer when it's no longer needed
        cleaner.register(this, this::shutdown);
        logger.debug("Audio mixer initialized.");
    }

    /**
     * Adds an input data line to the mixer. In EVENT mode, a listener will be added to process the data.
     *
     * @param input The data line to add.
     * @throws UnsupportedAudioFormatException If the audio format is not supported.
     */
    public void addInput(DataLine input) throws UnsupportedAudioFormatException {
        inputs.add(input);
        if (mixerMode == Mode.EVENT) {
            DataLineAdapter adapter = new DataLineAdapter() {
                public void onSend(DataLineEvent e) {
                    try {
                        process();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new MixingException(ex);
                    }
                }
            };
            input.addDataLineListener(adapter);
            logger.debug("Input listener attached: " + input);
            inputListeners.put(input, adapter);
        }
        logger.debug("Input: " + input + " added.");
    }

    /**
     * Adds an output data line to the mixer.
     * 
     * @param output The output data line to add.
     */
    public void addOutput(DataLine output) {
        outputs.add(output);
        logger.debug("Output: " + output + " added.");
    }

    /**
     * Adds an audio effect to the mixer. Only real-time effects are supported.
     *
     * @param effect The audio effect to add.
     * @throws UnsupportedAudioEffectException If the effect type is not supported.
     */
    public void addEffect(AudioEffect effect) throws UnsupportedAudioEffectException {
        if (effect.getType() == AudioEffect.Type.OFFLINE_PROCESSING) {
            logger.warn("Only AudioEffect.Type.REALTIME is supported to use in mixer.");
            throw new UnsupportedAudioEffectException("Only AudioEffect.Type.REALTIME is supported to use in mixer.");
        }
        if (effect.getClass().isAnnotationPresent(NonFixedSizeEffect.class)) {
            logger.warn(String.format(
                "Effect '%s' has variable output length.",
                effect.getClass().getSimpleName()
            ));
        }
        effects.add(effect);
        logger.debug("Effect: " + effect.getClass().getSimpleName() + " added.");
    }

    /**
     * Removes an input data line from the mixer.
     * 
     * @param input The input data line to remove.
     */
    public void removeInput(DataLine input) {
        inputs.remove(input);
        if (mixerMode == Mode.EVENT) {
            DataLineAdapter adapter = inputListeners.remove(input);
            if (adapter != null) {
                input.removeDataLineListener(adapter);
                logger.debug("Input listener detached: " + input);
            }
        }
        logger.debug("Input: " + input + " removed.");
    }

    /**
     * Removes an output data line from the mixer.
     * 
     * @param output The output data line to remove.
     */
    public void removeOutput(DataLine output) {
        outputs.remove(output);
        logger.debug("Output: " + output + " removed.");
    }

    /**
     * Removes an audio effect from the mixer.
     * 
     * @param effect The audio effect to remove.
     */
    public void removeEffect(AudioEffect effect) {
        effects.remove(effect);
        logger.debug("Effect: " + effect + " removed.");
    }

    public List<DataLine> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    public List<DataLine> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public List<AudioEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /**
     * Gets the pre-gain control for adjusting the gain before processing.
     * 
     * @return The pre-gain control.
     */
    public FloatControl getPreGainControl() {
        return preGainControl;
    }

    /**
     * Gets the post-gain control for adjusting the gain after processing.
     * 
     * @return The post-gain control.
     */
    public FloatControl getPostGainControl() {
        return postGainControl;
    }

    /**
     * Gets the pan control for adjusting the stereo balance.
     * 
     * @return The pan control.
     */
    public FloatControl getPanControl() {
        return panControl;
    }

    @Override
    public List<AudioControl> getAllControls() {
        List<AudioControl> controls = new CopyOnWriteArrayList<>();
        controls.add(preGainControl);
        controls.add(postGainControl);
        controls.add(panControl);
        return controls;
    }

    /**
     * Main loop for processing audio when operating in THREAD mode.
     * This method keeps running and processes the audio when inputs are available.
     */
    private void processLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (!inputs.isEmpty()) {
                    process();
                } else {
                    Thread.yield(); // Yield the thread if no inputs are available
                }
            }
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Processes the audio data from inputs, applies effects, adjusts gain and pan, 
     * and sends the mixed audio to the outputs.
     * 
     * @throws InterruptedException If the thread is interrupted during processing.
     */
    private void process() throws InterruptedException {
        if (inputs.isEmpty() || outputs.isEmpty()) return;

        // Collect samples from all input data lines
        float[][][] samples = new float[inputs.size()][][];
        int maxLength = 0;

        for (int i = 0; i < inputs.size(); i++) {
            DataLine dl = inputs.get(i);
            if (dl != null) {
                samples[i] = dl.receiveWithTimeout(50, TimeUnit.MILLISECONDS);
                if (samples[i] != null) {
                    for (float[] channel : samples[i]) {
                        maxLength = Math.max(maxLength, channel.length);
                    }
                }
            }
        }

        long processNsStart = System.nanoTime();

        if (maxLength == 0) return; // No data to process

        int channels = processAudioFormat.getChannels();
        float[][] mixed = new float[channels][maxLength];

        // Mixing the samples with pre-gain applied
        for (int k = 0; k < samples.length; k++) {
            if (samples[k] == null) continue;
            for (int ch = 0; ch < Math.min(channels, samples[k].length); ch++) {
                for (int j = 0; j < samples[k][ch].length; j++) {
                    mixed[ch][j] += samples[k][ch][j] * preGainControl.getValue();
                }
            }
        }

        // Apply audio effects
        if (!effects.isEmpty()) {
            mixed = applyEffects(mixed);
        }

        // Calculate left and right volume based on pan control
        float leftVol = 1.0f;
        float rightVol = 1.0f;
        if (panControl.getValue() < 0) {
            leftVol = 1.0f;
            rightVol = 1.0f + panControl.getValue(); // Decrease right volume when pan is to the left
        } else if (panControl.getValue() > 0) {
            leftVol = 1.0f - panControl.getValue(); // Decrease left volume when pan is to the right
            rightVol = 1.0f;
        }

        long processNsEnd = System.nanoTime();

        // Normalize and send the mixed audio data to outputs
        float gain = postGainControl.getValue();
        for (DataLine output : outputs) {
            if (output != null) {
                for (int ch = 0; ch < mixed.length; ch++) {
                    for (int j = 0; j < mixed[ch].length; j++) {
                        float targetGain = getGainForChannel(ch, gain, leftVol, rightVol);
                        mixed[ch][j] = Math.max(-1.0f, Math.min(1.0f, mixed[ch][j] * targetGain));
                    }
                }
                output.send(mixed);
            }
        }

        GlobalPerformanceMonitor.addTimeRecord(this, processNsStart, processNsEnd);
    }

    private float getGainForChannel(int channel, float mainGain, float leftVol, float rightVol) {
        float channelGain = (channel == 0 ? leftVol : rightVol);
        return mainGain * channelGain;
    }

    // Used in method applyEffects
    private int samplesCount = -1;

    /**
     * Applies all added effects to the audio sample sequentially.
     * 
     * @param samples The audio samples to process.
     * @return The processed audio sample.
     */
    private float[][] applyEffects(float[][] samples) {
        double totalDurationSec = samples[0].length / (double)processAudioFormat.getSampleRate();
        if (samplesCount != samples[0].length) {
            samplesCount = samples[0].length;
            logger.debug(String.format(
                "Samples duration: %.4f s.", totalDurationSec
            ));
        }
    
        for (AudioEffect effect : effects) {
            if (effect == null) continue;
            String name = effect.getClass().getSimpleName();
    
            long startNs = System.nanoTime();
            samples = effect.callProcess(samples);
            long elapsedNs = System.nanoTime() - startNs;
    
            double elapsedSec = elapsedNs / 1_000_000_000.0;

            logger.trace(String.format(
                "Effect '%s' took %.2f s (input duration %.2f s)", name, elapsedSec, totalDurationSec
            ));
            if (elapsedSec > totalDurationSec * 0.1) {
                logger.warn(String.format(
                    "Effect '%s' took %.2f s (input duration %.2f s)", name, elapsedSec, totalDurationSec
                ));
            }
        }
        return samples;
    }

    public void performProcess() {
        try {
            process();
        } catch (InterruptedException e) {
            throw new MixingException(e);
        }
    }

    /**
     * Shuts down the mixer and stops the mixer thread if running.
     * Cleans up any resources used by the mixer.
     */
    public void shutdown() {
        if (mixerThread != null) {
            mixerThread.interrupt();
            try {
                mixerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Entry<DataLine, DataLineAdapter> input : inputListeners.entrySet()) {
            input.getKey().removeDataLineListener(input.getValue());
        }
    }

    @Override
    public void onLoad() {
        if (mixerMode == Mode.THREAD) {
            if (mixerThread != null && !mixerThread.isAlive()) {
                mixerThread.start();
                logger.debug("Mixer thread started.");
            } else if (mixerThread == null) {
                logger.warn("Mixer thread is null, recreating it.");
                mixerThread = ThreadsFactory.createThread(ThreadType.MIXER, this::processLoop, "Mixing Thread-" + thisMixerInstance);
                mixerThread.start();
            }
        }
    }

    /**
     * Closes the mixer and shuts down all resources.
     */
    @Override
    public void close() {
        shutdown();
    }

    @Override
    public int getInstanceCount() {
        return mixerInstances;
    }

    @Override
    public int getCurrentInstance() {
        return thisMixerInstance;
    }
}
