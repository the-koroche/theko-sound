package org.theko.sound;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.direct.AudioDeviceException;
import org.theko.sound.event.AudioLineEvent;
import org.theko.sound.event.AudioOutputLineAdapter;

/**
 * The {@code SoundPlayer} class extends {@link SoundSource} and provides functionality
 * for playing audio files. It manages the audio output line, handles playback operations,
 * and allows for seeking and controlling the playback position.
 * 
 * <p>This class is designed to work with an {@link AudioOutputLine} and an {@link AudioPort}
 * to output audio. It supports opening audio files, initializing the audio pipeline,
 * starting and stopping playback, seeking to specific frame positions, and releasing
 * resources when playback is complete.
 * 
 * <p>Key features include:
 * <ul>
 *   <li>Opening audio files with specified buffer sizes and audio ports.</li>
 *   <li>Initializing the audio pipeline with support for custom audio formats.</li>
 *   <li>Starting and stopping playback with synchronized methods.</li>
 *   <li>Seeking to specific frame positions while ensuring alignment with buffer sizes.</li>
 *   <li>Releasing resources by closing the audio output line and associated components.</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * SoundPlayer player = new SoundPlayer();
 * player.open(new File("audio.mp3"), 1024, new AudioPort());
 * player.start();
 * player.setFramePosition(5000);
 * player.stop();
 * player.close();
 * }</pre>
 * 
 * <p>Note: This class relies on external dependencies such as {@link AudioOutputLine},
 * {@link AudioPort}, and {@link Logger}. Ensure these dependencies are properly configured
 * in your project.
 * 
 * @see SoundSource
 * @see AudioOutputLine
 * @see AudioPort
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class SoundPlayer extends SoundSource {
    private static final Logger logger = LoggerFactory.getLogger(SoundPlayer.class);

    private final AudioOutputLine aol;  // Audio output line for playing sound
    protected AudioPort audioPort;      // Audio port to output the audio

    /**
     * Constructs a new {@link SoundPlayer} and initializes the {@link AudioOutputLine}.
     * 
     * @throws RuntimeException if there is an error initializing the audio output line.
     */
    public SoundPlayer() {
        aol = new AudioOutputLine();
        logger.debug("AOL created.");
    }

    /**
     * Opens the audio file and prepares the playback with a specified buffer size and audio port.
     * 
     * @param file The audio file to be opened.
     * @param bufferSize The size of the buffer for playback.
     * @param port The audio output port.
     * @throws FileNotFoundException if the file cannot be found.
     */
    public void open(File file, int bufferSize, AudioPort port) throws FileNotFoundException {
        this.audioPort = port;
        super.open(file, bufferSize);
    }

    /**
     * Opens the audio file and prepares the playback with a specified buffer size and audio port.
     * 
     * @param filePath The audio file path to be opened.
     * @param bufferSize The size of the buffer for playback.
     * @param port The audio output port.
     * @throws FileNotFoundException if the file cannot be found.
     */
    public void open(String filePath, int bufferSize, AudioPort port) throws FileNotFoundException {
        this.audioPort = port;
        super.open(filePath, bufferSize);
    }

    /**
     * Initializes the audio pipeline for playback, including setting up the audio output line.
     * 
     * @throws UnsupportedAudioFormatException if the audio format is not supported.
     * @throws RuntimeException if there is an error during the initialization of the audio output line.
     */
    @Override
    protected void initializeAudioPipeline() throws UnsupportedAudioFormatException {
        super.initializeAudioPipeline();  // Initialize the base class pipeline

        try {
            // Initialize the audio output line with the given port and audio format
            aol.open(audioPort, audioFormat, bufferSize);
            logger.debug("AOL opened.");
            aol.setInputLine(getOutputLine());  // Set the output line of the sound source to the input of the output line
            logger.debug("AOL input attached to the mixer output.");
            aol.addAudioOutputLineListener(new AudioOutputLineAdapter() {
                @Override
                public void onWrite(AudioLineEvent e) {
                    played++;
                }
            });
            logger.debug("AOL on-writed action is attached.");
        } catch (AudioDeviceException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);  // Propagate the error if setup fails
        }
    }

    @Override
    protected void attachAudioOutAction() {
        // Do nothing
    }

    /**
     * Starts playback of the sound.
     * It calls the base class start method and starts the audio output line.
     */
    @Override
    public synchronized void start() {
        aol.start();    // Start the audio output line
        super.start();  // Start the base class playback
    }

    public synchronized void startAndWait() {
        start();
        try {
            Thread.sleep(getModifiedMicrosecondLength() / 1000);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Stops the playback of the sound.
     * It calls the base class stop method and stops the audio output line.
     */
    @Override
    public synchronized void stop() {
        super.stop();   // Stop the base class playback
        aol.stop();     // Stop the audio output line
    }

    /**
     * Sets the frame position for playback.
     *
     * @param frame The frame position to set.
     */
    public void setSamplePosition(long sample) {
        if (!isOpen) return;
    
        if (sample < 0) sample = 0;
        if (sample >= length) sample = length - 1;
    
        int buffer = (int) (sample / bufferSize);
        int remaining = (int) (sample % bufferSize);
    
        pendingSeeking = true; // Block the playback thread temporarily
        logger.debug("Pending seeking...");
        aol.flush();

        played = Math.min(buffer, audioDataFragments - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        pendingSeeking = false; // Unlock the playback thread
        logger.debug("Playback thread unlocked.");
        logger.debug("Position: buff:" + played + ", offset:" + offset);
    }

    /**
     * Closes the sound player and releases all resources.
     * Stops playback and closes the audio output line.
     */
    @Override
    public synchronized void close() {
        super.close();  // Close the base class resources
        aol.close();    // Close the audio output line
        logger.debug("AOL closed.");
    }
}
