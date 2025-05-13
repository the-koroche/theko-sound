package org.theko.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecCreationException;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.control.AudioControl;
import org.theko.sound.control.Controllable;
import org.theko.sound.control.FloatControl;
import org.theko.sound.effects.ResamplerEffect;
import org.theko.sound.event.DataLineAdapter;
import org.theko.sound.event.DataLineEvent;


/**
 * The `SoundSource` class provides functionality for audio playback, including
 * support for audio effects, looping, and playback position control. It implements
 * the `AutoCloseable` and `Controllable` interfaces, allowing for resource management
 * and control over audio playback settings.
 * 
 * <p>This class supports opening audio files, decoding them, and playing them back
 * with customizable buffer sizes. It also provides methods for controlling playback
 * speed, volume, and stereo panning. Additionally, it supports looping playback
 * and precise control over playback position in frames or microseconds.</p>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Opening audio files with automatic or custom buffer sizes.</li>
 *   <li>Support for audio effects, such as speed adjustment.</li>
 *   <li>Looping playback with configurable loop counts.</li>
 *   <li>Playback position control in frames or microseconds.</li>
 *   <li>Thread-safe operations for playback and resource management.</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * SoundSource soundSource = new SoundSource();
 * soundSource.open("path/to/audio/file.wav");
 * soundSource.start();
 * 
 * // Adjust playback settings
 * soundSource.getGainControl().setValue(0.5f); // Set volume
 * soundSource.setLoop(SoundSource.LOOP_CONTINUOUSLY); // Enable looping
 * 
 * // Stop playback and release resources
 * soundSource.stop();
 * soundSource.close();
 * }</pre>
 * 
 * <p>Note: This class requires proper handling of resources. Always call the `close()`
 * method to release resources when the sound source is no longer needed.</p>
 * 
 * @see AutoCloseable
 * @see Controllable
 * @see AudioMixer
 * @see AudioEffect
 * 
 * @author Alex Soloviov
 */
public class SoundSource implements AutoCloseable, Controllable {
    private static final Logger logger = LoggerFactory.getLogger(SoundSource.class);

    // Audio properties
    protected AudioMixer mixer;
    protected AudioFormat audioFormat;
    protected float[][][] audioData;
    protected int audioDataFragments;
    protected int bufferSize;
    protected List<AudioTag> metadata;
    private Thread playbackThread;
    private int threadPriotity = Thread.MAX_PRIORITY - 1; // Default thread priority
    protected boolean isPlaying;
    protected boolean isOpen;
    protected int played;
    protected int offset;
    protected long length;

    protected int loop;
    protected int loopCount;

    protected DataLine mixerIn;

    private DataLine audioOut;
    protected boolean pendingSeeking = false;
    
    protected ResamplerEffect speedEffect;

    // Instance counting
    private static int soundInstances = 0;
    private final int thisSoundInstance = ++soundInstances;

    // Predefined buffer sizes
    public static final int AUTO_BUFFER_SIZE = -1;
    public static final int BUFFER_SIZE_32MS = -10;
    public static final int BUFFER_SIZE_64MS = -11;
    public static final int BUFFER_SIZE_128MS = -12;
    public static final int BUFFER_SIZE_256MS = -13;
    public static final int BUFFER_SIZE_512MS = -14;
    public static final int BUFFER_SIZE_1024MS = -15;
    public static final int BUFFER_SIZE_2048MS = -16;

    public static final int NO_LOOP = 0;
    public static final int LOOP_CONTINUOUSLY = -1;

    // Time-out values
    protected static final int SEND_TIMEOUT = 500; // ms

    /**
     * Opens an audio file for playback with a specified buffer size.
     *
     * @param file The audio file to open.
     * @param bufferSize The size of the buffer for playback.
     * @throws FileNotFoundException if the file is not found.
     * @throws RuntimeException if there is an error during codec initialization or if the audio format is unsupported.
     */
    public void open(File file, int bufferSize) throws FileNotFoundException {
        logger.debug("Opening file...");
        validateFile(file); // Validate the file before processing
        String extension = getFileExtension(file.getName()); // Get file extension
        logger.debug("File extension: " + extension);
        if (extension.isEmpty()) return; // Exit if no valid extension
    
        try {
            // Decode the audio file and initialize data
            AudioDecodeResult result = decodeAudioFile(file, extension);
            logger.debug("Initializing audio data...");
            initializeAudioData(result, bufferSize);
            logger.debug("Initializing audio pipeline...");
            initializeAudioPipeline();
        } catch (AudioCodecCreationException e) {
            logger.error("Codec initialization failed.", e);
            throw new RuntimeException("Codec initialization failed.", e);
        } catch (AudioCodecException e) {
            logger.error("Codec exeception.", e);
            throw new RuntimeException(e); // Unknown codec exception
        } catch (UnsupportedAudioFormatException e) {
            logger.error("UnsupportedAudioFormatException.", e);
            throw new RuntimeException(e); // Audio format is not supported or invalid
        }
    }

