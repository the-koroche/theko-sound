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

import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioFormat.Encoding;
import org.theko.sound.AudioPort;
import org.theko.sound.ResourceLoader;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.utility.PlatformUtilities;
import org.theko.sound.utility.PlatformUtilities.Platform;

/**
 * {@code WASAPISharedBackend} is an implementation of the {@link AudioBackend} interface
 * that provides audio backend functionality using the Windows Audio Session API (WASAPI) in shared mode.
 * <p>
 * This backend is responsible for initializing and shutting down the WASAPI native library,
 * enumerating available audio ports, checking format support, and providing access to audio input/output backends.
 * </p>
 * <p>
 * The backend loads the native WASAPI DLL on class initialization and manages the lifecycle of the native resources.
 * It supports querying all available audio ports, filtering by flow and format, and retrieving default ports.
 * </p>
 * <p>
 * Note: Input backend functionality is not realized and will throw {@link UnsupportedOperationException}.
 * </p>
 *
 * @author Theko
 * @since v2.3.2
 * 
 * @see AudioBackend
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 */
@AudioBackendType(name = "WASAPI", version = "1.0")
public sealed class WASAPISharedBackend implements AudioBackend permits WASAPIExclusiveBackend, WASAPISharedOutput {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedBackend.class);

    private long backendContextPtr = 0;
    private boolean isOpen = false;

    static {
        if (PlatformUtilities.getPlatform() != Platform.WINDOWS) {
            logger.warn("WASAPI backend is not supported on this platform.");
        } else {
            try {
                String dllPath = ResourceLoader.getResourceFile("native/wasapi_backend.dll").getAbsolutePath();
                System.load(dllPath);
            } catch (UnsatisfiedLinkError e) {
                logger.error("Failed to load WASAPI DLL", e);
            }
        }
    }

    @Override
    public void initialize() throws AudioBackendException {
        if (isOpen) return;
        initialize0();
        isOpen = true;
    }

    @Override
    public void shutdown() throws AudioBackendException {
        if (!isOpen) return;
        shutdown0();
        isOpen = false;
    }

    @Override
    public Collection<AudioPort> getAllPorts() throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        AudioPort[] ports = getAllPortsInMode(false);
        if (ports == null) return List.of();
        return List.of(ports);
    }

    protected AudioPort[] getAllPortsInMode(boolean exclusive) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return getAllPorts0(exclusive);
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (flow == null) return List.of();
        return getAllPorts().stream()
                .filter(port -> port.getFlow() == flow)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (flow == null || audioFormat == null) return List.of();
        return getAllPorts().stream()
                .filter(port -> port.getFlow() == flow && isFormatSupported(port, audioFormat))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (flow == null) return Optional.empty();
        return Optional.ofNullable(getDefaultPort0(flow));
    }

    @Override
    public Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (flow == null || audioFormat == null) return Optional.empty();
        return getAvailablePorts(flow, audioFormat).stream().findFirst();
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat) throws BackendNotOpenException {
        return isFormatSupportedInMode(port, audioFormat, closestFormat, false);
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) throws BackendNotOpenException {
        return isFormatSupported(port, audioFormat, null);
    }

    protected boolean isFormatSupportedInMode(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat, boolean exclusive) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (port == null || !isAudioPortSupported(port)) return false;
        if (audioFormat == null) return false;
        logger.debug("Checking if audio format: {} is supported for port: {}", audioFormat, port);
        return isFormatSupported0(port, audioFormat, closestFormat, exclusive);
    }

    @Override
    public AudioInputBackend getInputBackend() {
        throw new UnsupportedOperationException("WASAPISharedInput is not realized.");
    }

    @Override
    public AudioOutputBackend getOutputBackend() {
        return new WASAPISharedOutput();
    }

    @Override
    public boolean isInitialized() {
        return isOpen;
    }

    public AudioFormat getDeviceFormat(AudioPort port) throws BackendNotOpenException {
        if (port == null) return null;

        int[] sampleRates = {192000, 96000, 48000, 44100, 22500, 16000, 11025, 8000};
        int[] bitDepths = {32, 24, 16};
        int[] channels = {2, 1};
        Encoding[] encodings = {Encoding.PCM_FLOAT, Encoding.PCM_SIGNED};

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
        for (AudioFormat current : sortedFormats) {
            AtomicReference<AudioFormat> closest = new AtomicReference<>();
            closest.set(null);
            if (isFormatSupportedInMode(port, current, closest, true)) {
                return current;
            }
            if (closest.get() != null) {
                return closest.get();
            }
        }

        logger.info("No compatible format found.");

        // No compatible format found
        return null;
    }

    protected boolean isAudioPortSupported(AudioPort port) {
        return port != null && port.getLink().getClass().equals(WASAPINativeAudioPortHandle.class);
    }

    // Native methods
    private synchronized native void initialize0();
    private synchronized native void shutdown0();
    private synchronized native AudioPort[] getAllPorts0(boolean exclusiveMixFormat);
    private synchronized native AudioPort getDefaultPort0(AudioFlow flow);
    private synchronized native boolean isFormatSupported0(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat, boolean exclusive);
} 
