package org.theko.sound;

import java.io.File;
import java.net.URL;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.formats.WAVECodec;
import org.theko.sound.direct.AudioDevice;
import org.theko.sound.direct.javasound.JavaSoundDevice;
import org.theko.sound.direct.javasound.JavaSoundInput;
import org.theko.sound.direct.javasound.JavaSoundOutput;

/**
 * The {@code AudioClassLoader} class is responsible for managing the initialization
 * and usage of the Reflections library for scanning and discovering classes in
 * specified packages. It also provides utility methods for accessing resources
 * as files or file paths.
 * 
 * <p>This class includes methods to:
 * <ul>
 *   <li>Initialize the Reflections library either automatically or manually based on system properties.</li>
 *   <li>Retrieve the Reflections instance for class scanning.</li>
 *   <li>Access resources as {@link File} objects or their absolute file paths.</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>{@code
 * // Initialize the Reflections library
 * AudioClassLoader.initialize();
 * 
 * // Get the Reflections instance
 * Reflections reflections = AudioClassLoader.getReflections();
 * 
 * // Access a resource as a file
 * File resourceFile = AudioClassLoader.getResourceAsFile("example/resource.txt");
 * 
 * // Get the absolute file path of a resource
 * String resourcePath = AudioClassLoader.getResourceAsFilePath("example/resource.txt");
 * }</pre>
 * 
 * <p>System Properties:
 * <ul>
 *   <li>{@code org.theko.sound.AudioClassLoader.manual_init} - If set to {@code true},
 *       manual initialization of Reflections is required. Defaults to {@code false}.</li>
 *   <li>{@code org.theko.sound.AudioClassLoader.exclude_custom} - If set to {@code true},
 *       loads only predefined classes. Defaults to {@code false}.</li>
 * </ul>
 * 
 * <p>Logging:
 * <ul>
 *   <li>Logs initialization status and errors using SLF4J {@link Logger}.</li>
 *   <li>Warns and falls back to predefined packages if Reflections initialization fails.</li>
 * </ul>
 * 
 * <p>Note: This class is final and cannot be subclassed.
 * 
 * @author Alex Soloviov
 */
public final class AudioClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(AudioClassLoader.class);

    private static Reflections reflections = tryToCreateReflections();
    private static final Set<Class<? extends AudioDevice>> fallbackDevices = Set.of(
        JavaSoundDevice.class, JavaSoundInput.class, JavaSoundOutput.class
    );

    private static final Set<Class<? extends AudioCodec>> fallbackCodecs = Set.of(
        WAVECodec.class
    );

    public static void initialize() {
        if (reflections == null) {
            reflections = createReflections();
            if (reflections == null) {
                logger.warn("Unable to initialize Reflections. The predefined classes is still available.");
            }
        } else {
            logger.info("Reflections already initialized.");
        }
    }

    @Deprecated
    public static Reflections getReflections() {
        return reflections;
    }

    public static Set<Class<? extends AudioDevice>> getAvailableDevices() {
        if (useFallback()) {
            logger.debug("Returning fallback AudioDevice classes.");
            return fallbackDevices;
        }
        try {
            return reflections.getSubTypesOf(AudioDevice.class);
        } catch (Exception e) {
            logger.warn("Failed to scan AudioDevice classes. Falling back.", e);
            return fallbackDevices;
        }
    }

    public static Set<Class<? extends AudioCodec>> getAvailableCodecs() {
        if (useFallback()) {
            logger.debug("Returning fallback AudioCodec classes.");
            return fallbackCodecs;
        }
        try {
            return reflections.getSubTypesOf(AudioCodec.class);
        } catch (Exception e) {
            logger.warn("Failed to scan AudioCodec classes. Falling back.", e);
            return fallbackCodecs;
        }
    }

    private static boolean useFallback() {
        return "true".equalsIgnoreCase(System.getProperty("org.theko.sound.AudioClassLoader.exclude_custom", "false"));
    }

    private static Reflections tryToCreateReflections() {
        if ("true".equalsIgnoreCase(System.getProperty("org.theko.sound.AudioClassLoader.manual_init", "false"))) {
            logger.info("Manual initialization enabled. Reflections will not be created.");
            return null;
        }
        return createReflections();
    }

    private static Reflections createReflections() {
        logger.debug("Creating Reflections instance for scanning.");
        if (useFallback()) {
            return null;
        }
        try {
            return new Reflections(new ConfigurationBuilder()
                    .forPackages("")
                    .addScanners(Scanners.SubTypes));
        } catch (ReflectionsException ex) {
            logger.warn("Reflections failed, falling back to default packages.", ex);
            return createDefaultReflections();
        }
    }

    private static Reflections createDefaultReflections() {
        return new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound.direct", "org.theko.sound.codec.formats")
                .addScanners(Scanners.SubTypes));
    }

    public static File getResourceAsFile(String name) {
        if (name == null) throw new IllegalArgumentException("Resource name cannot be null");
        URL url = AudioClassLoader.class.getClassLoader().getResource(name);
        if (url == null) {
            logger.error("Resource not found: {}", name);
            throw new IllegalArgumentException("Resource not found: " + name);
        }
        return new File(url.getPath());
    }

    public static String getResourceAsFilePath(String name) {
        return getResourceAsFile(name).getAbsolutePath();
    }
}
