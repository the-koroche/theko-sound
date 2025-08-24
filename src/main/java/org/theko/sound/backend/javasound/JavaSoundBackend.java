/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.backend.javasound;

import java.util.concurrent.atomic.AtomicReference;
import java.util.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioFormat.Encoding;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.UnsupportedPortLinkException;

/**
 * The {@code JavaSoundBackend} class is an implementation of the {@link AudioBackend} interface
 * that uses the Java Sound API to interact with audio hardware. It provides methods to
 * retrieve audio ports, check port compatibility, and access input and output backends.
 * 
 * <p>This class is annotated with {@link AudioBackendType} to specify its name and version.
 * It supports both input and output audio flows and handles audio formats through the
 * Java Sound API.
 * 
 * <p>This class is designed to work seamlessly with the Java Sound API, providing a bridge
 * between custom audio abstractions and the underlying audio hardware.
 * 
 * @see AudioBackend
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 * 
 * @since 1.0.0
 * @author Theko
 */
@AudioBackendType (name = "JavaSound", version = "1.1")
public sealed class JavaSoundBackend implements AudioBackend permits JavaSoundInput, JavaSoundOutput {

    private static final Logger logger = LoggerFactory.getLogger(JavaSoundBackend.class);

    protected static final Map<Mixer.Info, AudioPort> cachedPorts = new HashMap<>();
    private static boolean isAvailableInThisScope = false;

    static {
        try {
            Class.forName("javax.sound.sampled.AudioSystem");
            isAvailableInThisScope = true;
        } catch (ClassNotFoundException e) {
            logger.info("Java Sound API is not available.");
            isAvailableInThisScope = false;
        }
    }

    @Override
    public boolean isAvailableOnThisPlatform() {
        return isAvailableInThisScope;
    }
    
    @Override
    public Collection<AudioPort> getAllPorts() {
        List<AudioPort> ports = new ArrayList<>();

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            boolean hasOutput = hasOutputLines(mixer);
            boolean hasInput = hasInputLines(mixer);

            if (hasOutput) {
                ports.add(createPort(AudioFlow.OUT, mixer, info));
            }

            if (hasInput) {
                ports.add(createPort(AudioFlow.IN, mixer, info));
            }
        }

