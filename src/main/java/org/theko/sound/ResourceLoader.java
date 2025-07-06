package org.theko.sound;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ResourceLoader {
    
    private ResourceLoader() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static InputStream getResourceStream (String resourceName) {
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

    public static File getResourceFile (String resourceName) {
        try (InputStream resourceStream = getResourceStream(resourceName)) {
            String prefix = resourceName.replaceAll("[^a-zA-Z0-9]", "_");
            if (prefix.length() < 3) prefix += "___";
            File tempFile = File.createTempFile(prefix, ".tmp");
            tempFile.deleteOnExit();
            Files.copy(resourceStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary file for resource: " + resourceName, e);
        }
    }
}
