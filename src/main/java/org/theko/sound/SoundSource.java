/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.EventDispatcher;
import org.theko.events.ListenersManageable;
import org.theko.events.ListenersManager;
import org.theko.sound.codecs.AudioCodec;
import org.theko.sound.codecs.AudioCodecException;
import org.theko.sound.codecs.AudioCodecInfo;
import org.theko.sound.codecs.AudioCodecNotFoundException;
import org.theko.sound.codecs.AudioCodecs;
import org.theko.sound.codecs.AudioDecodeResult;
import org.theko.sound.codecs.AudioMetadata;
import org.theko.sound.controls.Controllable;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.effects.AudioEffect;
import org.theko.sound.effects.AudioEffectBuilder;
import org.theko.sound.effects.IncompatibleEffectTypeException;
import org.theko.sound.effects.MultipleVaryingSizeEffectsException;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.effects.VaryingSizeEffect;
import org.theko.sound.events.AudioControlEventType;
import org.theko.sound.events.SoundSourceEvent;
import org.theko.sound.events.SoundSourceEventType;
import org.theko.sound.events.SoundSourceListener;
import org.theko.sound.samples.SamplesValidation;
import org.theko.sound.util.AudioBufferUtilities;

/**
 * The {@code SoundSource} class represents an audio source that can be controlled
 * and played back. It implements the {@link AudioNode} and {@link Controllable} interfaces,
 * allowing it to render audio samples and manage audio controls.
 *
 * <p>This class provides functionalities such as opening an audio file, rendering audio samples,
 * and controlling playback. It supports looping and can reset playback state as needed.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Opening audio files and decoding them into samples data.</li>
 *   <li>Rendering audio samples through the {@link Playback} inner class.</li>
 *   <li>Managing playback state with controls for speed, gain, and pan.</li>
 *   <li>Support for looping and resetting playback.</li>
 * </ul>
 * 
 * <p>If the sound source is not opened or initialized, it will throw {@link IllegalStateException}.
 * To initialize the sound source (create an inner mixer), use the {@link #initialize()} method.
 *
 * <p>Usage example:
 * <pre>{@code
 * SoundSource soundSource = new SoundSource();
 * soundSource.open(new File("path/to/audio/file"));
 * soundSource.start();
 * }</pre>
 *
 * @see AudioNode
 * @see Controllable
 * @see AudioMixer
 * @see ResamplerEffect
 *
 * @since 0.2.0-beta
 * @author Theko
 */
