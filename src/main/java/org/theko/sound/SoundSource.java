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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.EventDispatcher;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.AudioEffect;
import org.theko.sound.effects.IncompatibleEffectTypeException;
import org.theko.sound.effects.MultipleVaryingSizeEffectsException;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.effects.VaryingSizeEffect;
import org.theko.sound.event.AudioControlEventType;
import org.theko.sound.event.SoundSourceEvent;
import org.theko.sound.event.SoundSourceEventType;
import org.theko.sound.event.SoundSourceListener;
import org.theko.sound.utility.ArrayUtilities;

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
 * <p>Usage example:
 * <pre>{@code
 * SoundSource soundSource = new SoundSource();
 * soundSource.open(new File("path/to/audio/file"));
 * soundSource.start();
 * }</pre>
 *
 * <p>Note: This class relies on external utilities and effects such as {@link AudioMixer}
 * and {@link ResamplerEffect} for processing audio data.
 *
 * @see AudioNode
 * @see Controllable
 * @see Playback
 * @see AudioMixer
 * @see ResamplerEffect
 * 
 * @since 2.0.0
 * @author Theko
 */
public class SoundSource implements AudioNode, Controllable, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SoundSource.class);
    private final EventDispatcher<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> eventDispatcher = new EventDispatcher<>();

    private float[][] samplesData;
    private AudioFormat audioFormat;
    private List<AudioTag> tags;
    
    protected AudioMixer innerMixer;
    protected ResamplerEffect resamplerEffect;

    private Playback playback;
    protected int playedSamples = 0;
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
                ArrayUtilities.fillZeros(samples);
                return;
            }

            int length = samples[0].length;

            int available = samplesData[0].length - playedSamples;
            int safeLength = Math.min(length, available);

            if (safeLength <= 0) {
                if (loop) {
                    playedSamples = 0;
                    safeLength = Math.min(length, samplesData[0].length);
                    eventDispatcher.dispatch(SoundSourceEventType.LOOP, new SoundSourceEvent(SoundSource.this));
                } else {
                    isPlaying = false;
                    playedSamples = 0;
                    ArrayUtilities.fillZeros(samples);
                    eventDispatcher.dispatch(SoundSourceEventType.DATA_ENDED, new SoundSourceEvent(SoundSource.this));
                    return;
                }
            }
            
            for (int ch = 0; ch < samples.length; ch++) {
                float[] src = (ch < samplesData.length) ? samplesData[ch] : null;
                for (int i = 0; i < length; i++) {
                    if (i < safeLength && src != null) {
                        samples[ch][i] = src[playedSamples + i];
                    } else {
                        samples[ch][i] = 0.0f;
                    }
                }
            }

            playedSamples += safeLength;
        }
    }

    /**
     * Constructs a new {@code SoundSource} object.
     * 
     * @param file The audio file to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    public SoundSource(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        this();
        this.open(file);
    }

    /**
     * Constructs a new {@code SoundSource} object.
     * 
     * @param file The audio file path to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
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
    }

    /**
     * Opens an audio file and decodes it into samples data.
     * 
     * @param file The audio file to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    public void open(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        if (file == null || !file.exists()) {
            logger.error("File not found: {}", file);
            throw new FileNotFoundException("File not found.");
        }
        try {
            decodeAudioFile(file);
        } catch (AudioCodecNotFoundException ex) {
            throw ex; // Already logged
        } catch (AudioCodecException ex) {
            throw new RuntimeException("Failed to decode audio file.", ex);
        }
        if (samplesData == null) {
            logger.error("Failed to decode audio file: {}.", file);
            throw new RuntimeException("Failed to decode audio file.");
        }

        playback = new Playback();

        innerMixer = new AudioMixer();
        innerMixer.addInput(playback);

        innerMixer.getPostGainControl().getListenerManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> 
                eventDispatcher.dispatch(SoundSourceEventType.VOLUME_CHANGE, new SoundSourceEvent(SoundSource.this)));

        innerMixer.getPanControl().getListenerManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> 
                eventDispatcher.dispatch(SoundSourceEventType.PAN_CHANGE, new SoundSourceEvent(SoundSource.this)));

        resamplerEffect = new ResamplerEffect();
        try {
            innerMixer.addEffect(resamplerEffect);
            resamplerEffect.getSpeedControl().getListenerManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> 
                    eventDispatcher.dispatch(SoundSourceEventType.SPEED_CHANGE, new SoundSourceEvent(SoundSource.this)));
        } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
            logger.error("Failed to add resampler effect to inner mixer", e);
            throw new RuntimeException(e);
        }

        eventDispatcher.dispatch(SoundSourceEventType.OPENED, new SoundSourceEvent(this));
    }

    /**
     * Opens an audio file and decodes it into samples data.
     * 
     * @param file The audio file path to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     */
    public void open(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    /**
     * Starts the playback of the sound source.
     */
    public void start() {
        if (isPlaying) return;
        isPlaying = true;
        playedSamples = 0;
        logger.debug("Playback started.");
        eventDispatcher.dispatch(SoundSourceEventType.STARTED, new SoundSourceEvent(this));
    }

    /**
     * Stops the playback of the sound source.
     */
    public void stop() {
        if (!isPlaying) return;
        isPlaying = false;
        playedSamples = 0;
        logger.debug("Playback stopped.");
        eventDispatcher.dispatch(SoundSourceEventType.STOPPED, new SoundSourceEvent(this));
    }

    @Override
    public void render(float[][] samples, int sampleRate) {
        innerMixer.render(samples, sampleRate);
    }

    @Override
    public void close() {
        stop();
        samplesData = null;
        audioFormat = null;

        logger.debug("Closed.");
        eventDispatcher.dispatch(SoundSourceEventType.CLOSED, new SoundSourceEvent(this));
    }

    /**
     * Resets the playback state of the sound source.
     */
    public void reset() {
        playedSamples = 0;
    }

    /**
     * Checks if the sound source is currently playing.
     * @return {@code true} if the sound source is playing, {@code false} otherwise.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Returns the gain control of the sound source.
     * @return The gain control of the sound source.
     */
    public FloatControl getGainControl() {
        return innerMixer.getPostGainControl();
    }

    /** 
     * Returns the inner mixer of the sound source.
     * @return The inner mixer of the sound source.
     */
    public AudioMixer getInnerMixer() {
        return innerMixer;
    }

    /**
     * Returns the resampler effect of the sound source.
     * @return The resampler effect of the sound source.
     */
    public ResamplerEffect getResamplerEffect() {
        return resamplerEffect;
    }

    /**
     * Sets the loop state of the sound source.
     * @param loop {@code true} to loop the sound source, {@code false} otherwise.
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * Checks if the sound source is in loop mode.
     * @return {@code true} if the sound source is in loop mode, {@code false} otherwise.
     */
    public boolean isLoop() {
        return loop;
    }
    
    /**
     * Returns the pan control of the sound source.
     * @return The pan control of the sound source.
     */
    public FloatControl getPanControl() {
        return innerMixer.getPanControl();
    }

    /**
     * Returns the speed control of the sound source.
     * @return The speed control of the sound source.
     */
    public FloatControl getSpeedControl() {
        return resamplerEffect.getSpeedControl();
    }

    /**
     * Sets the sample position of the sound source.
     * @param position The sample position to set.
     */
    public void setSamplePosition(int position) {
        if (position < 0 || position > samplesData[0].length) {
            logger.error("Position must be between 0 and {}", samplesData[0].length);
            throw new IllegalArgumentException("Position must be between 0 and " + samplesData[0].length);
        }
        playedSamples = position;
        eventDispatcher.dispatch(SoundSourceEventType.POSITION_CHANGE, new SoundSourceEvent(this));
    }

    /**
     * Returns the sample position of the sound source.
     * @return The sample position.
     */
    public int getSamplePosition() {
        return playedSamples;
    }

    /**
     * Sets the seconds position of the sound source.
     * @param seconds The seconds position to set.
     */
    public void setSecondsPosition(double seconds) {
        int samples = (int)AudioUnitsConverter.microsecondsToFrames((long)(seconds * 1_000_000.0), audioFormat.getSampleRate());
        setSamplePosition(samples);
    }

    /**
     * Returns the seconds position of the sound source.
     * @return The seconds position.
     */
    public double getSecondsPosition() {
        return AudioUnitsConverter.framesToMicroseconds(playedSamples, audioFormat.getSampleRate()) / 1_000_000.0;
    }

    /**
     * Returns the duration of the sound source in seconds.
     * @return The duration of the sound source in seconds.
     */
    public double getDuration() {
        if (samplesData == null || audioFormat == null) {
            return 0.0;
        }
        return samplesData[0].length / (double)audioFormat.getSampleRate();
    }

    /**
     * Returns the audio format of the sound source.
     * @return The audio format of the sound source.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Returns the metadata tags of the sound source.
     * @return The metadata tags of the sound source.
     */
    public List<AudioTag> getTags() {
        return tags;
    }

    /**
     * Applies an audio effect to the sound source samples.
     * <p>
     * The sound source must not be playing. If the effect is a {@link VaryingSizeEffect},
     * the internal sample arrays may be resized to accommodate the effect's requirements.
     *
     * @param effect The audio effect to apply.
     * @throws IllegalArgumentException if the effect is null.
     * @throws IllegalStateException if the sound source is not initialized or is currently playing.
     */
    public void applyEffect(AudioEffect effect) {
        if (samplesData == null || audioFormat == null) {
            throw new IllegalStateException("Sound source is not initialized.");
        }
        if (isPlaying) {
            throw new IllegalStateException("Sound source is playing.");
        }
        if (effect == null) {
            throw new IllegalArgumentException("Effect cannot be null.");
        }
        boolean varyingSizeEffect = effect instanceof VaryingSizeEffect;
        int targetLength = samplesData[0].length;
        if (varyingSizeEffect) {
            targetLength = ((VaryingSizeEffect) effect).getInputLength(samplesData[0].length);
        }
        if (targetLength != samplesData[0].length) {
            if (targetLength > samplesData[0].length) {
                logger.debug("Resizing samples data from {} to {}.", samplesData[0].length, targetLength);
                samplesData = ArrayUtilities.padArray(samplesData, samplesData.length, targetLength);
            }
        }
        effect.render(samplesData, audioFormat.getSampleRate());
        if (targetLength != samplesData[0].length) {
            if (targetLength < samplesData[0].length) {
                logger.debug("Resizing samples data from {} to {}.", samplesData[0].length, targetLength);
                samplesData = ArrayUtilities.cutArray(samplesData, 0, samplesData.length, 0, targetLength);
            }
        }
    }

    public void addListener(SoundSourceListener listener) {
        eventDispatcher.addListener(listener);
    }

    public void removeListener(SoundSourceListener listener) {
        eventDispatcher.removeListener(listener);
    }

    public List<SoundSourceListener> getListeners() {
        return eventDispatcher.getListeners();
    }

    /**
     * Decodes an audio file into samples data.
     * @param file The audio file to decode.
     */
    private void decodeAudioFile(File file) throws AudioCodecNotFoundException, AudioCodecException {
        try {
            String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
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

            if (decodeResult == null || decodeResult.getSamples() == null || decodeResult.getSamples().length == 0) {
                logger.error("Failed to decode audio file: {}", file.getName());
                throw new AudioCodecException("Failed to decode audio file: " + file.getName());
            }
            
            this.samplesData = decodeResult.getSamples();
            this.audioFormat = decodeResult.getAudioFormat();
            this.tags = decodeResult.getTags();
            logger.debug("Decoded audio file: {}", file.getName());
        } catch (AudioCodecNotFoundException ex) {
            logger.error("Audio codec not found: {}", file.getName(), ex);
            throw ex;
         }catch (AudioCodecException ex) {
            logger.error("Failed to decode audio file: {}", file.getName(), ex);
            throw ex;
        } catch (FileNotFoundException ex) {
            logger.error("Failed to open audio file: {}", file.getName(), ex);
        }
    }
}
