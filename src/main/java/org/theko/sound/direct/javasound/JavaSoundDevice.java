package org.theko.sound.direct.javasound;

import java.util.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.direct.AudioDevice;
import org.theko.sound.direct.AudioDeviceType;
import org.theko.sound.direct.AudioInputDevice;
import org.theko.sound.direct.AudioOutputDevice;

@AudioDeviceType(name = "JavaSound", version = "0.3b")
public class JavaSoundDevice implements AudioDevice {
    @Override
    public Collection<AudioPort> getAllPorts() {
        List<AudioPort> ports = new ArrayList<>();

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            boolean hasOutput = hasOutputLines(mixer);
            boolean hasInput = hasInputLines(mixer);

            if (hasOutput) {
                ports.add(createPort(AudioFlow.OUT, info));
            }

            if (hasInput) {
                ports.add(createPort(AudioFlow.IN, info));
            }
        }

        return ports;
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        List<AudioPort> availablePorts = new ArrayList<>();

        for (AudioPort port : getAllPorts()) {
            if (port.getFlow() == flow && isPortSupporting(port, audioFormat)) {
                availablePorts.add(port);
            }
        }

        if (availablePorts.isEmpty()) {
            throw new AudioPortsNotFoundException("No compatible audio ports found for the specified flow and format.");
        }

        return availablePorts;
    }

    @Override
    public boolean isPortSupporting(AudioPort port, AudioFormat audioFormat) throws UnsupportedAudioFormatException {
        Mixer mixer = getMixerForPort(port);
        if (mixer == null) return false;

        Line.Info lineInfo = (port.getFlow() == AudioFlow.OUT) ?
                new DataLine.Info(SourceDataLine.class, getJavaAudioFormat(audioFormat)) :
                new DataLine.Info(TargetDataLine.class, getJavaAudioFormat(audioFormat));

        return mixer.isLineSupported(lineInfo);
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        Collection<AudioPort> availablePorts = getAvailablePorts(flow, audioFormat);
        return availablePorts.stream().findFirst();
    }

    @Override
    public AudioInputDevice getInputDevice() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInputDevice'");
    }

    @Override
    public AudioOutputDevice getOutputDevice() {
        return new JavaSoundOutput();
    }

    private boolean hasOutputLines(Mixer mixer) {
        return Arrays.stream(mixer.getSourceLineInfo()).anyMatch(info -> info instanceof DataLine.Info);
    }

    private boolean hasInputLines(Mixer mixer) {
        return Arrays.stream(mixer.getTargetLineInfo()).anyMatch(info -> info instanceof DataLine.Info);
    }

    private AudioPort createPort(AudioFlow flow, Mixer.Info info) {
        return new AudioPort(flow, info.getName(), info.getVendor(), info.getVersion(), info.getDescription());
    }

    protected static javax.sound.sampled.AudioFormat getJavaAudioFormat(AudioFormat audioFormat) throws UnsupportedAudioFormatException {
        return new javax.sound.sampled.AudioFormat(
                getJavaAudioEncoding(audioFormat.getEncoding()),
                audioFormat.getSampleRate(),
                audioFormat.getBitsPerSample(),
                audioFormat.getChannels(),
                audioFormat.getFrameSize(),
                audioFormat.getByteRate() / audioFormat.getFrameSize(),
                audioFormat.isBigEndian()
        );
    }

    protected static javax.sound.sampled.AudioFormat.Encoding getJavaAudioEncoding(AudioFormat.Encoding encoding) throws UnsupportedAudioFormatException {
        switch (encoding) {
            case ALAW: return javax.sound.sampled.AudioFormat.Encoding.ALAW;
            case ULAW: return javax.sound.sampled.AudioFormat.Encoding.ULAW;
            case PCM_FLOAT: return javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
            case PCM_SIGNED: return javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
            case PCM_UNSIGNED: return javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;
            default: throw new UnsupportedAudioFormatException("Encoding not supported: " + encoding);
        }
    }

    protected static Mixer getMixerForPort(AudioPort port) {
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(port.getName())) {
                return AudioSystem.getMixer(info);
            }
        }
        return null;
    }
}
