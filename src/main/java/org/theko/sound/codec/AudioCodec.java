package org.theko.sound.codec;

import java.io.InputStream;
import java.util.List;

import org.theko.sound.AudioFormat;

public interface AudioCodec {
    AudioDecodeResult decode(InputStream is) throws AudioCodecException;
    AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException;
}