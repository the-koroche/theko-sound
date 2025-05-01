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

/**
 * The {@code JavaSoundDevice} class is an implementation of the {@link AudioDevice} interface
 * that uses the Java Sound API to interact with audio hardware. It provides methods to
 * retrieve audio ports, check port compatibility, and access input and output devices.
 * 
 * <p>This class is annotated with {@link AudioDeviceType} to specify its name and version.
 * It supports both input and output audio flows and handles audio formats through the
 * Java Sound API.
 * 
 * <p>Key functionalities include:
 * <ul>
 *   <li>Retrieving all available audio ports ({@link #getAllPorts()}).</li>
 *   <li>Filtering ports based on audio flow and format ({@link #getAvailablePorts(AudioFlow, AudioFormat)}).</li>
 *   <li>Checking if a port supports a specific audio format ({@link #isPortSupporting(AudioPort, AudioFormat)}).</li>
 *   <li>Getting the default port for a specific flow and format ({@link #getDefaultPort(AudioFlow, AudioFormat)}).</li>
 *   <li>Providing access to input and output devices ({@link #getInputDevice()} and {@link #getOutputDevice()}).</li>
 * </ul>
 * 
 * <p>Helper methods are included to:
 * <ul>
 *   <li>Determine if a mixer has input or output lines ({@link #hasInputLines(Mixer)} and {@link #hasOutputLines(Mixer)}).</li>
 *   <li>Create {@link AudioPort} instances for audio flows ({@link #createPort(AudioFlow, Mixer.Info)}).</li>
 *   <li>Convert custom {@link AudioFormat} to Java Sound's {@link javax.sound.sampled.AudioFormat} ({@link #getJavaSoundAudioFormat(AudioFormat)}).</li>
 *   <li>Retrieve the appropriate mixer for a given port ({@link #getMixerForPort(AudioPort)}).</li>
 * </ul>
 * 
 * <p>Exceptions handled include:
 * <ul>
 *   <li>{@link AudioPortsNotFoundException} - Thrown when no compatible audio ports are found.</li>
 *   <li>{@link UnsupportedAudioFormatException} - Thrown when an unsupported audio format is encountered.</li>
 * </ul>
 * 
 * <p>This class is designed to work seamlessly with the Java Sound API, providing a bridge
 * between custom audio abstractions and the underlying audio hardware.
 * 
 * @see AudioDevice
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 * 
  * @author Alex Soloviov
 */
@AudioDeviceType(name = "JavaSound", version = "1.0")
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

        return Collections.unmodifiableList(ports);
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

        return Collections.unmodifiableList(availablePorts);
    }

    @Override
    public boolean isPortSupporting(AudioPort port, AudioFormat audioFormat) {
        try {
        Mixer mixer = getMixerForPort(port);
        if (mixer == null) return false;

        Line.Info lineInfo = (port.getFlow() == AudioFlow.OUT) ?
                new DataLine.Info(SourceDataLine.class, getJavaSoundAudioFormat(audioFormat)) :
                new DataLine.Info(TargetDataLine.class, getJavaSoundAudioFormat(audioFormat));

        return mixer.isLineSupported(lineInfo);
        } catch (UnsupportedAudioFormatException ex) {
            return false;
        }
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        Collection<AudioPort> availablePorts = getAvailablePorts(flow, audioFormat);
        return availablePorts.stream().findFirst();
    }

    @Override
    public AudioInputDevice getInputDevice() {
        return new JavaSoundInput();
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

    protected static javax.sound.sampled.AudioFormat getJavaSoundAudioFormat(AudioFormat audioFormat) throws UnsupportedAudioFormatException {
        return new javax.sound.sampled.AudioFormat(
                getJavaSoundAudioEncoding(audioFormat.getEncoding()),
                audioFormat.getSampleRate(),
                audioFormat.getBitsPerSample(),
                audioFormat.getChannels(),
                audioFormat.getFrameSize(),
                audioFormat.getByteRate() / audioFormat.getFrameSize(),
                audioFormat.isBigEndian()
        );
    }

    protected static javax.sound.sampled.AudioFormat.Encoding getJavaSoundAudioEncoding(AudioFormat.Encoding encoding) throws UnsupportedAudioFormatException {
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
        if (port == null) {
            return AudioSystem.getMixer(null);
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(port.getName())) {
                return AudioSystem.getMixer(info);
            }
        }
        return null;
    }
}
