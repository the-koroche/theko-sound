package org.theko.sound.codec;

public class AudioCodecNotFoundException extends AudioCodecException {
    public AudioCodecNotFoundException () {
        super();
    }

    public AudioCodecNotFoundException (String message) {
        super(message);
    }

    public AudioCodecNotFoundException (Throwable cause) {
        super(cause);
    }

    public AudioCodecNotFoundException (String message, Throwable cause) {
        super(message, cause);
    }
}
