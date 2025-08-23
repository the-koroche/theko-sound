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

package org.theko.sound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendCreationException;
import org.theko.sound.backend.AudioBackendNotFoundException;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.utility.AudioPortUtility;

/**
 * Represents a collection of audio ports.
 * Supports filtering by flow, format, and active status.
 * 
 * @author Theko
 * @since 2.3.2
 */
public class AudioPorts extends ArrayList<AudioPort> {
    
    public AudioPorts(Collection<AudioPort> ports) {
        super(ports);
    }

    public AudioPorts filter(AudioFlow flow) {
        return new AudioPorts(super.stream()
            .filter( p -> p.getFlow() == flow)
            .collect(Collectors.toList()));
    }

    public AudioPorts filter(AudioFormat audioFormat) {
        return new AudioPorts(super.stream()
            .filter( p -> {
                Class<? extends AudioBackend> backendClass = 
                    AudioPortUtility.getBackendByLinkType(p.getLink().getClass());
                if (backendClass == null) return false;
                try {
                    AudioBackend backend = AudioBackends.getBackend(backendClass);
                    return backend.isFormatSupported(p, audioFormat);
                } catch (AudioBackendNotFoundException | AudioBackendCreationException e) {
                    return false;
                }
            })
            .collect(Collectors.toList()));
    }

    public AudioPorts filter(boolean isActive) {
        return new AudioPorts(super.stream()
            .filter( p -> p.isActive() == isActive)
            .collect(Collectors.toList()));
    }
}
