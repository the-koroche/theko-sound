package org.theko.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.IncompatibleEffectTypeException;
import org.theko.sound.effects.MultipleVaryingSizeEffectsException;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.utility.ArrayUtilities;

/**
 * The {@code SoundSource} class represents an audio source that can be controlled
 * and played back. It implements the {@link AudioNode} and {@link Controllable} interfaces,
 * allowing it to render audio samples and manage audio controls.
 *
 * <p>This class provides functionalities such as opening an audio file, rendering audio samples,
 * and controlling playback. It supports looping and can reset playback state as needed.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Opening audio files and decoding them into samples data.</li>
 *   <li>Rendering audio samples through the {@link Playback} inner class.</li>
 *   <li>Managing playback state with controls for speed, gain, and pan.</li>
 *   <li>Support for looping and resetting playback.</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * SoundSource soundSource = new SoundSource();
 * soundSource.open(new File("path/to/audio/file"));
 * soundSource.start();
 * }</pre>
 *
 * <p>Note: This class relies on external utilities and effects such as {@link AudioMixer}
 * and {@link ResamplerEffect} for processing audio data.</p>
 *
 * @see AudioNode
 * @see Controllable
 * @see Playback
 * @see AudioMixer
 * @see ResamplerEffect
 * 
 * @since v2.0.0
 * @author Theko
 */
public class SoundSource implements AudioNode, Controllable, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SoundSource.class);

    private float[][] samplesData;
    private AudioFormat audioFormat;
    
    protected AudioMixer innerMixer;
    protected ResamplerEffect resamplerEffect;

    private Playback playback;
    protected int playedSamples = 0;
    protected boolean isPlaying = false;
    protected boolean loop = false;

    public class Playback implements AudioNode {
        @Override
        public void render (float[][] samples, int sampleRate, int length) {
            if (!isPlaying) {
                ArrayUtilities.fillZeros(samples);
                return;
            }

            int available = samplesData[0].length - playedSamples;
            int safeLength = Math.min(length, available);

            if (safeLength <= 0) {
                if (loop) {
                    playedSamples = 0;
                    safeLength = Math.min(length, samplesData[0].length);
                } else {
                    isPlaying = false;
                    playedSamples = 0;
                    ArrayUtilities.fillZeros(samples);
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

    public SoundSource (File file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(file);
    }

    public SoundSource (String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(file);
    }

    public SoundSource () {
    }

    public void open (File file) throws FileNotFoundException, AudioCodecNotFoundException {
        if (file == null || !file.exists()) {
            logger.error("File not found: {}", file);
            throw new FileNotFoundException("File not found.");
        }
        decodeAudioFile(file);
        if (samplesData == null) {
            logger.error("Failed to decode audio file: {}. Audio codec not found.", file);
            throw new AudioCodecNotFoundException("Audio codec not found.");
        }

        playback = new Playback();

        innerMixer = new AudioMixer();
        innerMixer.addInput(playback);

        resamplerEffect = new ResamplerEffect();
        try {
            innerMixer.addEffect(resamplerEffect);
        } catch (IncompatibleEffectTypeException | MultipleVaryingSizeEffectsException e) {
            logger.error("Failed to add resampler effect to inner mixer", e);
            throw new RuntimeException(e);
        }
    }

    public void open (String file) throws FileNotFoundException, AudioCodecNotFoundException {
        this.open(new File(file));
    }

    public void start () {
        isPlaying = true;
        playedSamples = 0;
    }

    public void stop () {
        isPlaying = false;
        playedSamples = 0;
    }

    @Override
    public void render (float[][] samples, int sampleRate, int length) {
        innerMixer.render(samples, sampleRate, length);
    }

    public void close () {
        stop();
        samplesData = null;
    }

    public void reset () {
        playedSamples = 0;
    }

    public boolean isPlaying () {
        return isPlaying;
    }

    public FloatControl getGainControl () {
        return innerMixer.getPostGainControl();
    }

    public AudioMixer getInnerMixer () {
        return innerMixer;
    }

    public ResamplerEffect getResamplerEffect () {
        return resamplerEffect;
    }

    public void setLoop (boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop () {
        return loop;
    }
    
    public FloatControl getPanControl () {
        return innerMixer.getPanControl();
    }

    public FloatControl getSpeedControl () {
        return resamplerEffect.getSpeedControl();
    }

    public void setSamplePosition (int position) {
        if (position < 0 || position > samplesData[0].length) {
            throw new IllegalArgumentException("Position must be between 0 and " + samplesData[0].length);
        }
        playedSamples = position;
    }

    public int getSamplePosition () {
        return playedSamples;
    }

    public void setSecondsPosition (double seconds) {
        int samples = (int)AudioConverter.microsecondsToSamples((long)(seconds * 1_000_000), audioFormat.getSampleRate());
        setSamplePosition(samples);
    }

    public double getSecondsPosition () {
        return AudioConverter.samplesToMicrosecond(playedSamples, audioFormat.getSampleRate()) / 1_000_000.0;
    }

    public double getDuration () {
        if (samplesData == null || audioFormat == null) {
            return 0.0;
        }
        return samplesData[0].length / (double)audioFormat.getSampleRate();
    }

    public AudioFormat getAudioFormat () {
        return audioFormat;
    }

    private void decodeAudioFile (File file) {
        try {
            String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            if (fileExtension == null || fileExtension.isEmpty()) {
                logger.error("File has no extension: {}", file.getName());
                throw new AudioCodecException("File has no extension: " + file.getName());
            }
            logger.debug("Decoding audio file: {} with extension: {}", file.getName(), fileExtension);
            AudioCodecInfo codec = AudioCodecs.fromExtension(fileExtension);

            logger.debug("Using codec: {}", codec);
            AudioCodec audioCodec = AudioCodecs.getCodec(codec);

            logger.debug("Audio codec: {}", audioCodec.getClass().getSimpleName());
            AudioDecodeResult decodeResult = audioCodec.decode(new FileInputStream(file));

            if (decodeResult == null || decodeResult.getSamples() == null || decodeResult.getSamples().length == 0) {
                logger.error("Failed to decode audio file: {}", file.getName());
                throw new AudioCodecException("Failed to decode audio file: " + file.getName());
            }
            
            this.samplesData = decodeResult.getSamples();
            this.audioFormat = decodeResult.getAudioFormat();
            logger.debug("Decoded audio file: {} with format: {}", file.getName(), audioFormat);
        } catch (AudioCodecException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
