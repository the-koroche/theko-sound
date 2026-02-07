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

package org.theko.sound.backend.dummy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioFormat.Encoding;
import static org.theko.sound.AudioFormat.Encoding.*;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPorts;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;

/**
 * Dummy implementation of an audio backend that does not produce or capture any real audio.
 * This backend can be used for testing, development, or as a fallback when no real
 * audio backend is available.
 * <p>
 * It provides one dummy input port and one dummy output port, both supporting the
 * standard {@link AudioFormat#NORMAL_QUALITY_FORMAT}. Format checks always succeed
 * as long as the port is not null.
 * 
 * <p>
 * Note: Input backend functionality is not implemented yet and will throw
 * {@link UnsupportedOperationException}.
 * 
 * 
 * @author Theko
 * @since 2.4.1
 */
@AudioBackendType(name = "Dummy",
                description = "A dummy audio backend that does not produce or capture real audio",
                input = true, output = true)
public class DummyAudioBackend implements AudioBackend {

    /**
     * Always returns true since this backend is platform-independent.
     *
     * @return true
     */
    @Override
    public boolean isAvailableOnThisPlatform() {
        return true;
    }

    /**
     * Returns a list of all dummy audio ports: one input and one output.
     *
     * @return a collection containing both dummy input and output ports
     */
    @Override
    public Collection<AudioPort> getAllPorts() {
        List<AudioPort> ports = new ArrayList<>();
        final AudioPort outPort = new AudioPort(
            new DummyPortLink(), AudioFlow.OUT, true, AudioFormat.NORMAL_QUALITY_FORMAT,
            "Dummy Output Port", "Dummy Backend", "1.0", "Output");
        final AudioPort inPort = new AudioPort(
            new DummyPortLink(), AudioFlow.IN, true, AudioFormat.NORMAL_QUALITY_FORMAT,
            "Dummy Input Port", "Dummy Backend", "1.0", "Input");
        ports.add(outPort);
        ports.add(inPort);
        return ports;
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat)
            throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        return new AudioPorts(getAllPorts()).filter(flow).filter(audioFormat);
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow) throws AudioPortsNotFoundException {
        return new AudioPorts(getAllPorts()).filter(flow);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat,
            AtomicReference<AudioFormat> closestFormat) {
        if (port == null || audioFormat == null) return false;

        int sampleRate = audioFormat.getSampleRate() >= 0 ? audioFormat.getSampleRate() 
                        : AudioFormat.NORMAL_QUALITY_FORMAT.getSampleRate();
        int channels = audioFormat.getChannels() >= 0 ? audioFormat.getChannels() 
                        : AudioFormat.NORMAL_QUALITY_FORMAT.getChannels();
        int bitsPerSample = audioFormat.getBitsPerSample() >= 0 ? audioFormat.getBitsPerSample() 
                        : AudioFormat.NORMAL_QUALITY_FORMAT.getBitsPerSample();
        bitsPerSample += (8 - bitsPerSample % 8) % 8; // round to nearest multiple of 8

        Encoding encoding = audioFormat.getEncoding();
        if (encoding == null || (encoding == PCM_UNSIGNED && bitsPerSample != 8)) {
            encoding = AudioFormat.NORMAL_QUALITY_FORMAT.getEncoding();
        }

        AudioFormat result = new AudioFormat(sampleRate, bitsPerSample, channels, encoding, false);
        if (closestFormat != null) closestFormat.set(result);

        return result.isSameFormat(audioFormat);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) {
        return isFormatSupported(port, audioFormat, null);
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws AudioPortsNotFoundException {
        return getAvailablePorts(flow).stream().findFirst();
    }

    @Override
    public Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat)
            throws AudioPortsNotFoundException, UnsupportedAudioFormatException {
        return getAvailablePorts(flow, audioFormat).stream().findFirst();
    }

    /**
     * Returns the input backend.
     *
     * @return a dummy input backend instance
     */
    @Override
    public AudioInputBackend getInputBackend() {
        return new DummyInputBackend();
    }

    /**
     * Returns the output backend.
     *
     * @return a dummy output backend instance
     */
    @Override
    public AudioOutputBackend getOutputBackend() {
        return new DummyOutputBackend();
    }

    /**
     * Always returns true since this backend is considered initialized by default.
     *
     * @return true
     */
    @Override
    public boolean isInitialized() {
        return true;
    }
}