        return Collections.unmodifiableList(ports);
    }

    private AudioPort createPort(AudioFlow flow, Mixer mixer, Mixer.Info info) {
        if (cachedPorts.containsKey(info) || cachedPorts.get(info) != null) {
            return cachedPorts.get(info);
        }
        AudioFormat mixFormat = getMixingFormat(mixer, flow);
        AudioPort port = new AudioPort(
            info,
            flow,
            true,
            mixFormat,
            info.getName(),
            info.getVendor(),
            info.getVersion(),
            info.getDescription()
        );
        cachedPorts.put(info, port);
        return port;
    }

    private boolean hasOutputLines(Mixer mixer) {
        return Arrays.stream(mixer.getSourceLineInfo()).anyMatch(info -> info instanceof DataLine.Info);
    }

    private boolean hasInputLines(Mixer mixer) {
        return Arrays.stream(mixer.getTargetLineInfo()).anyMatch(info -> info instanceof DataLine.Info);
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        List<AudioPort> availablePorts = new ArrayList<>();

        for (AudioPort port : getAllPorts()) {
            if (port.getFlow() == flow && isFormatSupported(port, audioFormat)) {
                logger.debug("Found compatible audio port: {}", port);
                availablePorts.add(port);
            }
        }

        if (availablePorts.isEmpty()) {
            logger.warn("No compatible audio ports found for the specified flow and format.");
            throw new AudioPortsNotFoundException("No compatible audio ports found for the specified flow and format.");
        }

        return Collections.unmodifiableList(availablePorts);
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow) throws AudioPortsNotFoundException {
        List<AudioPort> availablePorts = new ArrayList<>();

        for (AudioPort port : getAllPorts()) {
            if (port.getFlow() == flow) {
                logger.debug("Found compatible audio port: {}", port);
                availablePorts.add(port);
            }
        }

        if (availablePorts.isEmpty()) {
            logger.warn("No compatible audio ports found for the specified flow and format.");
            throw new AudioPortsNotFoundException("No compatible audio ports found for the specified flow and format.");
        }

        return Collections.unmodifiableList(availablePorts);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat) {
        // TODO: Implement closest format
        return isFormatSupported(port, audioFormat);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) {
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
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws AudioPortsNotFoundException {
        Collection<AudioPort> availablePorts = getAvailablePorts(flow);
        return availablePorts.stream().findFirst();
    }

    @Override
    public Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        Collection<AudioPort> availablePorts = getAvailablePorts(flow, audioFormat);
        return availablePorts.stream().findFirst();
    }

    @Override
    public AudioInputBackend getInputBackend() {
        return new JavaSoundInput();
    }

    @Override
    public AudioOutputBackend getOutputBackend() {
        return new JavaSoundOutput();
    }

    @Override
    public boolean isInitialized() {
        return true; // Java Sound is always initialized, due to static initialization of AudioSystem
    }

    protected static Mixer getMixerForPort(AudioPort port) {
        if (port == null) {
            logger.debug("Using default mixer.");
            return AudioSystem.getMixer(null);
        }

        Object mixer = port.getLink();
        if (mixer instanceof Mixer.Info) {
            return AudioSystem.getMixer((Mixer.Info) mixer);
        } else {
            logger.warn("Audio port link is not compatible with Java Sound: {}", port);
            throw new UnsupportedPortLinkException("Audio port link is not compatible with Java Sound");
        }
    }

    protected AudioFormat getMixingFormat(Mixer mixer, AudioFlow flow) {
        if (mixer == null) return null;

        int[] sampleRates = {384000, 352800, 192000, 176400, 96000, 88200, 48000, 44100, 22500, 16000, 11025, 8000};
        int[] bitDepths = {64, 32, 24, 16, 8};
        int[] channels = {2, 1};
        Encoding[] encodings = {Encoding.PCM_FLOAT, Encoding.PCM_SIGNED, Encoding.PCM_UNSIGNED};

        List<AudioFormat> sortedFormats = new ArrayList<>();
        
        // Generate combinations of audio format properties
        for (int ch : channels) {
            for (int bits : bitDepths) {
                for (int rate : sampleRates) {
                    for (Encoding enc : encodings) {
                        // Skip invalid combinations:
                        // 1. 8-bit FLOAT or stereo SIGNED PCM is not valid
                        // 2. FLOAT encoding must be at least 32-bit
                        if (bits == 8 && (enc == Encoding.PCM_FLOAT || (enc == Encoding.PCM_SIGNED && ch == 2))) continue;
                        if (enc == Encoding.PCM_FLOAT && bits < 32) continue;
                        if (bits >= 32 && (enc == Encoding.PCM_SIGNED || enc == Encoding.PCM_UNSIGNED)) continue;

                        sortedFormats.add(new AudioFormat(rate, bits, ch, enc, false));
                    }
                }
            }
        }
        try {
            for (AudioFormat current : sortedFormats) {
                if (isMixerSupporting(mixer, flow, current)) {
                    logger.trace("Compatible format found: {}", current);
                    return current;
                }
                logger.trace("Format {} is not compatible with the audio port. Skipping.", current);
            }
        } catch (UnsupportedAudioFormatException ex) {
            logger.info("Unsupported audio format: {}", ex);
        }

        logger.info("No compatible format found.");

        // No compatible format found
        return null;
    }

    protected static boolean isMixerSupporting(Mixer mixer, AudioFlow flow, AudioFormat audioFormat) throws UnsupportedAudioFormatException {
        Line.Info lineInfo = (flow == AudioFlow.OUT) ?
                    new DataLine.Info(SourceDataLine.class, getJavaSoundAudioFormat(audioFormat)) :
                    new DataLine.Info(TargetDataLine.class, getJavaSoundAudioFormat(audioFormat));

        return mixer.isLineSupported(lineInfo);
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
            default: 
                logger.debug("Encoding not supported: {}", encoding);
                throw new UnsupportedAudioFormatException("Encoding not supported: " + encoding);
        }
    }
}
