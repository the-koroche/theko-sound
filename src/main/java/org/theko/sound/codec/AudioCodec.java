package org.theko.sound.codec;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFormat;

public abstract class AudioCodec {
    private static final Logger logger = LoggerFactory.getLogger(AudioCodec.class);

    protected abstract AudioDecodeResult decode(InputStream is) throws AudioCodecException;
    protected abstract AudioEncodeResult encode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException;

    public AudioDecodeResult callDecode(InputStream is) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioDecodeResult adr = decode(is);
        long endNs = System.nanoTime();
        logger.debug("Elapsed decoding time: " + (endNs - startNs) + " ns.");
        return adr;
    }

    public AudioEncodeResult callEncode(byte[] data, AudioFormat format, List<AudioTag> tags) throws AudioCodecException {
        long startNs = System.nanoTime();
        AudioEncodeResult aer = encode(data, format, tags);
        long endNs = System.nanoTime();
        logger.debug("Elapsed encoding time: " + (endNs - startNs) + " ns.");
        return aer;
    }

    public abstract AudioCodecInfo getInfo();
}