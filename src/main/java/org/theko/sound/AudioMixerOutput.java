package org.theko.sound;

import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendNotFoundException;

public class AudioMixerOutput implements AutoCloseable {
    private final AudioOutputLayer layer;
    private AudioMixer mixer;
    private boolean isStarted = false;

    public AudioMixerOutput (AudioOutputLayer layer) {
        if (layer == null) {
            throw new IllegalArgumentException("Layer cannot be null.");
        }
        this.layer = layer;
    }

    public AudioMixerOutput () throws AudioBackendCreationException, AudioBackendNotFoundException {
        this(new AudioOutputLayer());
    }

    public void open (AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        layer.open(audioFormat);
        startOnce();
    }

    public void open (AudioPort port, AudioFormat format) throws AudioBackendException {
        layer.open(port, format);
        startOnce();
    }

    public void open (AudioPort port, AudioFormat format, int bufferSize) throws AudioBackendException {
        layer.open(port, format, bufferSize);
        startOnce();
    }

    private void startOnce () throws AudioBackendException {
        if (!isStarted && layer.isOpen() && mixer != null) {
            start();
            isStarted = true;
        }
    }

    public void start () throws AudioBackendException {
        layer.start();
    }

    public void stop () throws AudioBackendException {
        layer.stop();
    }

    public void close () {
        isStarted = false;
        layer.close();
    }

    public void setMixer (AudioMixer mixer) {
        if (mixer == null) {
            throw new IllegalArgumentException("Mixer cannot be null.");
        }
        this.mixer = mixer;
        layer.setRootNode(mixer);
        startOnce();
    }

    public AudioMixer getMixer () {
        return mixer;
    }

    public AudioOutputLayer getAudioOutputLayer () {
        return layer;
    }
}
