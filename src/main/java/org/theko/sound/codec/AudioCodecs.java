package org.theko.sound.codec;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioClassLoader;

public class AudioCodecs {
    private static final Logger logger = LoggerFactory.getLogger(AudioCodecs.class);

    // A collection to hold all registered audio codecs
    private static final Collection<AudioCodecInfo> audioCodecs = Collections.synchronizedSet(new LinkedHashSet<>());

    static {
        registerCodecs();
    }

    private AudioCodecs() { }

    /**
     * Register all audio codecs that are annotated with AudioCodecType
     */
    private static void registerCodecs() {
        logger.debug("Registering audio codecs...");
        // Use Reflections to find all classes implementing AudioCodec
        audioCodecs.clear();
        Set<Class<? extends AudioCodec>> allAudioCodecs = AudioClassLoader.getAvailableCodecs();

        for (Class<? extends AudioCodec> audioCodecClass : allAudioCodecs) {
            if (audioCodecClass.isAnnotationPresent(AudioCodecType.class)) {
                AudioCodecInfo codecInfo = new AudioCodecInfo(audioCodecClass);
                audioCodecs.add(codecInfo);
                logger.info("Found audio codec: " + codecInfo);
            } else {
                logger.info("Found audio codec without information: " + audioCodecClass.getSimpleName());
            }
        }
    }

    /**
     * Get an AudioCodecInfo by its name
     */
    public static AudioCodecInfo fromName(String name) throws AudioCodecNotFoundException {
        for (AudioCodecInfo audioCodec : audioCodecs) {
            if (audioCodec.getName().equalsIgnoreCase(name)) {
                return audioCodec;
            }
        }
        logger.error("No audio codecs found by name: '" + name + "'.");
        throw new AudioCodecNotFoundException("No audio codecs found by name: '" + name + "'.");
    }

    /**
     * Get an AudioCodecInfo by its exception
     * @param name File extension without a dot or an asterisk.
     */
    public static AudioCodecInfo fromExtension(String extension) throws AudioCodecNotFoundException {
        for (AudioCodecInfo audioCodec : audioCodecs) {
            if (audioCodec.getExtension().equalsIgnoreCase(extension)) {
                return audioCodec;
            }
        }
        logger.error("No audio codecs found by extension: '" + extension + "'.");
        throw new AudioCodecNotFoundException("No audio codecs found by extension: '" + extension + "'.");
    }

    public static AudioCodec getCodec(AudioCodecInfo codecInfo) throws AudioCodecCreationException {
        try {
            return codecInfo.getCodecClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            logger.error(e.getMessage());
            throw new AudioCodecCreationException(e);
        }
    }

    /**
     * Get all registered audio codecs
     */
    public static Collection<AudioCodecInfo> getAllCodecs() {
        return audioCodecs;
    }
}
