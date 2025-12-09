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
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;
import org.theko.sound.utility.FileUtilities;
import org.theko.sound.utility.PlatformUtilities;
import org.theko.sound.utility.ResourceLoader;
import org.theko.sound.utility.ResourceNotFoundException;
import org.theko.sound.utility.PlatformUtilities.Platform;

/**
 * {@code WASAPISharedBackend} is an implementation of the {@link AudioBackend} interface
 * that provides audio backend functionality using the Windows Audio Session API (WASAPI) in shared mode.
 * <p>
 * This backend is responsible for initializing and shutting down the WASAPI native library,
 * enumerating available audio ports, checking format support, and providing access to audio input/output backends.
 * 
 * <p>
 * The backend loads the native WASAPI DLL on class initialization and manages the lifecycle of the native resources.
 * It supports querying all available audio ports, filtering by flow and format, and retrieving default ports.
 * 
 * <p>
 * Note: Input backend functionality is not realized and will throw {@link UnsupportedOperationException}.
 *
 * @author Theko
 * @since 2.3.2
 * 
 * @see AudioBackend
 * @see AudioPort
 * @see AudioFlow
 * @see AudioFormat
 */
@AudioBackendType(name = "WASAPI", version = "1.0")
public sealed class WASAPISharedBackend implements AudioBackend permits WASAPISharedOutput {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedBackend.class);

    private static final File lib32, lib64;
    private long backendContextPtr = 0;
    private boolean isInitialized = false;

    private static final boolean isSupported;

    static {
        lib64 = loadLibrary("native/WASApiShrd64.dll", "X64");
        lib32 = loadLibrary("native/WASApiShrd32.dll", "X32");

        isSupported = isAvailableOnThisPlatformStatic();

        if (!isSupported) {
            logger.warn("WASAPI backend is not supported on this platform.");
        } else {
            File libToLoad = switch (PlatformUtilities.getArchitecture()) {
                case X86_64 -> lib64;
                case X86_32 -> lib32;
                default -> {
                    logger.error("Unsupported architecture.");
                    yield null;
                }
            };
            if (libToLoad != null) {
                try {
                    System.load(libToLoad.getAbsolutePath());
                    logger.info("Loaded WASAPI library: {}", libToLoad.getName());
                } catch (UnsatisfiedLinkError e) {
                    logger.error("Failed to load WASAPI library: {}", libToLoad.getAbsolutePath(), e);
                    logger.warn("Library failed to load; API operations may be unstable. See stack trace for details: {}", e.getMessage());
                }
            } else {
                logger.error("WASAPI library is null for architecture {}", PlatformUtilities.getArchitecture());
            }
        }
    }

    private static File loadLibrary(String resourcePath, String archLabel) {
        try {
            return ResourceLoader.getResourceFile(resourcePath);
        } catch (ResourceNotFoundException e) {
            logger.error("{} library was not found: {}", archLabel, resourcePath, e);
            return null;
        }
    }

    protected static boolean isAvailableOnThisPlatformStatic() {
        boolean isWindows = PlatformUtilities.getPlatform() == Platform.WINDOWS;
        boolean is64 = PlatformUtilities.getArchitecture().getBits() == 64;
        boolean is32 = PlatformUtilities.getArchitecture().getBits() == 32;
        boolean hasLibraryResource = (is64 && lib64 != null) || (is32 && lib32 != null);
        boolean hasSysLibrary = hasWASAPISystemLib();

        logger.trace("isWindows: {}, hasLibraryResource: {}, hasSysLibrary: {}", isWindows, hasLibraryResource, hasSysLibrary);

        return isWindows && hasLibraryResource && hasSysLibrary;
    }

    private static boolean hasWASAPISystemLib() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null) return false;

        File sys32 = new File(systemRoot, "System32");
        File wow64 = new File(systemRoot, "SysWOW64");

        return FileUtilities.existsAny(sys32, "mmdevapi.dll", "avrt.dll")
            || FileUtilities.existsAny(wow64, "mmdevapi.dll", "avrt.dll");
    }

    @Override
    public boolean isAvailableOnThisPlatform() {
        return isSupported;
    }

    @Override
    public void initialize() throws AudioBackendException {
        if (isInitialized) return;
        nInit();
        isInitialized = true;
    }

    @Override
    public void shutdown() throws AudioBackendException {
        if (!isInitialized) return;
        nShutdown();
        isInitialized = false;
    }

    @Override
    public Collection<AudioPort> getAllPorts() throws BackendNotOpenException {
        boolean initBefore = false;
        if (!isInitialized()) {
            initBefore = true;
            initialize();
        }
        try {
            AudioPort[] ports = nGetAllPorts();
            if (ports == null || ports.length == 0) {
                return List.of();
            }

            return Arrays.asList(ports);
        } finally {
            if (initBefore) {
                shutdown();
            }
        }
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow) throws BackendNotOpenException {
        if (flow == null) return List.of();

        return getAllPorts().stream()
                .filter(port -> port.getFlow() == flow)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (flow == null || audioFormat == null) return List.of();

        boolean initBefore = false;
        if (!isInitialized()) {
            initBefore = true;
            initialize();
        }
        try {
            return getAllPorts().stream()
                    .filter(port -> port.getFlow() == flow && isFormatSupported(port, audioFormat))
                    .collect(Collectors.toList());
        } finally {
            if (initBefore) {
                shutdown();
            }
        }
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws BackendNotOpenException {
        if (flow == null) return Optional.empty();

        boolean initBefore = false;
        if (!isInitialized()) {
            initBefore = true;
            initialize();
        }
        try {
            return Optional.ofNullable(nGetDefaultPort(flow));
        } finally {
            if (initBefore) {
                shutdown();
            }
        }
    }

    @Override
    public Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (flow == null || audioFormat == null) return Optional.empty();
        return getAvailablePorts(flow, audioFormat).stream().findFirst();
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat) throws BackendNotOpenException {
        if (port == null || !isAudioPortSupported(port)) return false;
        if (audioFormat == null) return false;

        boolean initBefore = false;
        if (!isInitialized()) {
            initBefore = true;
            initialize();
        }
        try {
            logger.trace("Checking if audio format: {} is supported for port: {}", audioFormat, port);
            return nIsFormatSupported(port, audioFormat, closestFormat);
        } finally {
            if (initBefore) {
                shutdown();
            }
        }
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) throws BackendNotOpenException {
        return isFormatSupported(port, audioFormat, null);
    }

    @Override
    public AudioInputBackend getInputBackend() {
        throw new UnsupportedOperationException("WASAPISharedInput is not implemented yet.");
    }

    @Override
    public AudioOutputBackend getOutputBackend() {
        return new WASAPISharedOutput();
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isAudioPortSupported(AudioPort port) {
        return port != null && port.getLink().getClass().equals(WASAPIPortHandle.class);
    }

    private synchronized native void nInit();
    private synchronized native void nShutdown();
    private synchronized native AudioPort[] nGetAllPorts();
    private synchronized native AudioPort nGetDefaultPort(AudioFlow flow);
    private synchronized native boolean nIsFormatSupported(AudioPort port, AudioFormat audioFormat, AtomicReference<AudioFormat> closestFormat);
} 
