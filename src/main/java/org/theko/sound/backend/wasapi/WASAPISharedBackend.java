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

@AudioBackendType(name = "WASAPI", version = "1.0")
public class WASAPISharedBackend implements AudioBackend {

    private static final Logger logger = LoggerFactory.getLogger(WASAPISharedBackend.class);

    protected boolean isOpen = false;

    static {
        String dllPath = ResourceLoader.getResourceFile("native/wasapi.dll").getAbsolutePath();
        System.load(dllPath);
    }

    @Override
    public void initialize() throws AudioBackendException {
        initialize0();
        isOpen = true;
    }

    @Override
    public void shutdown() throws AudioBackendException {
        shutdown0();
        isOpen = false;
    }

    @Override
    public Collection<AudioPort> getAllPorts() throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        AudioPort[] ports = getAllPorts0();
        return List.of(ports);
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return getAllPorts().stream()
                .filter(port -> port.getFlow() == flow)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return getAllPorts().stream()
                .filter(port -> port.getFlow() == flow && isFormatSupported(port, audioFormat))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AudioPort> getDefaultPort(AudioFlow flow) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return Optional.ofNullable(getDefaultPort0(flow));
    }

    @Override
    public Optional<AudioPort> getPort(AudioFlow flow, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return getAvailablePorts(flow, audioFormat).stream().findFirst();
    }

    @Override
    public boolean isFormatSupported(AudioPort port, AudioFormat audioFormat) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        return isFormatSupported0(port, audioFormat);
    }

    @Override
    public AudioFormat getBestMatchFormat(AudioPort port) throws BackendNotOpenException {
        if (!isOpen) throw new BackendNotOpenException("Backend is not initialized.");
        AudioFormat audioFormat = getMixFormat0(port);
        return audioFormat;
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

    // Native methods
    private native void initialize0();
    private native void shutdown0();
    private native AudioPort[] getAllPorts0();
    private native AudioPort getDefaultPort0(AudioFlow flow);
    private native AudioFormat getMixFormat0(AudioPort port);
    private native boolean isFormatSupported0(AudioPort port, AudioFormat audioFormat);
} 
