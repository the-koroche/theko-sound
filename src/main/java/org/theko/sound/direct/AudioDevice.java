package org.theko.sound.direct;

import java.util.Collection;
import java.util.Optional;

import org.theko.sound.AudioFlow;
import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;

public interface AudioDevice {
    default void initialize() throws AudioDeviceException { };
    Collection<AudioPort> getAllPorts();
    Collection<AudioPort> getAvailablePorts(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException;
    boolean isPortSupporting(AudioPort port, AudioFormat audioFormat);
    Optional<AudioPort> getDefaultPort(AudioFlow flow, AudioFormat audioFormat) throws AudioPortsNotFoundException, UnsupportedAudioFormatException;

    AudioInputDevice getInputDevice();
    AudioOutputDevice getOutputDevice();
}
