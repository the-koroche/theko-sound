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

public class SoundSource implements AutoCloseable {
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

    private static int soundInstances = 0;
    private final int thisSoundInstance = soundInstances;

    public static final int AUTO_BUFFER_SIZE = -1;
    public static final int BUFFER_SIZE_32MS = -10;
    public static final int BUFFER_SIZE_64MS = -11;
    public static final int BUFFER_SIZE_128MS = -12;
    public static final int BUFFER_SIZE_256MS = -13;
    public static final int BUFFER_SIZE_512MS = -14;
    public static final int BUFFER_SIZE_1024MS = -15;
    public static final int BUFFER_SIZE_2048MS = -16;

    public SoundSource () {
        soundInstances++;
    }

    public void open(File file, int bufferSize) throws FileNotFoundException {
        validateFile(file);
        String extension = getFileExtension(file.getName());
        if (extension.isEmpty()) return;
    
        try {
            AudioDecodeResult result = decodeAudioFile(file, extension);
            initializeAudioData(result, bufferSize);
            initializeAudioPipeline();
        } catch (AudioCodecCreationException e) {
            throw new RuntimeException("Codec initialization failed.", e);
        } catch (AudioCodecException e) {
            throw new RuntimeException(e); // Unknown codec exception
        } catch (UnsupportedAudioFormatException e) {
            throw new RuntimeException(e); // Audio format is not supported by audio device, or invalid
        }
    }

    public void open(File file) throws FileNotFoundException {
        open(file, -1);
    }
    
    protected void validateFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found.");
        }
    }
    
    protected AudioDecodeResult decodeAudioFile(File file, String extension) throws FileNotFoundException, AudioCodecException {
        AudioCodecInfo audioCodecInfo = AudioCodecs.fromExtension(extension);
        AudioCodec audioCodec = AudioCodecs.getCodec(audioCodecInfo);
        return audioCodec.decode(new FileInputStream(file));
    }
    
    protected void initializeAudioData(AudioDecodeResult result, int bufferSize) {
        byte[] data = result.getBytes();
        this.audioFormat = result.getAudioFormat();
        this.metadata = result.getTags();
        if (bufferSize == -1) { // automatic
            this.bufferSize = Math.max((int) (this.audioFormat.getByteRate() * 0.128), 4096); // 128 ms
        } else if (bufferSize > 0) { // user-defined
            this.bufferSize = bufferSize;
        } else {
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
        this.length = data.length;
        this.audioData = AudioBufferizer.bufferize(data, this.audioFormat, this.bufferSize);
    }
    
    protected void initializeAudioPipeline() throws UnsupportedAudioFormatException {
        mixerIn = new DataLine(audioFormat);
        audioOut = new DataLine(audioFormat);
        
        this.mixer = new AudioMixer(AudioMixer.Mode.EVENT, audioFormat);
        mixer.addInput(mixerIn);
        mixer.addOutput(audioOut);
        
        this.speedEffect = new SpeedChangeEffect(audioFormat);
        try {
            mixer.addEffect(speedEffect);
        } catch (UnsupportedAudioEffectException ignored) {}
        
        isOpen = true;
        isPlaying = false;
    }

    public void start() {
        if (!isOpen || isPlaying) return;
        isPlaying = true;
    
        if (playbackThread == null || !playbackThread.isAlive()) { // Check on null and isAlive()
            playbackThread = new Thread(this::playbackLoop, "Playback Thread-" + thisSoundInstance);
            playbackThread.setPriority(Thread.MAX_PRIORITY - 1); // not the maximum perfomance for audio, but allowing other processes to use CPU.
            playbackThread.start();
        }
    }

    public void stop() {
        if (!isOpen) return;
        isPlaying = false;
    }

    public FloatController getGainController() {
        return mixer.getPostGainController();
    }

    public FloatController getPanController() {
        return mixer.getPanController();
    }

    public FloatController getSpeedController() {
        return speedEffect.getSpeedController();
    }

    protected void playbackLoop() {
        while (isPlaying && isOpen && !Thread.currentThread().isInterrupted() && played < audioData.length) {
            try {
                if (pendingFrameSeek) {
                     // for short time frame position change, and thread blocking
                    Thread.sleep(1);
                    continue;
                }
                if (offset <= 0) {
                    mixerIn.sendWithTimeout(audioData[played], 500, TimeUnit.MILLISECONDS);
                } else {
                    byte[] audioData = new byte[bufferSize - offset];
                    System.arraycopy(this.audioData[played], offset, audioData, 0, audioData.length);
                    offset = 0;
                    mixerIn.sendWithTimeout(audioData, 500, TimeUnit.MILLISECONDS);
                }
                played++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                throw new PlaybackException(e);
            }
        }
        if (played >= audioData.length) {
            played = 0;
        }
    }

    public void addEffect(AudioEffect effect) throws UnsupportedAudioEffectException {
        mixer.addEffect(effect);
    }

    public void setFramePosition(long frame) {
        if (!isOpen) return;
    
        if (frame < 0) frame = 0;
        if (frame >= length) frame = length - 1;
    
        int buffer = (int) (frame / bufferSize);
        int remaining = (int) (frame % bufferSize);
    
        pendingFrameSeek = true; // block playback thread

        played = Math.min(buffer, audioData.length - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        pendingFrameSeek = false; // unlock playback thread
    }

    public void setMicrosecondPosition(long mcs) {
        int frameSize = audioFormat.getFrameSize();
        long sample = (long)((double)(mcs) / 1_000_000 * audioFormat.getByteRate()) / audioFormat.getFrameSize();
        setFramePosition(sample * frameSize);
    }

    public void setModifiedMicrosecondPosition(long mcs) {
        int frameSize = audioFormat.getFrameSize();
        long sample = (long)((double)(mcs) / speedEffect.getSpeedController().getValue() / 1_000_000 * audioFormat.getByteRate()) / audioFormat.getFrameSize();
        setFramePosition(sample * frameSize);
    }

    public long getFramePosition() {
        if (!isOpen) {
            return -1;
        }
        return (bufferSize * played + offset) / audioFormat.getFrameSize();
    }

    public long getMicrosecondPosition() {
        return (long)(((double)(getFramePosition() * audioFormat.getFrameSize()) / audioFormat.getByteRate()) * 1_000_000);
    }

    public long getModifiedMicrosecondPosition() {
        return (long)(((double)(getFramePosition() * audioFormat.getFrameSize()) / speedEffect.getSpeedController().getValue() / audioFormat.getByteRate()) * 1_000_000);
    }

    public long getFrameLength() {
        return length / audioFormat.getFrameSize();
    }

    public long getMicrosecondLength() {
        return (long)((double)(length) / audioFormat.getByteRate() * 1_000_000);
    }

    public long getModifiedMicrosecondLength() {
        return (long)((double)(length) / speedEffect.getSpeedController().getValue() / audioFormat.getByteRate() * 1_000_000);
    }

    public DataLine getOutputLine() {
        if (!isOpen) return null;
        return audioOut;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() {
        stop();
    }

    private static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1);
        }
        return ""; // Return empty string if no extension
    }
}