    /**
     * Opens an audio file for playback with an automatic buffer size.
     *
     * @param filePath The audio file path to open.
     * @param bufferSize The size of the buffer for playback.
     * @throws FileNotFoundException if the file is not found.
     * @throws RuntimeException if there is an error during codec initialization or if the audio format is unsupported.
     */
    public void open(String filePath, int bufferSize) throws FileNotFoundException {
        open(new File(filePath), bufferSize);
    }

    /**
     * Opens an audio file for playback with an automatic buffer size.
     *
     * @param file The audio file to open.
     * @throws FileNotFoundException if the file is not found.
     * @throws RuntimeException if there is an error during codec initialization or if the audio format is unsupported.
     */
    public void open(File file) throws FileNotFoundException {
        open(file, -1); // Default to automatic buffer size
    }

    /**
     * Opens an audio file for playback with an automatic buffer size.
     *
     * @param filePath The audio file path to open.
     * @throws FileNotFoundException if the file is not found.
     * @throws RuntimeException if there is an error during codec initialization or if the audio format is unsupported.
     */
    public void open(String filePath) throws FileNotFoundException {
        open(new File(filePath));
    }
    
    /**
     * Validates that the provided file exists.
     *
     * @param file The file to validate.
     * @throws FileNotFoundException if the file does not exist.
     */
    protected void validateFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            logger.error("File not found.");
            throw new FileNotFoundException("File not found.");
        }
    }
    
    /**
     * Decodes the audio file based on its extension.
     *
     * @param file The audio file to decode.
     * @param extension The file extension.
     * @return The decoded audio data.
     * @throws FileNotFoundException if the file is not found.
     * @throws AudioCodecException if there is an error during codec processing.
     */
    protected AudioDecodeResult decodeAudioFile(File file, String extension) throws FileNotFoundException, AudioCodecException {
        AudioCodecInfo audioCodecInfo = AudioCodecs.fromExtension(extension);
        AudioCodec audioCodec = AudioCodecs.getCodec(audioCodecInfo);
        logger.debug("Selected audio codec: " + audioCodecInfo.getName());
        logger.debug("Decoding file...");
        return audioCodec.callDecode(new FileInputStream(file)); // Decode the file
    }
    
    /**
     * Initializes the audio data, including buffer size calculations.
     *
     * @param result The decoded audio result.
     * @param bufferSize The buffer size for playback.
     */
    protected void initializeAudioData(AudioDecodeResult result, int bufferSize) {
        byte[] data = result.getBytes();
        this.audioFormat = result.getAudioFormat();
        this.metadata = result.getTags();

        float[][] samples = SampleConverter.toSamples(data, audioFormat);
        
        // Set the buffer size based on user input or default to automatic calculation
        if (bufferSize == -1) { // automatic
            this.bufferSize = Math.max((int) ((audioFormat.getSampleRate() * audioFormat.getChannels()) * 0.128), 4096); // 128 ms
        } else if (bufferSize > 0) { // user-defined
            this.bufferSize = bufferSize;
        } else {
            // Handle predefined buffer sizes
            double mult;
            switch (bufferSize) {
                case BUFFER_SIZE_32MS: mult = 0.032; break;
                case BUFFER_SIZE_64MS: mult = 0.064; break;
                case BUFFER_SIZE_128MS: mult = 0.128; break;
                case BUFFER_SIZE_256MS: mult = 0.256; break;
                case BUFFER_SIZE_512MS: mult = 0.512; break;
                case BUFFER_SIZE_1024MS: mult = 1.024; break;
                case BUFFER_SIZE_2048MS: mult = 2.048; break;
                default: mult = 0.512; break;
            }
            this.bufferSize = Math.max((int) (audioFormat.getSampleRate() * audioFormat.getChannels() * mult), 4096);
        }

        // Set length and buffer the audio data
        this.length = data.length;
        logger.debug("Buffer size: " + this.bufferSize);
        this.audioData = AudioBufferizer.bufferizeSamples(samples, this.bufferSize);
        audioDataFragments = this.audioData.length;
        // Convert this.bufferSize in bytes, to multiplier for played audioData frgaments.
    }
    
    /**
     * Initializes the audio pipeline, including mixers and effects.
     *
     * @throws UnsupportedAudioFormatException if the audio format is unsupported.
     */
    protected void initializeAudioPipeline() throws UnsupportedAudioFormatException {
        mixerIn = new DataLine(audioFormat, 1);
        audioOut = new DataLine(audioFormat, 1);
        logger.debug("Data lines initialized.");
        
        attachAudioOutAction();
        
        this.mixer = new AudioMixer(AudioMixer.Mode.EVENT, audioFormat);
        logger.debug("Mixer initialized.");
        mixer.addInput(mixerIn);
        mixer.addOutput(audioOut);
        logger.debug("Data lines are attached to the mixer.");
        
        // Initialize speed change effect
        this.speedEffect = new ResamplerEffect(audioFormat);
        logger.debug("Speed changer initialized.");
        try {
            mixer.addEffect(speedEffect); // Add effect to mixer
            logger.debug("Speed changer is attached to the mixer.");
        } catch (UnsupportedAudioEffectException ignored) {}

        isOpen = true;
        isPlaying = false;
        
        loop = 0;
        loopCount = NO_LOOP;
    }

    /**
     * Attaches an action to the output data line of the mixer, which
     * increments the "played" counter each time audio data is sent to
     * the output line. This is used to track the total amount of audio
     * data that has been played.
     */
    protected void attachAudioOutAction() {
        audioOut.addDataLineListener(new DataLineAdapter() {
            @Override
            public void onReceive(DataLineEvent e) {
                played++;
            }
        });
    }

    /**
     * Sets the priority of the playback thread. This is useful for adjusting the
     * CPU priority of the thread in case of high CPU usage.
     * 
     * @param priority The thread priority to set.
     */
    public void setPlaybackThreadPriority(int priority) {
        if (playbackThread != null) {
            playbackThread.setPriority(priority);
            threadPriotity = priority; // Update the thread priority
        }
    }

    /**
     * Starts the playback of the audio.
     */
    public void start() {
        if (!isOpen || isPlaying) return;
        isPlaying = true;
        resetPosition();
    
        if (playbackThread == null || !playbackThread.isAlive()) { // Check thread status
            playbackThread = new Thread(this::playbackLoop, "Playback Thread-" + thisSoundInstance);
            playbackThread.setPriority(threadPriotity); // Adjust CPU priority
            playbackThread.start();
            logger.debug("Playback thread recreated and started.");
        }
        logger.debug("Playback started.");
    }

    /**
     * Stops the playback of the audio.
     */
    public void stop() {
        if (!isOpen) return;
        isPlaying = false;
        logger.debug("Playback stopped.");
    }

    /**
     * Returns the gain control for volume adjustments.
     *
     * @return The gain control.
     */
    public FloatControl getGainControl() {
        return mixer.getPostGainControl();
    }

    /**
     * Returns the pan control for stereo adjustments.
     *
     * @return The pan control.
     */
    public FloatControl getPanControl() {
        return mixer.getPanControl();
    }

    /**
     * Returns the speed control for speed adjustments.
     *
     * @return The speed control.
     */
    public FloatControl getSpeedControl() {
        return speedEffect.getSpeedControl();
    }

    @Override
    public List<AudioControl> getAllControls() {
        ArrayList<AudioControl> controls = new ArrayList<>();
        controls.add(getGainControl());
        controls.add(getPanControl());
        controls.add(getSpeedControl());
        return Collections.unmodifiableList(controls);
    }

    /**
     * Playback loop for continuous audio streaming.
     */
    protected void playbackLoop() {
        try {
            logger.debug("Playback loop started.");
            while (isPlaying && isOpen && !Thread.currentThread().isInterrupted() && played < audioDataFragments) {
                if (pendingSeeking) {
                    // Short sample position changes block the thread temporarily
                    Thread.sleep(1);
                    continue;
                }
                boolean success = false;
                if (offset <= 0) {
                    success = mixerIn.sendWithTimeout(audioData[played], SEND_TIMEOUT, TimeUnit.MILLISECONDS); // Send data to mixer
                } else {
                    int framesRemaining = bufferSize - offset;
                    float[][] audioData = new float[audioFormat.getChannels()][framesRemaining];
                    for (int i = 0; i < audioFormat.getChannels(); i++) {
                        System.arraycopy(this.audioData[played][i], offset, audioData[i], 0, framesRemaining);
                    }
                    offset = 0;
                    success = mixerIn.sendWithTimeout(audioData, SEND_TIMEOUT, TimeUnit.MILLISECONDS);
                }
                logger.debug("Buffer " + played + " of " + audioDataFragments + ", sended to the mixer.");

                if (!success) logger.debug("Audio send operation fail.");
                if (played > audioDataFragments - 1 && needLoop()) {
                    resetPosition();
                    logger.debug("Loop. Remaining loops: " + loop + "/" + loopCount + ".");
                }
            }
            Thread.sleep(AudioConverter.framesToMicroseconds(bufferSize / audioFormat.getFrameSize(), audioFormat) / 1000); // Wait for the last buffer to finish
            isPlaying = false; // Mark playback as stopped
            logger.debug("Playback loop ended.");

            // Loop playback when end is reached
            if (played >= audioDataFragments) {
                resetPosition();
            }
            
        } catch (InterruptedException e) {
            logger.error("Interrupted exception: ", e);
        }
    }

    /**
     * Checks if the playback loop should be continued.
     * 
     * @return true if the loop should be continued, false otherwise.
     */
    protected boolean needLoop() {
        if (played >= audioDataFragments - 1) {
            switch (loopCount) {
                case NO_LOOP: return false;
                case LOOP_CONTINUOUSLY: return true;
                default:
                    if (loop < loopCount) {
                        loop++;
                        return true;
                    } else {
                        return false;
                    }
            }
        } else {
            return false;
        }
    }

    /**
     * Adds an audio effect to the sound source.
     *
     * @param effect The effect to add.
     * @throws UnsupportedAudioEffectException if the effect type is not supported.
     */
    public void addEffect(AudioEffect effect) throws UnsupportedAudioEffectException {
        mixer.addEffect(effect);
    }

    /**
     * Removes an audio effect from the sound source.
     *
     * @param effect The effect to remove.
     */
    public void removeEffect(AudioEffect effect) {
        mixer.removeEffect(effect);
    }

    /**
     * Returns the list of audio effects applied to the sound source.
     * 
     * @return The list of audio effects.
     */
    public List<AudioEffect> getEffects() {
        return mixer.getEffects();
    }

    /**
     * Sets the number of loops for playback.
     * If the loop count is set to 0, the sound will not loop.
     * @param loopCount The number of loops to set.
     *                  Use NO_LOOP for no loops, or LOOP_CONTINUOUSLY for infinite loops.
     */
    public void setLoop(int loopCount) {
        this.loopCount = loopCount;
        if (loop > loopCount) {
            loop = 0;
        }
    }

    /**
     * Sets the sample position for playback.
     *
     * @param sample The sample position to set.
     */
    public void setSamplePosition(long sample) {
        if (!isOpen) return;
    
        if (sample < 0) sample = 0;
        if (sample >= length) sample = length - 1;
    
        int buffer = (int) (sample / bufferSize);
        int remaining = (int) (sample % bufferSize);
    
        pendingSeeking = true; // Block the playback thread temporarily
        logger.debug("Pending seeking...");

        played = Math.min(buffer, audioDataFragments - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        pendingSeeking = false; // Unlock the playback thread
        logger.debug("Playback thread unlocked.");
        logger.debug("Position: buff:" + played + ", offset:" + offset);
    }

    /**
     * Sets the playback position in microseconds.
     *
     * @param mcs The microsecond position.
     */
    public void setMicrosecondPosition(long mcs) {
        long sample = AudioConverter.microsecondsToSamples(mcs, audioFormat.getSampleRate());
        setSamplePosition(sample);
    }

    /**
     * Sets the playback position in modified microseconds.
     *
     * @param mcs The modified microsecond position.
     */
    public void setModifiedMicrosecondPosition(long mcs) {
        long sample = AudioConverter.microsecondsToSamples(mcs, (int)(audioFormat.getSampleRate() / speedEffect.getSpeedControl().getValue()));
        setSamplePosition(sample);
    }

    /**
     * Returns the current sample position based on played buffers and offset.
     * Adjusts for sample size to ensure accurate calculation.
     *
     * @return The current sample position.
     */
    public long getDelayedSamplePosition() {
        return (long) (played * bufferSize + offset);
    }

    /**
     * Returns the current sample position, based on the playback position timer.
     *
     * @return The current sample position (from the timer).
     */
    public long getSamplePosition() {
        return getDelayedSamplePosition();
    }

    /**
     * Returns the current position in microseconds, based on the playback timer.
     * 
     * @return The current position in microseconds.
     */
    public long getMicrosecondPosition() {
        return AudioConverter.samplesToMicrosecond(getSamplePosition(), audioFormat.getSampleRate());
    }

    /**
     * Returns the current position in microseconds, taking into account the modified speed effect.
     * The position is adjusted by the speed control value, which affects playback speed.
     * 
     * @return The current position in microseconds, with speed modifications applied.
     */
    public long getModifiedMicrosecondPosition() {
        return (long)((double)getMicrosecondPosition() * speedEffect.getSpeedControl().getValue());
    }

    /**
     * Returns the total length of the audio in frames.
     * The sample length is determined by dividing the total length in bytes by the sample size.
     * 
     * @return The total length of the audio in frames.
     */
    public long getSampleLength() {
        return length / audioFormat.getFrameSize();
    }

    /**
     * Returns the total length of the audio in microseconds.
     * The calculation takes the length in bytes and divides it by the byte rate of the audio format.
     * 
     * @return The total length of the audio in microseconds.
     */
    public long getMicrosecondLength() {
        return (long)((double)(length) / audioFormat.getByteRate() * 1_000_000);
    }

    /**
     * Returns the total length of the audio in microseconds, with the modified speed effect applied.
     * This method adjusts the length by the speed control value.
     * 
     * @return The total length of the audio in microseconds, with speed modifications applied.
     */
    public long getModifiedMicrosecondLength() {
        return (long)((double)(length) / speedEffect.getSpeedControl().getValue() / audioFormat.getByteRate() * 1_000_000);
    }

    /**
     * Returns the buffer size used for playback.
     * 
     * @return The buffer size in bytes.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Returns the audio format used by this sound source.
     * The audio format includes details such as sample rate, number of channels, and encoding.
     * 
     * @return The audio format.
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    /**
     * Returns whether the sound is currently playing.
     * 
     * @return True if the sound is playing, false otherwise.
     */
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Returns whether the sound source is open and ready for playback.
     * 
     * @return True if the sound source is open, false otherwise.
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Returns the output line for audio playback.
     * 
     * @return The {@link DataLine} representing the output line, or {@code null} if the sound source is not open.
     */
    public DataLine getOutputLine() {
        if (!isOpen) return null;
        return audioOut;
    }

    /**
     * Returns the mixer used for audio processing.
     * @return mixer The {@link AudioMixer} instance used for audio processing, or {@code null} if the sound source is not open.
     */
    public AudioMixer getMixer() {
        return mixer;
    }

    /**
     * Closes the sound source and cleans up resources.
     * Stops the playback if it is active and sets the sound source as closed.
     */
    @Override
    public void close() {
        if (!isOpen) return;
        if (isPlaying()) {
            stop(); // Stop playback
        }
        isOpen = false; // Mark the sound source as closed
        logger.debug("Cleanup...");

        if (mixerIn != null) mixerIn.close();
        if (audioOut != null) audioOut.close();
        if (mixer != null) mixer.close();
        if (metadata != null) {
            try {
                metadata.clear();
            } catch (UnsupportedOperationException e) {
                metadata = null;
            }
        }

        mixerIn = null;
        audioOut = null;
        mixer = null;
        audioData = null;
        // audioFormat = null;
        logger.debug("Cleanup completed.");
    }

    /**
     * Returns a string representation of the SoundSource object, providing details
     * about its current state, including instance number, open status, playing status,
     * total length, and buffer size.
     *
     * @return A string that represents the current state of the SoundSource.
     */
    @Override
    public String toString() {
        return String.format("SoundSource {Instance: %d, Open: %s, Playing: %s, Length: %d, BufferSize: %d}", thisSoundInstance, isOpen, isPlaying, length, bufferSize);
    }

    /**
     * Resets the position of the sound source to the beginning, and sets the played and 
     * offset values to 0. This is called when the sound source is closed, and is also used
     * by the start() method to reset the position before starting playback.
     */
    protected void resetPosition() {
        played = 0;
    }

    /**
     * Retrieves the file extension from a given file path.
     * 
     * @param filepath The path of the file whose extension is to be extracted.
     * @return The file extension as a string, or an empty string if no extension is found.
     */
    private static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1); // Extract and return the extension
        }
        return ""; // Return empty string if no extension is found
    }
}
