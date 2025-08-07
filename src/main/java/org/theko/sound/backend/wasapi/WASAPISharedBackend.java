package org.theko.sound.backend.wasapi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.ResourceLoader;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackendException;
import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.BackendNotOpenException;

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
public class WASAPISharedBackend implements AudioBackend {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedBackend.class);

    private long backendContextPtr = 0;
    private boolean isOpen = false;

    static {
        String dllPath = ResourceLoader.getResourceFile("native/wasapi.dll").getAbsolutePath();
        System.load(dllPath);
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
        AudioPort[] ports = getAllPorts0();
        if (ports == null) return List.of();
        return List.of(ports);
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
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        if (port == null || !isAudioPortSupported(port)) return false;
        return isFormatSupported0(port, audioFormat);
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

    protected boolean isAudioPortSupported(AudioPort port) {
        return port != null && port.getLink().getClass().equals(WASAPINativeAudioPortHandle.class);
    }

    // Native methods
    private native void initialize0();
    private native void shutdown0();
    private native AudioPort[] getAllPorts0();
    private native AudioPort getDefaultPort0(AudioFlow flow);
    private native boolean isFormatSupported0(AudioPort port, AudioFormat audioFormat);
} 
