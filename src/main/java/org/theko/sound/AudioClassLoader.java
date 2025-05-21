package org.theko.sound;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.javasound.JavaSoundBackend;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.formats.WAVECodec;

/**
 * The {@code AudioClassLoader} is a utility class responsible for discovering and managing
 * audio backend and codec implementations at runtime. It leverages the Reflections library
 * to scan the classpath for implementations of {@link AudioBackend} and {@link AudioCodec},
 * allowing for extensibility via custom backends and codecs.
 * <p>
 * The class supports configuration via system properties:
 * <ul>
 *   <li>{@code org.theko.sound.classLoader.manual_init} - If set to {@code true}, disables automatic initialization and requires manual invocation of {@link #initialize()}.</li>
 *   <li>{@code org.theko.sound.classLoader.exclude_custom} - If set to {@code true}, disables scanning for custom backends/codecs and only uses predefined defaults.</li>
 * </ul>
 * <p>
 * The class provides methods to retrieve available audio backends and codecs, as well as
 * utility methods for accessing resources as files or file paths.
 * <p>
 * This class is not intended to be instantiated.
 * 
 * @since v1.2.0
 *
 * @author Theko
 */
public final class AudioClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(AudioClassLoader.class);
    public static final String SETTINGS_PROPERTY = "org.theko.sound.classLoader";

    private static final boolean MANUAL_INIT = Boolean.parseBoolean(
        System.getProperty(SETTINGS_PROPERTY + ".manual_init", "false"));

    private static final boolean EXCLUDE_CUSTOM = Boolean.parseBoolean(
        System.getProperty(SETTINGS_PROPERTY + ".exclude_custom", "false"));

    private static final Set<Class<? extends AudioBackend>> DEF_BACKEND_CLASSES = Set.of(
        JavaSoundBackend.class
    );

    private static final Set<Class<? extends AudioCodec>> DEF_CODEC_CLASSES = Set.of(
        WAVECodec.class
    );

    private static Reflections reflections;
    private static boolean initialized = false;

    static {
        if (!MANUAL_INIT && !EXCLUDE_CUSTOM) {
            initialize();
        }
    }

    private AudioClassLoader() {
    }

    /**
     * Initializes the Reflections library for scanning and discovering classes.
     * This method checks if initialization has already occurred to prevent
     * redundant execution. If the EXCLUDE_CUSTOM property is set, it skips
     * loading custom audio backends and codecs. In the absence of manual initialization
     * and exclusion, it attempts to configure Reflections with the specified package.
     * Logs initialization status and handles errors by falling back to predefined packages.
     */
    public static void initialize() {
        if (!initialized) {
            initialized = true;
        } else {
            logger.info("AudioClassLoader has already been initialized.");
            return;
        }
        if (EXCLUDE_CUSTOM) {
            logger.debug("Custom audio backends and codecs will not be loaded.");
            return;
        }

        try {
        reflections = new Reflections(new ConfigurationBuilder()
            .forPackage("" )
        );
        } catch (ReflectionsException e) {
            logger.error("Error initializing Reflections", e);
            logger.warn("Falling back to predefined packages.");
        }
    }

    /**
     * @deprecated As of v1.5.0, use {@link #getAvailableBackends()} and
     *             {@link #getAvailableCodecs()} instead to access the classes
     *             of available audio backends and codecs.
     */
    @Deprecated
    public static Reflections getReflections() {
        return reflections;
    }

    /**
     * Returns a set of all available audio backend classes, including custom ones if
     * not excluded by the {@code "exclude_custom"} property. If an error occurs during
     * the scanning process, it falls back to the predefined default backend classes.
     *
     * @return A set of all available audio backend classes.
     */
    public static Set<Class<? extends AudioBackend>> getAvailableBackends() {
        try {
            if (EXCLUDE_CUSTOM) {
                return DEF_BACKEND_CLASSES;
            }
            Set<Class<? extends AudioBackend>> backendClasses = reflections.getSubTypesOf(AudioBackend.class);
            backendClasses.addAll(DEF_BACKEND_CLASSES);
            return backendClasses;
        } catch (ReflectionsException e) {
            logger.error("Error scanning for audio backends", e);
            return DEF_BACKEND_CLASSES;
        }
    }

    public static Set<Class<? extends AudioCodec>> getAvailableCodecs() {
        try {
        /**
         * Returns a set of all available audio codec classes, including custom ones if
         * not excluded by the {@code "exclude_custom"} property. If an error occurs during
         * the scanning process, it falls back to the predefined default codec classes.
         *
         * @return A set of all available audio codec classes.
         */
            if (EXCLUDE_CUSTOM) {
                return DEF_CODEC_CLASSES;
            }
            Set<Class<? extends AudioCodec>> codecClasses = reflections.getSubTypesOf(AudioCodec.class);
            codecClasses.addAll(DEF_CODEC_CLASSES);
            return codecClasses;
        } catch (ReflectionsException e) {
            logger.error("Error scanning for audio codecs", e);
            return DEF_CODEC_CLASSES;
        }
    }

    /**
     * Returns the given resource as a File object. If the resource does not exist, an
     * IllegalArgumentException is thrown.
     *
     * @param name the name of the resource.
     * @return the resource as a File object.
     * @throws IllegalArgumentException if the resource does not exist.
     */
    public static File getResourceAsFile(String name) {
        if (name == null) throw new IllegalArgumentException("Resource name cannot be null");
        URL url = AudioClassLoader.class.getClassLoader().getResource(name);
        if (url == null) {
            logger.error("Resource not found: {}", name);
            throw new IllegalArgumentException("Resource not found: " + name);
        }
        return new File(url.getPath());
    }

    /**
     * Returns the given resource as a file path. If the resource does not exist, an
     * IllegalArgumentException is thrown.
     *
     * @param name the name of the resource.
     * @return the resource as a file path.
     * @throws IllegalArgumentException if the resource does not exist.
     */
    public static String getResourceAsFilePath(String name) {
        return getResourceAsFile(name).getAbsolutePath();
    }
}
