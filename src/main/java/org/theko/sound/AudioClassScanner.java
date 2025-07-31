package org.theko.sound;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioInputBackend;
import org.theko.sound.backend.AudioOutputBackend;
import org.theko.sound.backend.javasound.JavaSoundBackend;
import org.theko.sound.backend.wasapi.WASAPIExclusiveBackend;
import org.theko.sound.backend.wasapi.WASAPISharedBackend;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.formats.WAVECodec;
import org.theko.sound.properties.AudioSystemProperties;

/**
 * The AudioClassScanner class is responsible for scanning the classpath for audio backend and codec classes.
 * <p>It uses the Reflections library to find all subclasses of AudioBackend and AudioCodec,
 * filtering out input and output backends to only include general audio backends.
 * The scanned classes are stored in static sets for later retrieval.
 * This class is not meant to be instantiated.
 *
 * @since v2.0.0
 * @author Theko
 * 
 * @see ResourceLoader
 * @see AudioBackend
 * @see AudioCodec
 */
public final class AudioClassScanner {

    private AudioClassScanner() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    private static final Logger logger = LoggerFactory.getLogger(AudioClassScanner.class);

    private static final Set<Class<? extends AudioBackend>> definedBackends = Set.of(
        JavaSoundBackend.class,
        WASAPISharedBackend.class,
        WASAPIExclusiveBackend.class
    );

    private static final Set<Class<? extends AudioCodec>> definedCodecs = Set.of(
        WAVECodec.class
    );

    private static Reflections reflections;
    private static Set<Class<? extends AudioBackend>> scannedBackends;
    private static Set<Class<? extends AudioCodec>> scannedCodecs;

    static {
        if (AudioSystemProperties.SCAN_CLASSES) {
            scanClasses();
        }
    }

    /**
     * Scans the classpath for audio backend and codec classes.
     * This method uses the Reflections library to find all subclasses of AudioBackend and AudioCodec.
     * It filters out input and output backends to only include general audio backends.
     * The scanned classes are stored in static sets for later retrieval.
     * This method is time consuming and should be called only once,
     * typically at application startup.
     */
    private static void scanClasses() {
        if (reflections == null) {
            try {
                scannedBackends = new HashSet<>(definedBackends);
                scannedCodecs = new HashSet<>(definedCodecs);

                reflections = new Reflections(
                    new ConfigurationBuilder()
                        .addUrls(ClasspathHelper.forJavaClassPath())
                        .setScanners(Scanners.SubTypes)
                );

                Set<Class<? extends AudioBackend>> allBackends = reflections.getSubTypesOf(AudioBackend.class);

                Set<Class<? extends AudioBackend>> filteredBackends = allBackends.stream()
                    .filter(cls -> !AudioInputBackend.class.isAssignableFrom(cls))
                    .filter(cls -> !AudioOutputBackend.class.isAssignableFrom(cls))
                    .collect(Collectors.toSet());

                scannedBackends.addAll(filteredBackends);

                scannedCodecs.addAll(reflections.getSubTypesOf(AudioCodec.class));

                logger.info("Scanned {} audio backends, {} codecs",
                    scannedBackends.size(), scannedCodecs.size());

            } catch (ReflectionsException ex) {
                logger.error("Error initializing 'org.reflections.Reflections'", ex);
                logger.warn("Falling back to predefined packages.");
                logger.info("Found {} audio backends, {} codecs",
                    scannedBackends.size(), scannedCodecs.size());
            }
        }
    }

    /**
     * Returns a set of all audio backend classes available in the system.
     * If class scanning is enabled, it will include both predefined and scanned backends.
     * Otherwise, it will return only the predefined backends.
     *
     * @return A set of audio backend classes.
     */
    public static Set<Class<? extends AudioBackend>> getBackendClasses () {
        return scannedBackends != null ? Collections.unmodifiableSet(scannedBackends) : definedBackends;
    }

    /**
     * Returns a set of all audio codec classes available in the system.
     * If class scanning is enabled, it will include both predefined and scanned codecs.
     * Otherwise, it will return only the predefined codecs.
     *
     * @return A set of audio codec classes.
     */
    public static Set<Class<? extends AudioCodec>> getCodecClasses () {
        return scannedCodecs != null ? Collections.unmodifiableSet(scannedCodecs) : definedCodecs;
    }
}