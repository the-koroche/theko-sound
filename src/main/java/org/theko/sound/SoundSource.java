package org.theko.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecCreationException;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.control.FloatController;
import org.theko.sound.effects.SpeedChangeEffect;

/**
 * SoundSource represents an audio file that can be opened, played, and controlled. 
 * It handles the playback, buffering, and various effects of the audio data.
 */
public class SoundSource implements AutoCloseable {

    // Audio properties
    protected AudioMixer mixer;
    protected AudioFormat audioFormat;
    protected byte[][] audioData;
    protected int bufferSize;
    protected List<AudioTag> metadata;
    private Thread playbackThread;
    protected boolean isPlaying;
    protected boolean isOpen;
    protected int played;
    protected int offset;
    protected long length;

    private DataLine mixerIn, audioOut;
    protected boolean pendingFrameSeek = false;
    
    protected SpeedChangeEffect speedEffect;

    // Instance counting
    private static int soundInstances = 0;
    private final int thisSoundInstance = soundInstances;

    // Predefined buffer sizes
    public static final int AUTO_BUFFER_SIZE = -1;
    public static final int BUFFER_SIZE_32MS = -10;
    public static final int BUFFER_SIZE_64MS = -11;
    public static final int BUFFER_SIZE_128MS = -12;
    public static final int BUFFER_SIZE_256MS = -13;
    public static final int BUFFER_SIZE_512MS = -14;
    public static final int BUFFER_SIZE_1024MS = -15;
    public static final int BUFFER_SIZE_2048MS = -16;

    /**
     * Constructor for SoundSource. Increments soundInstances counter.
     */
    public SoundSource () {
        soundInstances++;
    }

    /**
     * Opens an audio file for playback with a custom buffer size.
     *
     * @param file The audio file to open.
     * @param bufferSize The size of the buffer for playback.
     * @throws FileNotFoundException if the file is not found.
     */
    public void open(File file, int bufferSize) throws FileNotFoundException {
        validateFile(file); // Validate the file before processing
        String extension = getFileExtension(file.getName()); // Get file extension
        if (extension.isEmpty()) return; // Exit if no valid extension
    
        try {
            // Decode the audio file and initialize data
            AudioDecodeResult result = decodeAudioFile(file, extension);
            initializeAudioData(result, bufferSize);
            initializeAudioPipeline();
        } catch (AudioCodecCreationException e) {
            throw new RuntimeException("Codec initialization failed.", e);
        } catch (AudioCodecException e) {
            throw new RuntimeException(e); // Unknown codec exception
        } catch (UnsupportedAudioFormatException e) {
            throw new RuntimeException(e); // Audio format is not supported or invalid
        }
    }

    /**
     * Opens an audio file for playback with an automatic buffer size.
     *
     * @param file The audio file to open.
     * @throws FileNotFoundException if the file is not found.
     */
    public void open(File file) throws FileNotFoundException {
        open(file, -1); // Default to automatic buffer size
    }
    
    /**
     * Validates that the provided file exists.
     *
     * @param file The file to validate.
     * @throws FileNotFoundException if the file does not exist.
     */
    protected void validateFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
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
        return audioCodec.decode(new FileInputStream(file)); // Decode the file
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
        
        // Set the buffer size based on user input or default to automatic calculation
        if (bufferSize == -1) { // automatic
            this.bufferSize = Math.max((int) (this.audioFormat.getByteRate() * 0.128), 4096); // 128 ms
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
            this.bufferSize = Math.max((int) (this.audioFormat.getByteRate() * mult), 4096);
        }

