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
            logger.info("Manual initialization of Reflections is required. Set 'org.theko.sound.AudioClassLoader.manual_init' to 'false' to enable automatic initialization.");
            return null; // Manual initialization is required.
        } else {
            logger.info("Automatic initialization of Reflections is enabled. Set 'org.theko.sound.AudioClassLoader.manual_init' to 'true' to disable automatic initialization.");
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
        try {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("") // Scan all packages.
                    .addScanners(Scanners.SubTypes) // Look for subtypes.
            );
        } catch (ReflectionsException ex) {
            logger.warn("ReflectionsException: " + ex.getMessage());
            logger.warn("Falling back to predefined classes.");
            ex.printStackTrace();
            reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound.direct", "org.theko.sound.codec.formats") // Fallback: scan only predefined classes.
                .addScanners(Scanners.SubTypes)
            );
        }
        return reflections;
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
