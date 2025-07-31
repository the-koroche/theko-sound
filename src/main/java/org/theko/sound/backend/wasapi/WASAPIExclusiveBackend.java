package org.theko.sound.backend.wasapi;

import org.theko.sound.backend.AudioBackendType;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;

@AudioBackendType(name = "WASAPI Exclusive", version = "1.0")
public class WASAPIExclusiveBackend extends WASAPISharedBackend {

    @Override
    public AudioInputBackend getInputBackend() {
        throw new UnsupportedOperationException("WASAPIExclusiveInput is not realized.");
    }

    @Override
    public AudioOutputBackend getOutputBackend() {
        return new WASAPIExclusiveOutput();
    }
}