        // Set length and buffer the audio data
        this.length = data.length;
        this.audioData = AudioBufferizer.bufferize(data, this.audioFormat, this.bufferSize);
    }
    
    /**
     * Initializes the audio pipeline, including mixers and effects.
     *
     * @throws UnsupportedAudioFormatException if the audio format is unsupported.
     */
    protected void initializeAudioPipeline() throws UnsupportedAudioFormatException {
        mixerIn = new DataLine(audioFormat);
        audioOut = new DataLine(audioFormat);
        
        this.mixer = new AudioMixer(AudioMixer.Mode.EVENT, audioFormat);
        mixer.addInput(mixerIn);
        mixer.addOutput(audioOut);
        
        // Initialize speed change effect
        this.speedEffect = new SpeedChangeEffect(audioFormat);
        try {
            mixer.addEffect(speedEffect); // Add effect to mixer
        } catch (UnsupportedAudioEffectException ignored) {}

        isOpen = true;
        isPlaying = false;
    }

    /**
     * Starts the playback of the audio.
     */
    public void start() {
        if (!isOpen || isPlaying) return;
        isPlaying = true;
    
        if (playbackThread == null || !playbackThread.isAlive()) { // Check thread status
            playbackThread = new Thread(this::playbackLoop, "Playback Thread-" + thisSoundInstance);
            playbackThread.setPriority(Thread.MAX_PRIORITY - 1); // Adjust CPU priority
            playbackThread.start();
        }
    }

    /**
     * Stops the playback of the audio.
     */
    public void stop() {
        if (!isOpen) return;
        isPlaying = false;
    }

    /**
     * Returns the gain controller for volume adjustments.
     *
     * @return The gain controller.
     */
    public FloatController getGainController() {
        return mixer.getPostGainController();
    }

    /**
     * Returns the pan controller for stereo adjustments.
     *
     * @return The pan controller.
     */
    public FloatController getPanController() {
        return mixer.getPanController();
    }

    /**
     * Returns the speed controller for speed adjustments.
     *
     * @return The speed controller.
     */
    public FloatController getSpeedController() {
        return speedEffect.getSpeedController();
    }

    /**
     * Playback loop for continuous audio streaming.
     */
    protected void playbackLoop() {
        while (isPlaying && isOpen && !Thread.currentThread().isInterrupted() && played < audioData.length) {
            try {
                if (pendingFrameSeek) {
                    // Short frame position changes block the thread temporarily
                    Thread.sleep(1);
                    continue;
                }
                if (offset <= 0) {
                    mixerIn.sendWithTimeout(audioData[played], 500, TimeUnit.MILLISECONDS); // Send data to mixer
                } else {
                    byte[] audioData = new byte[bufferSize - offset];
                    System.arraycopy(this.audioData[played], offset, audioData, 0, audioData.length); // Handle offset
                    offset = 0;
                    mixerIn.sendWithTimeout(audioData, 500, TimeUnit.MILLISECONDS);
                }
                played++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new PlaybackException(e); // Handle playback errors
            }
        }

        // Loop playback when end is reached
        if (played >= audioData.length) {
            played = 0;
        }
    }

    /**
     * Adds an audio effect to the sound source.
     *
     * @param effect The effect to add.
     * @throws UnsupportedAudioEffectException if the effect is unsupported.
     */
    public void addEffect(AudioEffect effect) throws UnsupportedAudioEffectException {
        mixer.addEffect(effect);
    }

    /**
     * Sets the frame position for playback.
     *
     * @param frame The frame position to set.
     */
    public void setFramePosition(long frame) {
        if (!isOpen) return;
    
        if (frame < 0) frame = 0;
        if (frame >= length) frame = length - 1;
    
        int buffer = (int) (frame / bufferSize);
        int remaining = (int) (frame % bufferSize);
    
        pendingFrameSeek = true; // Block the playback thread temporarily

        played = Math.min(buffer, audioData.length - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        pendingFrameSeek = false; // Unlock the playback thread
    }

    /**
     * Sets the playback position in microseconds.
     *
     * @param mcs The microsecond position.
     */
    public void setMicrosecondPosition(long mcs) {
        int frameSize = audioFormat.getFrameSize();
        long sample = (long)((double)(mcs) / 1_000_000 * audioFormat.getByteRate()) / audioFormat.getFrameSize();
        setFramePosition(sample * frameSize);
    }

    /**
     * Sets the playback position in modified microseconds.
     *
     * @param mcs The modified microsecond position.
     */
    public void setModifiedMicrosecondPosition(long mcs) {
        int frameSize = audioFormat.getFrameSize();
        long sample = (long)((double)(mcs) / speedEffect.getSpeedController().getValue() / 1_000_000 * audioFormat.getByteRate()) / audioFormat.getFrameSize();
        setFramePosition(sample * frameSize);
    }

    /**
     * Returns the current frame position.
     *
     * @return The current frame position.
     */
    public long getFramePosition() {
        return (long)played * bufferSize + offset;
    }

    /**
     * Returns the current position in microseconds, based on the current frame position.
     * The calculation is based on the frame size and the byte rate of the audio format.
     * 
     * @return The current position in microseconds.
     */
    public long getMicrosecondPosition() {
        return (long)(((double)(getFramePosition() * audioFormat.getFrameSize()) / audioFormat.getByteRate()) * 1_000_000);
    }

    /**
     * Returns the current position in microseconds, taking into account the modified speed effect.
     * The position is adjusted by the speed controller value, which affects playback speed.
     * 
     * @return The current position in microseconds, with speed modifications applied.
     */
    public long getModifiedMicrosecondPosition() {
        return (long)(((double)(getFramePosition() * audioFormat.getFrameSize()) / speedEffect.getSpeedController().getValue() / audioFormat.getByteRate()) * 1_000_000);
    }

    /**
     * Returns the total length of the audio in frames.
     * The frame length is determined by dividing the total length in bytes by the frame size.
     * 
     * @return The total length of the audio in frames.
     */
    public long getFrameLength() {
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
     * This method adjusts the length by the speed controller value.
     * 
     * @return The total length of the audio in microseconds, with speed modifications applied.
     */
    public long getModifiedMicrosecondLength() {
        return (long)((double)(length) / speedEffect.getSpeedController().getValue() / audioFormat.getByteRate() * 1_000_000);
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
     * Closes the sound source and cleans up resources.
     * Stops the playback if it is active and sets the sound source as closed.
     */
    @Override
    public void close() {
        if (!isOpen) return;
        stop(); // Stop playback
        isOpen = false; // Mark the sound source as closed
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
