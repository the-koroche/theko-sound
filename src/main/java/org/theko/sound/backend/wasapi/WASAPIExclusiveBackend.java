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

package org.theko.sound.backend.wasapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;

@AudioBackendType(name = "WASAPI Exclusive", version = "1.0")
public final class WASAPIExclusiveBackend extends WASAPISharedBackend {

    @Override
    public Collection<AudioPort> getAllPorts() throws BackendNotOpenException {
        AudioPort[] ports = getAllPortsInMode(true);
        if (ports == null) return List.of();
        List<AudioPort> result = new ArrayList<>();
        for (AudioPort port : ports) {
            AudioFormat supportedAudioFormat = getDeviceFormat(port);
            if (supportedAudioFormat == null) {
                AudioPort withoutFormatPort = new AudioPort(
                    port.getLink(),
                    port.getFlow(),
                    port.isActive(),
                    null,
                    port.getName(),
                    port.getVendor(),
                    port.getVersion(),
                    port.getDescription()
                );
                result.add(withoutFormatPort);
            }
            result.add(new AudioPort(
                port.getLink(),
                port.getFlow(),
                port.isActive(),
                supportedAudioFormat,
                port.getName(),
                port.getVendor(),
                port.getVersion(),
                port.getDescription()
            ));
        }
        return List.of(ports);
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws BackendNotOpenException {
        AudioPort audioPort = super.getDefaultPort(flow).orElse(null);
        if (audioPort == null) return Optional.empty();
        AudioFormat supportedAudioFormat = getDeviceFormat(audioPort);
        if (supportedAudioFormat == null) {
            AudioPort withoutFormatPort = new AudioPort(
                audioPort.getLink(),
                flow,
                audioPort.isActive(),
                null,
                audioPort.getName(),
                audioPort.getVendor(),
                audioPort.getVersion(),
                audioPort.getDescription()
            );
            return Optional.of(withoutFormatPort);
        }
        AudioPort targetPort = new AudioPort(
            audioPort.getLink(),
            flow,
            audioPort.isActive(),
            supportedAudioFormat,
            audioPort.getName(),
            audioPort.getVendor(),
            audioPort.getVersion(),
            audioPort.getDescription()
        );
        return Optional.of(targetPort);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat) throws BackendNotOpenException {
        return isFormatSupportedInMode(port, audioFormat, closestFormat, true);
    }

    @Override
    public AudioInputBackend getInputBackend() {
        throw new UnsupportedOperationException("WASAPIExclusiveInput is not realized.");
    }

    @Override
    public AudioOutputBackend getOutputBackend() {
        return new WASAPIExclusiveOutput();
    }
}