public class SoundSource implements AudioNode, Controllable, AutoCloseable,
        ListenersManageable<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> {

    private static final Logger logger = LoggerFactory.getLogger(SoundSource.class);
    private final EventDispatcher<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> eventDispatcher = new EventDispatcher<>();

    private float[][] samplesData;
    private AudioFormat audioFormat;
    private AudioMetadata tags;

    private AudioMixer innerMixer;
    protected ResamplerEffect resamplerEffect;
    protected final FloatControl speedControl = new FloatControl("Speed", 0.0f, 50f, 1.0f);

    private Playback playback;
    private int playedFrames = 0;
    protected boolean isPlaying = false;
    protected boolean loop = false;

    /**
     * The {@code Playback} class represents the inner class responsible for rendering audio samples.
     * It implements the {@link AudioNode} interface and overrides the {@link AudioNode#render(float[][], int)} method.
     */
    public class Playback implements AudioNode {
        @Override
        public void render(float[][] samples, int sampleRate) {
            if (!isPlaying) {
                AudioBufferUtilities.fill(samples, 0.0f);
                return;
            }

            int length = samples[0].length;

            int available = samplesData[0].length - playedFrames;
            int safeLength = Math.min(length, available);

            if (safeLength <= 0) {
                if (loop) {
                    playedFrames = 0;
                    safeLength = Math.min(length, samplesData[0].length);
                    dispatch(SoundSourceEventType.LOOP);
                } else {
                    isPlaying = false;
                    playedFrames = 0;
                    AudioBufferUtilities.fill(samples, 0.0f);
                    dispatch(SoundSourceEventType.DATA_ENDED);
                    return;
                }
            }

            for (int ch = 0; ch < samples.length; ch++) {
                float[] src = (ch < samplesData.length) ? samplesData[ch] : null;
                for (int i = 0; i < length; i++) {
                    if (i < safeLength && src != null) {
                        samples[ch][i] = src[playedFrames + i];
                    } else {
                        samples[ch][i] = 0.0f;
                    }
                }
            }

            playedFrames += safeLength;
        }
    }

    /**
     * Constructs a new {@code SoundSource} object with the specified audio data.
     * <p>
     * The {@code SoundSource} object is initialized with the specified audio data,
     * format, and tags. The object is not set to play back the audio data
     * automatically when the object is constructed.
     *
     * @param samplesData The audio samples data
     * @param audioFormat The audio format of the samples data
     * @param tags The audio tags associated with the samples data
     * @throws IllegalArgumentException If the samples data or audio format are invalid
     * @throws RuntimeException If adding the resampler effect fails
     */
    public SoundSource(float[][] samplesData, AudioFormat audioFormat, AudioMetadata tags) {
        this();
        this.open(samplesData, audioFormat, tags);
    }

    /**
     * Constructs a new {@code SoundSource} object and opens the specified audio file.
     *
     * @param file The audio file to open
     * @throws FileNotFoundException If the audio file is not found
     * @throws AudioCodecNotFoundException If the audio codec is not found
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails
     */
    public SoundSource(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        this();
        this.open(file);
    }

    /**
     * Constructs a new {@code SoundSource} object and opens the specified audio file.
     *
     * @param file The audio file path to open
     * @throws FileNotFoundException If the audio file is not found
     * @throws AudioCodecNotFoundException If the audio codec is not found
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails
     */
    public SoundSource(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this();
        this.open(file);
    }

    /**
     * Constructs a new {@code SoundSource} object,
     * which is not associated with any audio file.
     */
    public SoundSource() {
        var eventMap = eventDispatcher.createEventMap();
        eventMap.put(SoundSourceEventType.OPENED, SoundSourceListener::onOpened);
        eventMap.put(SoundSourceEventType.CLOSED, SoundSourceListener::onClosed);
        eventMap.put(SoundSourceEventType.STARTED, SoundSourceListener::onStarted);
        eventMap.put(SoundSourceEventType.STOPPED, SoundSourceListener::onStopped);
        eventMap.put(SoundSourceEventType.VOLUME_CHANGE, SoundSourceListener::onVolumeChanged);
        eventMap.put(SoundSourceEventType.PAN_CHANGE, SoundSourceListener::onPanChanged);
        eventMap.put(SoundSourceEventType.SPEED_CHANGE, SoundSourceListener::onSpeedChanged);
        eventMap.put(SoundSourceEventType.POSITION_CHANGE, SoundSourceListener::onPositionChanged);
        eventMap.put(SoundSourceEventType.LOOP, SoundSourceListener::onLoop);
        eventMap.put(SoundSourceEventType.DATA_ENDED, SoundSourceListener::onDataEnded);
        eventDispatcher.setEventMap(eventMap);

        speedControl.addConsumer(AudioControlEventType.VALUE_CHANGED, (type, event) -> {
            ResamplerEffect resampler = this.resamplerEffect;
            if (resampler != null) {
                resampler.getSpeedControl().setValue(speedControl.getValue());
                dispatch(SoundSourceEventType.SPEED_CHANGE);
            }
        });
    }

    /**
     * Initializes the sound source for playback by creating an inner mixer and associating it with a playback node.
     * This method is called automatically by the {@link #open(float[][], AudioFormat, AudioMetadata)} and
     * {@link #open(File)} methods.
     * <p>
     * The inner mixer is configured with post-gain and pan controls that dispatch {@link SoundSourceEventType#VOLUME_CHANGE}
     * and {@link SoundSourceEventType#PAN_CHANGE} events, respectively, when their values change.
     * <p>
     * Additionally, a resampler effect is added to the inner mixer, which allows for real-time resampling of
     * the audio signal.
     * <p>
     * If the resampler effect cannot be added to the inner mixer due to incompatible effect types or multiple
     * varying-size effects, a runtime exception is thrown.
     * 
     * @throws RuntimeException If the resampler effect cannot be added to the inner mixer
     */
    public void initialize() {
        if (innerMixer == null) {
            innerMixer = new AudioMixer();

            innerMixer.getPostGainControl().addConsumer(AudioControlEventType.VALUE_CHANGED, (type, event) ->
                    dispatch(SoundSourceEventType.VOLUME_CHANGE));

            innerMixer.getPanControl().addConsumer(AudioControlEventType.VALUE_CHANGED, (type, event) ->
                    dispatch(SoundSourceEventType.PAN_CHANGE));
        } else if (playback != null) {
            innerMixer.removeInput(playback);
        }

        playback = new Playback();
        innerMixer.addInput(playback);

        if (resamplerEffect == null) {
            resamplerEffect = new ResamplerEffect();
        }
        try {
            if (!innerMixer.getEffects().contains(resamplerEffect)) {
                innerMixer.addEffect(resamplerEffect);
            }
        } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
            logger.error("Failed to add resampler effect to inner mixer", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the sound source with the specified samples, audio format, and tags.
     * <p>
     * This method validates the samples and audio format, and then associates the sound source with the given data.
     * It also sets up the inner audio mixer and playback effect, and adds a resampler effect if it is not already present.
     * Finally, it resets the playback position and dispatches an {@link SoundSourceEventType#OPENED} event.
     *
     * @param samples The samples data to open
     * @param format The audio format of the samples data
     * @param tags The audio tags associated with the samples data
     * @throws IllegalArgumentException If the samples or audio format are invalid
     * @throws RuntimeException If setting up the inner audio mixer or playback effect fails
     */
    public void open(float[][] samples, AudioFormat format, AudioMetadata tags) {
        SamplesValidation.validateSamples(samples);
        if (format == null) {
            throw new IllegalArgumentException("Audio format cannot be null.");
        }
        this.samplesData = samples;
        this.audioFormat = format;
        this.tags = tags;

        initialize();
        reset(); // Reset the playback position
        dispatch(SoundSourceEventType.OPENED);
    }

    /**
     * Opens the specified audio file, decodes it, and prepares this SoundSource for playback.
     *
     * @param file The audio file to open. Supported formats depend on available codecs
     * @throws FileNotFoundException If the file does not exist or cannot be read
     * @throws AudioCodecNotFoundException If no suitable codec is available for decoding
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails
     */
    public void open(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        if (file == null || !file.exists()) {
            logger.error("File not found: {}", file);
            throw new FileNotFoundException("File not found.");
        }
        if (!file.isFile()) {
            throw new FileNotFoundException("Path is not a file.");
        }

        try {
            decodeAndOpenAudioFile(file);
        } catch (FileNotFoundException | AudioCodecNotFoundException ex) {
            throw ex; // Already logged
        } catch (AudioCodecException ex) {
            logger.error("Failed to decode audio file.", ex);
            throw new RuntimeException("Failed to decode audio file.", ex);
        }
    }

    /**
     * Opens the specified audio file from its path, decodes it, and prepares this SoundSource for playback.
     * If this SoundSource was opened previously, the mixer and resampler are not recreated,
     * and existing mixer effects are preserved.
     *
     * @param file The audio file path to open. Supported formats depend on available codecs
     * @throws FileNotFoundException If the file does not exist or cannot be read
     * @throws AudioCodecNotFoundException If no suitable codec is available for decoding
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails
     * @see #open(File)
     */
    public void open(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    /**
     * Checks if the sound source is initialized with a valid mixer and resampler effect.
     * Initialization occurs when the sound source is opened with either {@link #open(float[][], AudioFormat, AudioMetadata)} or
     * {@link #open(File)} methods.
     * @return true if the sound source is initialized, false otherwise
     */
    public boolean isInitialized() {
        return innerMixer != null && resamplerEffect != null;
    }

    /**
     * Returns true if the sound source has valid audio data, false otherwise.
     * <p>
     * A sound source has valid audio data if the samples data is not null, has a length greater than 0,
     * and the audio format is not null.
     *
     * @return true if the sound source has valid audio data, false otherwise
     */
    public boolean hasAudioData() {
        return samplesData != null && samplesData.length > 0 && audioFormat != null;
    }

    /**
     * Starts the playback of the sound source.
     * This method doesn't reset the played position, use {@link #reset()} for that.
     */
    public void start() {
        if (isPlaying) return;
        if (playedFrames >= samplesData[0].length) playedFrames = 0;
        isPlaying = true;
        logger.trace("Playback started");
        dispatch(SoundSourceEventType.STARTED);
    }

    /**
     * Stops the playback of the sound source, without resetting the played position.
     */
    public void stop() {
        if (!isPlaying) return;
        isPlaying = false;
        logger.trace("Playback stopped");
        dispatch(SoundSourceEventType.STOPPED);
    }

    /**
     * Checks if the sound source is currently playing.
     * @return {@code true} if the sound source is playing, {@code false} otherwise
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Renders the audio samples to the mixer.
     * <p>
     * This method is called by the audio mixer to request the sound source to render its audio data.
     * The sound source will then pass the samples to the mixer for mixing.
     * @param samples The audio samples to render
     * @param sampleRate The sample rate of the audio samples
     */
    @Override
    public void render(float[][] samples, int sampleRate) {
        innerMixer.render(samples, sampleRate);
    }

    /**
     * Closes the sound source.
     * <p>
     * This method stops the playback and resets the played position to 0.
     * It also dispatches a {@link SoundSourceEventType#CLOSED} event.
     */
    @Override
    public void close() {
        stop();
        reset();
        logger.trace("Closed");
        dispatch(SoundSourceEventType.CLOSED);
    }

    /**
     * Resets the playback position of the sound source.
     */
    public void reset() {
        playedFrames = 0;
    }

    /**
     * Sets the resampler effect of the sound source, sets the speed control value, and returns {@code true} if successful.
     * If the old resampler effect is the same as the new resampler effect, nothing is done, and {@code true} is returned.
     * If the new resampler effect cannot be added to the inner mixer, {@code false} is returned.
     *
     * @param newResamplerEffect The resampler effect to set, cannot be null
     * @throws IllegalArgumentException if the new resampler effect is null
     * @throws IllegalStateException if the sound source is not initialized
     */
    public boolean setResamplerEffect(ResamplerEffect newResamplerEffect) {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not prepared.");
        }
        if (newResamplerEffect == null)
            throw new IllegalArgumentException("Resampler effect cannot be null.");
        if (this.resamplerEffect == newResamplerEffect)
            return true;

        if (this.resamplerEffect != null) {
            innerMixer.removeEffect(this.resamplerEffect);
        }
        try {
            innerMixer.addEffect(newResamplerEffect);
            this.resamplerEffect = newResamplerEffect;
            this.resamplerEffect.getSpeedControl().setValue(this.speedControl.getValue());
            logger.debug("Resampler effect changed to {}", newResamplerEffect.toString() + ".");
            return true;
        } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
            logger.error("Failed to add resampler effect to inner mixer", e);
            return false;
        }
    }

    /**
     * Sets the loop state of the sound source.
     * @param loop {@code true} to loop the sound source, {@code false} otherwise
     * @throws IllegalStateException if the sound source is not initialized
     */
    public void setLooping(boolean loop) {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        this.loop = loop;
    }

    /**
     * Checks if the sound source is looping.
     * @return {@code true} if the sound source is looping, {@code false} otherwise
     * @throws IllegalStateException if the sound source is not initialized
     */
    public boolean isLooping() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return loop;
    }

    /**
     * Returns the inner mixer of the sound source.
     * The inner mixer is a mixer that contains the playback node and resampler effect.
     * It is used to process audio samples and apply effects to the sound source.
     *
     * @return The inner mixer of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public AudioMixer getInnerMixer() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return innerMixer;
    }

    /**
     * @return The gain control of the sound source (same as {@link #getPostGainControl()})
     * @throws IllegalStateException if the sound source is not initialized
     */
    public FloatControl getGainControl() {
        return getPostGainControl();
    }

    /**
     * @return The pre-gain control of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public FloatControl getPreGainControl() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return innerMixer.getPreGainControl();
    }

    /**
     * @return The post-gain control of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public FloatControl getPostGainControl() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return innerMixer.getPostGainControl();
    }

    /**
     * @return The resampler effect of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public ResamplerEffect getResamplerEffect() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return resamplerEffect;
    }

    /**
     * @return The pan control of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public FloatControl getPanControl() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return innerMixer.getPanControl();
    }

    /**
     * @return The speed control of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public FloatControl getSpeedControl() {
        if (!isInitialized()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return speedControl;
    }

    /**
     * Sets the frame position of the sound source.
     * @param position The frame position to set
     * @throws IllegalArgumentException if the position is out of bounds
     * @throws IllegalStateException if the sound source is not opened
     */
    public void setFramePosition(int position) {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        if (position < 0 || position > samplesData[0].length) {
            logger.error("Position must be between 0 and {}", samplesData[0].length);
            throw new IllegalArgumentException("Position must be between 0 and " + samplesData[0].length);
        }
        playedFrames = position;
        dispatch(SoundSourceEventType.POSITION_CHANGE);
    }

    /**
     * @return The frame position of the sound source
     * @throws IllegalStateException if the sound source is not opened
     */
    public int getFramePosition() {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        return playedFrames;
    }

    /**
     * Sets the seconds position of the sound source.
     * @param seconds The seconds position to set
     * @throws IllegalStateException if the sound source is not opened
     */
    public void setSecondsPosition(double seconds) {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        int samples = (int)AudioUnitsConverter.microsecondsToFrames((long)(seconds * 1_000_000.0), audioFormat.getSampleRate());
        setFramePosition(samples);
    }

    /**
     * @return The seconds position of the sound source
     * @throws IllegalStateException if the sound source is not opened
     */
    public double getSecondsPosition() {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        return AudioUnitsConverter.framesToMicroseconds(playedFrames, audioFormat.getSampleRate()) / 1_000_000.0;
    }

    /**
     * @return The duration of the sound source in seconds
     * @throws IllegalStateException if the sound source is not opened
     */
    public double getDuration() {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        return samplesData[0].length / (double)audioFormat.getSampleRate();
    }

    /**
     * Returns the audio samples associated with this sound source.
     *
     * <p>The returned array is a 2D float array of shape [channels][frames],
     * where each element is a normalized floating-point sample in the range [-1.0, 1.0].
     *
     * @return The audio samples associated with this sound source (a ref)
     * @throws IllegalStateException if the sound source is not opened
     */
    public float[][] getSamples() {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not opened.");
        }
        return samplesData;
    }

    /**
     * Returns the audio format of the sound source.
     * May be null if the sound source is not initialized.
     * 
     * @return The audio format of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public AudioFormat getAudioFormat() {
        if (audioFormat == null) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return audioFormat;
    }

    /**
     * Returns the metadata of the sound source.
     * May be null if the sound source is not initialized, or has no metadata.
     * @return The metadata of the sound source
     * @throws IllegalStateException if the sound source is not initialized
     */
    public AudioMetadata getMetadata() {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        return tags;
    }

    /**
     * Applies an audio effect to the sound source samples.
     * <p>
     * The sound source must not be playing. If the effect is a {@link VaryingSizeEffect},
     * the internal sample arrays may be resized to accommodate the effect's requirements.
     *
     * @param effect The audio effect to apply
     * @throws IllegalArgumentException if the effect is null
     * @throws IllegalStateException if the sound source is not initialized or is currently playing
     */
    public void applyEffect(AudioEffect effect) {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        if (isPlaying) {
            throw new IllegalStateException("Sound source is playing.");
        }
        if (effect == null) {
            throw new IllegalArgumentException("Effect cannot be null.");
        }

        float mix = effect.getMixLevelControl().getValue();
        if (!effect.getEnableControl().getValue() || mix <= 0.0f) {
            return; // Effect disabled or zero mix, nothing to do
        }

        boolean varyingSize = effect instanceof VaryingSizeEffect;
        int targetLength = varyingSize
                ? ((VaryingSizeEffect) effect).getTargetLength(samplesData[0].length)
                : samplesData[0].length;

        int effectInputLength = Math.max(targetLength, samplesData[0].length);

        // Prepare input array for effect
        float[][] effectInput = new float[samplesData.length][effectInputLength];
        for (int ch = 0; ch < samplesData.length; ch++) {
            System.arraycopy(samplesData[ch], 0, effectInput[ch], 0, samplesData[ch].length);
        }

        // Apply the effect
        logger.trace("Applying effect {} on samples {}, with target length {}...",
                effect.getClass().getSimpleName(), samplesData.length, targetLength);
        long startNs = System.nanoTime();
        effect.render(effectInput, audioFormat.getSampleRate());
        long endNs = System.nanoTime();
        logger.trace("Effect took {} ms", (endNs - startNs) / 1_000_000.0);

        // Mix effect output back into samplesData
        for (int ch = 0; ch < samplesData.length; ch++) {
            int len = Math.min(samplesData[ch].length, effectInput[ch].length);
            for (int i = 0; i < len; i++) {
                samplesData[ch][i] = samplesData[ch][i] * (1.0f - mix) + effectInput[ch][i] * mix;
            }
        }

        // If VaryingSizeEffect increased array length, resize samplesData
        if (varyingSize && targetLength != samplesData[0].length) {
            float[][] newSamples = new float[samplesData.length][targetLength];
            for (int ch = 0; ch < samplesData.length; ch++) {
                int copyLen = Math.min(samplesData[ch].length, targetLength);
                System.arraycopy(samplesData[ch], 0, newSamples[ch], 0, copyLen);
                if (targetLength > samplesData[ch].length) {
                    System.arraycopy(effectInput[ch], samplesData[ch].length,
                                    newSamples[ch], samplesData[ch].length,
                                    targetLength - samplesData[ch].length);
                }
            }
            samplesData = newSamples;
        }
    }

    /**
     * Applies an audio effect builder to the sound source.
     * The audio effect builder is responsible for creating the actual
     * audio effect and passing it to this method.
     *
     * @param effect The audio effect builder that will be applied
     * @throws IllegalArgumentException If the effect builder is null
     * @throws IllegalStateException If the sound source is not initialized
     */
    public void applyEffect(AudioEffectBuilder effect) {
        if (!hasAudioData()) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        if (effect == null) {
            throw new IllegalArgumentException("Effect builder cannot be null.");
        }
        applyEffect(effect.getEffect());
    }

    @Override
    public ListenersManager<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> getListenersManager() {
        return eventDispatcher.getListenersManager();
    }

    private void decodeAndOpenAudioFile(File file) throws FileNotFoundException, AudioCodecNotFoundException, AudioCodecException {
        try {
            int dot = file.getName().lastIndexOf('.');
            if (dot < 0 || dot == file.getName().length() - 1) {
                throw new AudioCodecException("File has no valid extension: " + file.getName());
            }
            String fileExtension = file.getName().substring(dot + 1);

            if (fileExtension == null || fileExtension.isEmpty()) {
                logger.error("File has no extension: {}", file.getName());
                throw new AudioCodecException("File has no extension: " + file.getName());
            }
            logger.debug("Decoding audio file: {} with extension: {}", file.getName(), fileExtension);
            AudioCodecInfo codec = AudioCodecs.fromExtension(fileExtension);

            logger.debug("Using codec: {}", codec.getName());
            AudioCodec audioCodec = AudioCodecs.getCodec(codec);

            AudioDecodeResult decodeResult = audioCodec.decode(
                new BufferedInputStream(new FileInputStream(file), 1024 * 256)
            );

            if (decodeResult == null ||
                    decodeResult.getSamples() == null || decodeResult.getSamples().length == 0 ||
                    decodeResult.getAudioFormat() == null) {
                logger.error("Failed to decode audio file: {}", file.getName());
                throw new AudioCodecException("Failed to decode audio file: " + file.getName());
            }
            logger.trace("Decoded audio file: {}", file.getName());

            this.open(decodeResult.getSamples(), decodeResult.getAudioFormat(), decodeResult.getMetadata());
            logger.trace("Opened audio file: {}", file.getName());
        } catch (AudioCodecNotFoundException ex) {
            logger.error("Audio codec not found: {}. {}", file.getName(), ex.getMessage());
            throw ex;
         }catch (AudioCodecException ex) {
            logger.error("Failed to decode audio file: {}. {}", file.getName(), ex.getMessage());
            throw ex;
        } catch (FileNotFoundException ex) {
            logger.error("Failed to open audio file: {}. {}", file.getName(), ex.getMessage());
            throw ex;
        }
    }

    private void dispatch(SoundSourceEventType type) {
        eventDispatcher.dispatch(type, new SoundSourceEvent(this));
    }
}
