package org.theko.sound;

import java.io.File;
import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.direct.AudioDeviceException;

/**
 * A subclass of {@link SoundSource} for audio playback using an {@link AudioOutputLine}.
 * Provides methods to control playback, including starting, stopping, and setting the position.
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
        try {
            aol = new AudioOutputLine();
            logger.debug("AOL created.");
        } catch (AudioDeviceNotFoundException | AudioDeviceCreationException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);  // Propagate the exception if device setup fails
        }
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
            aol.setInput(getOutputLine());  // Set the output line of the sound source to the input of the output line
            logger.debug("AOL input attached to the mixer output.");
            aol.setOnWritedAction(() -> {
                played++;
            });
            logger.debug("AOL on-writed action is attached.");
        } catch (AudioDeviceException | AudioPortsNotFoundException | UnsupportedAudioFormatException e) {
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
     * Sets the current frame position of the playback.
     * This adjusts the frame position and ensures the correct alignment with buffer size.
     * 
     * @param frame The frame position to seek to.
     */
    @Override
    public synchronized void setFramePosition(long frame) {
        if (!isOpen) return;

        frame = frame / audioFormat.getFrameSize() * audioFormat.getFrameSize();  // Align frame to frame size
    
        if (frame < 0) frame = 0;  // Ensure the frame position is not negative
        if (frame >= length) frame = length - 1;  // Ensure the frame position does not exceed the audio length
    
        int buffer = (int) (frame / bufferSize);  // Calculate which buffer the frame is in
        int remaining = (int) (frame % bufferSize);  // Calculate the remaining offset within the buffer
    
        // Stop playback temporarily and seek to the new position
        pendingFrameSeek = true;
        logger.debug("Pending frame seeking...");
        aol.flush();  // Flush the audio output line to reset playback position
        logger.debug("AOL flushed.");

        played = Math.min(buffer, audioData.length - 1);  // Adjust the played buffer index
        offset = Math.min(remaining, bufferSize - 1);  // Adjust the offset within the buffer
    
        pendingFrameSeek = false;  // Unlock playback after seeking
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
