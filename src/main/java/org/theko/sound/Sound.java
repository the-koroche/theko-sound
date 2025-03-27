package org.theko.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecCreationException;
import org.theko.sound.codec.AudioCodecInfo;
import org.theko.sound.codec.AudioCodecNotFoundException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;
import org.theko.sound.codec.AudioTag;
import org.theko.sound.control.FloatController;
import org.theko.sound.effects.SpeedChangeEffect;

public class Sound implements AutoCloseable {
    private final AudioOutputLine aol;
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
    protected boolean enableAOLine;
    
    protected SpeedChangeEffect speedEffect;

    private static int soundInstances = 0;

    public Sound (boolean enableAOLine) throws SoundException {
        soundInstances++;
        this.enableAOLine = enableAOLine;
        try {
            if (enableAOLine) {
                this.aol = new AudioOutputLine();
            } else {
                this.aol = null;
            }
        } catch (AudioDeviceNotFoundException | AudioDeviceCreationException e) {
            throw new SoundException(e);
        }
    }

    public void open(File file) throws SoundException, FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found.");
        }
        String extension = getFileExtension(file.getName());
        if (extension.equals("")) {
            return; // Invalid file
        }
        try {
            AudioCodecInfo audioCodecInfo = AudioCodecs.fromExtension(extension);
            AudioCodec audioCodec = AudioCodecs.getCodec(audioCodecInfo);
            AudioDecodeResult result = audioCodec.decode(new FileInputStream(file));

            byte[] data = result.getBytes();
            this.audioFormat = result.getAudioFormat();
            this.metadata = result.getTags();
            this.bufferSize = Math.max((int)(audioFormat.getByteRate() * 0.25), 4096);
            if (this.aol != null) this.aol.open(null, audioFormat, bufferSize);

            this.length = data.length;
            this.audioData = AudioBufferizer.bufferize(data, audioFormat, bufferSize);
            playbackThread = new Thread(this::playbackLoop, "Playback Thread-" + soundInstances);

            mixerIn = new DataLine(audioFormat);
            audioOut = new DataLine(audioFormat);

            this.mixer = new AudioMixer(AudioMixer.Mode.EVENT, audioFormat);
            mixer.addInput(mixerIn);
            mixer.addOutput(audioOut);
            if (this.aol != null) aol.setInput(audioOut);

            this.speedEffect = new SpeedChangeEffect(audioFormat);
            mixer.addEffect(speedEffect);

            isOpen = true;
            isPlaying = false;
        } catch (AudioCodecNotFoundException e) {
            throw new SoundException("Audio codec for this type of file is missing.", e);
        } catch (AudioCodecCreationException e) {
            throw new SoundException("Codec initialization failed.", e);
        } catch (FileNotFoundException ignored) { 
        } catch (Exception e) {
            throw new SoundException(e);
        }
    }

    public void start() {
        if (!isOpen || isPlaying) return;
        isPlaying = true;
        if (this.aol != null) aol.start();
    
        if (playbackThread == null || !playbackThread.isAlive()) { // Проверка на null и isAlive()
            playbackThread = new Thread(this::playbackLoop, "Playback Thread-" + soundInstances);
            playbackThread.start();
        }
    }

    public void stop() {
        if (!isOpen) return;
        isPlaying = false;
        if (this.aol != null) aol.stop();
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
            }
        }
        if (played >= audioData.length) {
            played = 0;
        }
    }

    public void setFramePosition(long frame) {
        if (!isOpen) return;
    
        if (frame < 0) frame = 0;
        if (frame >= length) frame = length - 1;
    
        int buffer = (int) (frame / bufferSize);
        int remaining = (int) (frame % bufferSize);
    
        stop();
        if (this.aol != null) aol.flush();

        played = Math.min(buffer, audioData.length - 1);
        offset = Math.min(remaining, bufferSize - 1);
    
        start();
    }

    public void setMicrosecondPosition(long mcs) {
        int frameSize = audioFormat.getFrameSize();
        long sample = (long)((double)(mcs) / 1_000_000 * audioFormat.getByteRate()) / audioFormat.getFrameSize();
        setFramePosition(sample * frameSize);
    }

    public long getFramePosition() {
        if (!isOpen) {
            return -1;
        }
        return (bufferSize * played + (enableAOLine ? aol.available() : 0) + offset) / audioFormat.getFrameSize();
    }

    public long getMicrosecondPosition() {
        return (long)(((double)(getFramePosition()) / audioFormat.getByteRate()) * 1_000_000);
    }

    public long getFrameLength() {
        return length / audioFormat.getFrameSize();
    }

    public long getMicrosecondLength() {
        return (long)((double)(length) / audioFormat.getByteRate() * 1_000_000);
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

    @Override
    public void close() {
        stop();
        if (this.aol != null) aol.close();
    }

    private static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1);
        }
        return ""; // Return empty string if no extension
    }
}
