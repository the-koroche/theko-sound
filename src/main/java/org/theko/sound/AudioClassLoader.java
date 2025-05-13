package org.theko.sound;

import java.io.File;
import java.net.URL;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * This method must be called before using any device / codec classes.
     * It initializes the Reflections library to scan for classes in the specified packages.
    */
    public static final void initialize() {
        if (reflections == null) {
            reflections = createReflections();
        } else {
            logger.info("Reflections instance already initialized.");
        }
    }

    /**
     * Tries to create a Reflections instance for class scanning.
     * If manual initialization is required (i.e., {@code org.theko.sound.AudioClassLoader.manual_init} is set to {@code true}),
     * this method returns {@code null} and logs a message indicating that manual initialization is required.
     * Otherwise, automatic initialization of Reflections is attempted.
     * If the automatic initialization fails, a warning is logged and a fallback Reflections instance is created.
     * The fallback instance scans only predefined packages containing device and codec classes.
     * @return A Reflections instance for class scanning, or {@code null} if manual initialization is required.
     */
    private static Reflections tryToCreateReflections() {
        String manualInit = System.getProperty("org.theko.sound.AudioClassLoader.manual_init", "false").toLowerCase();
        if (manualInit.equals("true")) {
            return null; // Manual initialization is required.
        } else {
            return createReflections(); // Automatic initialization.
        }
    }

    /**
     * Creates a Reflections instance for package scanning.
     * If the automatic initialization fails (e.g., due to a ReflectionsException),
     * a warning is logged and a fallback Reflections instance is created.
     * The fallback instance scans only predefined packages containing device and codec classes.
     * @return A Reflections instance for class scanning.
     */
    private static Reflections createReflections() {
        logger.debug("Creating Reflections instance for package scanning.");
        Reflections reflections = null;
        boolean fullSuccess = false;

        String useDefaultReflections = System.getProperty("org.theko.sound.AudioClassLoader.exclude_custom", "false").toLowerCase();
        if (useDefaultReflections.equals("true")) {
            logger.debug("Using default Reflections instance.");
            reflections = createDefaultReflections();
            return reflections;
        }
        try {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("") // Scan all packages.
                    .addScanners(Scanners.SubTypes) // Look for subtypes.
            );
            fullSuccess = true;
        } catch (ReflectionsException ex) {
            logger.warn("ReflectionsException", ex);
        }

        if (!fullSuccess) {
            logger.warn("Falling back to predefined classes.");
            reflections = createDefaultReflections();
        }
        return reflections;
    }

    private static Reflections createDefaultReflections() {
        return new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound.direct", "org.theko.sound.codec.formats") // Fallback: scan only predefined classes.
                .addScanners(Scanners.SubTypes)
        );
    }

    /**
     * Returns the Reflections instance for package scanning. The returned instance is
     * used by the AudioClassLoader to find available audio devices and codecs. If the
     * automatic initialization fails (e.g., due to a ReflectionsException), a warning
     * is logged and a fallback Reflections instance is created that only scans predefined
     * packages containing device and codec classes.
     * @return A Reflections instance for class scanning.
     */
    public static Reflections getReflections() {
        return reflections;
    }

    /**
     * Returns the resource at the given name as a File object.
     * 
     * @param name The name of the resource to retrieve.
     * @return A File object representing the resource, or throws an IllegalArgumentException if the resource is not found.
     */
    public static File getResourceAsFile(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Resource name cannot be null");
        }
        URL url = AudioClassLoader.class.getClassLoader().getResource(name);
        if (url == null) {
            logger.error("Resource not found: " + name);
            throw new IllegalArgumentException("Resource not found: " + name);
        }
        return new File(url.getPath());
    }

    /**
     * Returns the absolute file path of the resource at the given name.
     * @param name The name of the resource to retrieve.
     * @return The absolute file path of the resource, or throws an IllegalArgumentException if the resource is not found.
     */
    public static String getResourceAsFilePath(String name) {
        return getResourceAsFile(name).getAbsolutePath();
    }
}
