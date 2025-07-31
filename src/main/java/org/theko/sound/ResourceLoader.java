package org.theko.sound;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for loading resources from the classpath.
 * <p>
 * This class provides methods to load resources as streams or files.
 * It ensures that the resource name is valid and exists in the classpath.
 * </p>
 * 
 * @see AudioClassScanner
 *
 * @since v2.0.0
 * @author Theko
 */
public class ResourceLoader {
    
    private ResourceLoader() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Loads a resource as a stream from the classpath.
     * @param resourceName The name of the resource to load.
     * @return The resource as a stream.
     */
    public static InputStream getResourceStream(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("Resource name cannot be null or empty.");
        }
        if (!resourceName.startsWith("/")) {
            resourceName = "/" + resourceName;
        }
        InputStream stream = ResourceLoader.class.getResourceAsStream(resourceName);
        if (stream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }
        return stream;
    }

    /**
     * Loads a resource as a file from the classpath.
     * It creates a temporary file and copies the resource to it.
     * @param resourceName The name of the resource to load.
     * @return The resource as a file.
     */
    public static File getResourceFile(String resourceName) {
        try (InputStream resourceStream = getResourceStream(resourceName)) {
            String prefix = resourceName.replaceAll("[^a-zA-Z0-9]", "_");
            if (prefix.length() < 3) prefix += "___";
            String extension = resourceName.substring(resourceName.lastIndexOf('.'));
            File tempFile = File.createTempFile(prefix, extension);
            tempFile.deleteOnExit();
            Files.copy(resourceStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary file for resource: " + resourceName, e);
        }
    }
}
