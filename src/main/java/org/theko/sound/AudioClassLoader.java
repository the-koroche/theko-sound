package org.theko.sound;

import java.io.File;
import java.net.URL;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static Reflections getReflections() {
        return reflections;
    }

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

    public static String getResourceAsFilePath(String name) {
        return getResourceAsFile(name).getAbsolutePath();
    }
}
