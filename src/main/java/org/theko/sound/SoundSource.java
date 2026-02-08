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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.events.EventDispatcher;
import org.theko.events.ListenersManager;
import org.theko.events.ListenersManagerProvider;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioTags;
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
public class SoundSource implements AudioNode, Controllable, AutoCloseable,
        ListenersManagerProvider<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> {

    private static final Logger logger = LoggerFactory.getLogger(SoundSource.class);
    private final EventDispatcher<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> eventDispatcher = new EventDispatcher<>();

    private float[][] samplesData;
    private AudioFormat audioFormat;
    private AudioTags tags;
    
    protected AudioMixer innerMixer;
    protected ResamplerEffect resamplerEffect;
    protected final FloatControl speedControl = new FloatControl("Speed", 0.0f, 50f, 1.0f);

    private Playback playback;
    protected int playedFrames = 0;
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
                    ArrayUtilities.fillZeros(samples);
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
     * Constructs a new {@code SoundSource} object and opens the specified audio file.
     * 
     * @param file The audio file to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails.
     */
    public SoundSource(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        this();
        this.open(file);
    }

    /**
     * Constructs a new {@code SoundSource} object and opens the specified audio file.
     * 
     * @param file The audio file path to open.
     * @throws FileNotFoundException If the audio file is not found.
     * @throws AudioCodecNotFoundException If the audio codec is not found.
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails.
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

        speedControl.getListenersManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> {
            ResamplerEffect resampler = this.resamplerEffect;
            if (resampler != null) {
                resampler.getSpeedControl().setValue(speedControl.getValue());
                dispatch(SoundSourceEventType.SPEED_CHANGE);
            }
        });
    }

    /**
     * Opens the specified audio file, decodes it, and prepares this SoundSource for playback.
     * If this SoundSource was opened previously, the mixer and resampler are not recreated,
     * and existing mixer effects are preserved.
     *
     * @param file The audio file to open. Supported formats depend on available codecs.
     * @throws FileNotFoundException If the file does not exist or cannot be read.
     * @throws AudioCodecNotFoundException If no suitable codec is available for decoding.
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails.
     */
    public void open(File file) throws FileNotFoundException, AudioCodecNotFoundException {
        if (file == null || !file.exists()) {
            logger.error("File not found: {}", file);
            throw new FileNotFoundException("File not found.");
        }

        try {
            decodeAudioFile(file);
        } catch (FileNotFoundException | AudioCodecNotFoundException ex) {
            throw ex; // Already logged
        } catch (AudioCodecException ex) {
            logger.error("Failed to decode audio file.", ex);
            throw new RuntimeException("Failed to decode audio file.", ex);
        }

        if (innerMixer == null) {
            innerMixer = new AudioMixer();

            innerMixer.getPostGainControl().getListenersManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> 
                    dispatch(SoundSourceEventType.VOLUME_CHANGE));

            innerMixer.getPanControl().getListenersManager().addConsumer(AudioControlEventType.VALUE_CHANGED, event -> 
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

        reset(); // Reset the playback position
        dispatch(SoundSourceEventType.OPENED);
    }

    /**
     * Opens the specified audio file from its path, decodes it, and prepares this SoundSource for playback.
     * If this SoundSource was opened previously, the mixer and resampler are not recreated,
     * and existing mixer effects are preserved.
     *
     * @param file The audio file path to open. Supported formats depend on available codecs.
     * @throws FileNotFoundException If the file does not exist or cannot be read.
     * @throws AudioCodecNotFoundException If no suitable codec is available for decoding.
     * @throws RuntimeException If decoding fails for other reasons or adding the resampler effect fails.
     * @see #open(File)
     */
    public void open(String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    /**
     * Starts the playback of the sound source.
     * This method doesn't reset the played position, use {@link #reset()} for that.
     */
    public void start() {
        if (isPlaying) return;
        if (playedFrames >= samplesData[0].length) playedFrames = 0;
        isPlaying = true;
        logger.trace("Playback started.");
        dispatch(SoundSourceEventType.STARTED);
    }

    /**
     * Stops the playback of the sound source, without resetting the played position.
     */
    public void stop() {
        if (!isPlaying) return;
        isPlaying = false;
        logger.trace("Playback stopped.");
        dispatch(SoundSourceEventType.STOPPED);
    }

    @Override
    public void render(float[][] samples, int sampleRate) {
        innerMixer.render(samples, sampleRate);
    }

    @Override
    public void close() {
        stop();
        logger.debug("Closed.");
        dispatch(SoundSourceEventType.CLOSED);
    }

    /**
     * Resets the playback position of the sound source.
     */
    public void reset() {
        playedFrames = 0;
    }

    /**
     * Checks if the sound source is currently playing.
     * @return {@code true} if the sound source is playing, {@code false} otherwise.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * @return The gain control of the sound source.
     */
    public FloatControl getGainControl() {
        return innerMixer.getPostGainControl();
    }

    /** 
     * @return The inner mixer of the sound source.
     */
    public AudioMixer getInnerMixer() {
        return innerMixer;
    }

    /**
     * @return The resampler effect of the sound source.
     */
    public ResamplerEffect getResamplerEffect() {
        return resamplerEffect;
    }

    /**
     * Sets the resampler effect of the sound source, sets the speed control value, and returns {@code true} if successful.
     * If the old resampler effect is the same as the new resampler effect, nothing is done, and {@code true} is returned.
     * If the new resampler effect cannot be added to the inner mixer, {@code false} is returned.
     * 
     * @param newResamplerEffect The resampler effect to set, cannot be null.
     * @throws IllegalArgumentException if the new resampler effect is null.
     */
    public boolean setResamplerEffect(ResamplerEffect newResamplerEffect) {
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
            logger.debug("Resampler effect changed.");
            return true;
        } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
            logger.error("Failed to add resampler effect to inner mixer", e);
            return false;
        }
    }

    /**
     * Sets the loop state of the sound source.
     * @param loop {@code true} to loop the sound source, {@code false} otherwise.
     */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    /**
     * Checks if the sound source is looping.
     * @return {@code true} if the sound source is looping, {@code false} otherwise.
     */
    public boolean isLooping() {
        return loop;
    }
    
    /**
     * @return The pan control of the sound source.
     */
    public FloatControl getPanControl() {
        return innerMixer.getPanControl();
    }

    /**
     * @return The speed control of the sound source.
     */
    public FloatControl getSpeedControl() {
        return speedControl;
    }

    /**
     * Sets the frame position of the sound source.
     * @param position The frame position to set.
     */
    public void setFramePosition(int position) {
        if (position < 0 || position > samplesData[0].length) {
            logger.error("Position must be between 0 and {}", samplesData[0].length);
            throw new IllegalArgumentException("Position must be between 0 and " + samplesData[0].length);
        }
        playedFrames = position;
        dispatch(SoundSourceEventType.POSITION_CHANGE);
    }

    /**
     * @return The frame position of the sound source.
     */
    public int getFramePosition() {
        return playedFrames;
    }

    /**
     * Sets the seconds position of the sound source.
     * @param seconds The seconds position to set.
     */
    public void setSecondsPosition(double seconds) {
        int samples = (int)AudioUnitsConverter.microsecondsToFrames((long)(seconds * 1_000_000.0), audioFormat.getSampleRate());
        setFramePosition(samples);
    }

    /**
     * @return The seconds position of the sound source.
     */
    public double getSecondsPosition() {
        return AudioUnitsConverter.framesToMicroseconds(playedFrames, audioFormat.getSampleRate()) / 1_000_000.0;
    }

    /**
     * @return The duration of the sound source in seconds.
     */
    public double getDuration() {
        if (samplesData == null || audioFormat == null) {
            return 0.0;
        }
        return samplesData[0].length / (double)audioFormat.getSampleRate();
    }

    /**
     * @return The audio format of the sound source.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * @return The metadata tags of the sound source.
     */
    public AudioTags getTags() {
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

        float mix = effect.getMixLevelControl().getValue();
        if (effect.getEnableControl().isDisabled() || mix <= 0.0f) {
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
        effect.render(effectInput, audioFormat.getSampleRate());

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

    @Override
    public ListenersManager<SoundSourceEvent, SoundSourceListener, SoundSourceEventType> getListenersManager() {
        return eventDispatcher.getListenersManager();
    }

    private void decodeAudioFile(File file) throws FileNotFoundException, AudioCodecNotFoundException, AudioCodecException {
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

            if (decodeResult == null ||
                    decodeResult.getSamples() == null || decodeResult.getSamples().length == 0 ||
                    decodeResult.getAudioFormat() == null) {
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
            throw ex;
        }
    }

    private void dispatch(SoundSourceEventType type) {
        eventDispatcher.dispatch(type, new SoundSourceEvent(this));
    }
}
