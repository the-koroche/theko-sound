package org.theko.sound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.properties.AudioSystemProperties;
import org.theko.sound.utility.ThreadUtilities;

public class AudioOutputLine {
    
    private static final Logger logger = LoggerFactory.getLogger(AudioOutputLine.class);

    private final AudioOutputBackend aob;
    private AudioFormat audioFormat;
    private AudioNode rootNode;

    private Thread processingThread;
    private int bufferSize = 2048;

    public AudioOutputLine (AudioOutputBackend aob) {
        this.aob = aob;
        logger.debug("Created audio output line: " + aob);
    }

    public AudioOutputLine () throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(AudioBackends.getOutputBackend(AudioBackends.getPlatformBackend()));
    }

    public void open (AudioPort port, AudioFormat audioFormat, int bufferSizeInSamples) throws AudioBackendCreationException, AudioBackendNotFoundException {
        try {
            AudioPort targetPort = (port == null ? aob.getDefaultPort(AudioFlow.OUT, audioFormat).get() : port);
            aob.open(targetPort, audioFormat, bufferSizeInSamples * audioFormat.getFrameSize());
            this.audioFormat = audioFormat;
            this.bufferSize = bufferSizeInSamples;
            logger.debug("Opened audio output line with {} port, {} format, and {} buffer size", targetPort, audioFormat, bufferSizeInSamples);
        } catch (AudioBackendException | AudioPortsNotFoundException ex) {
            throw new AudioBackendCreationException("Failed to open audio output line.", ex);
        } catch (UnsupportedAudioFormatException ex) {
            throw new AudioBackendCreationException("Unsupported audio format.", ex);
        }
    }

    public void open (AudioPort port, AudioFormat audioFormat) throws AudioBackendCreationException, AudioBackendNotFoundException {
        this.open(port, audioFormat, 2048);
    }

    public void open (AudioFormat audioFormat) throws AudioBackendCreationException, AudioBackendNotFoundException, AudioPortsNotFoundException, UnsupportedAudioFormatException {
        this.open(aob.getDefaultPort(AudioFlow.OUT, audioFormat).get(), audioFormat);
    }

    public boolean isOpen () {
        return aob.isOpen();
    }

    public void start () throws AudioBackendException {
        aob.start();
        processingThread = ThreadUtilities.createThread(
            "AudioOutputLine-ProcessingThread",
            AudioSystemProperties.AUDIO_OUTPUT_LINE_THREAD_TYPE,
            AudioSystemProperties.AUDIO_OUTPUT_LINE_THREAD_PRIORITY,
            () -> {
                try {
                    process();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.debug("Audio output line processing thread interrupted");
                } catch (Exception e) {
                    logger.error("Error in audio output line processing thread", e);
                }
            }
        );
        processingThread.setDaemon(true);
        processingThread.start();
        logger.debug("Started audio output line");
    }

    public void stop () throws AudioBackendException {
        aob.stop();
        processingThread.interrupt();
        logger.debug("Stopped audio output line");
    }

    public void flush () throws AudioBackendException {
        aob.flush();
    }

    public void drain () throws AudioBackendException {
        aob.drain();
    }

    public int write (byte[] data, int offset, int length) throws AudioBackendException {
        if (!isOpen()) {
            throw new AudioBackendException("Cannot write. Backend is not open.");
        }
        if (rootNode == null) {
            return aob.write(data, offset, length);
        } else {
            logger.warn("The root node is not null, so the audio data will not be written.");
            return -1;
        }
    }

    public int available () {
        return aob.available();
    }

    public int getLineBufferSize () {
        return bufferSize;
    }

    public int getBackendBufferSize () {
        return aob.getBufferSize();
    }

    public void setBufferSize (int bufferSize) {
        // This method doesn't update the audio output backend's buffer size
        this.bufferSize = bufferSize;
    }

    public void setRootNode (AudioNode rootNode) {
        this.rootNode = rootNode;
    }

    public void close () {
        aob.close();
    }

    public AudioFormat getAudioFormat () {
        return audioFormat;
    }

    public long getFramePosition () {
        return aob.getFramePosition();
    }

    public long getMicrosecondPosition () {
        return aob.getMicrosecondPosition();
    }

    public long getMicrosecondLatency () {
        return aob.getMicrosecondLatency();
    }

    public AudioPort getCurrentAudioPort () {
        return aob.getCurrentAudioPort();
    }

    private void process () throws InterruptedException {
        float[][] sampleBuffer = new float[audioFormat.getChannels()][bufferSize];
        long bufferMs = AudioConverter.samplesToMicrosecond(sampleBuffer, audioFormat.getSampleRate()) / 1000;
        while (!processingThread.isInterrupted()) {
            if (rootNode == null) {
                Thread.sleep(bufferMs);
            }
            rootNode.render(sampleBuffer, audioFormat.getSampleRate(), bufferSize);
            if (sampleBuffer[0].length != bufferSize) {
                throw new RuntimeException(new LengthMismatchException());
            }
            aob.write(SampleConverter.fromSamples(sampleBuffer, audioFormat), 0, bufferSize * audioFormat.getFrameSize());
        }
    }

    public AudioOutputBackend getAudioOutputBackend () {
        return aob;
    }
}
