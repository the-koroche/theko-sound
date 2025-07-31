package org.theko.sound.backend.wasapi;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioPort;
import org.theko.sound.backend.AudioBackendException;

public class WASAPIExclusiveOutput extends WASAPISharedOutput {

    @Override
    protected void openOut(AudioPort port, AudioFormat audioFormat, int bufferSize) throws AudioBackendException {
        openOut0(true, port, audioFormat, bufferSize);
    }
}
