/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

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
            String name = getTempFileName(resourceName);

            File dir = new File(System.getProperty("java.io.tmpdir"), "ThekoSound\\Unpacked");
            dir.mkdirs();

            File tempFile = new File(dir, name);
            tempFile.deleteOnExit();
            Files.copy(resourceStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary file for resource: " + resourceName, e);
        }
    }

    private static String getTempFileName(String resourceName) {
        int lastDot = resourceName.lastIndexOf('.');

        String withoutExtension = (lastDot == -1 ? resourceName : resourceName.substring(0, lastDot));
        String prefix = withoutExtension.replaceAll("[^a-zA-Z0-9]", "_");
        if (prefix.length() < 3) prefix += "_".repeat(3 - prefix.length());

        String extension = (lastDot == -1 ? ".tmp" : resourceName.substring(lastDot));

        String random = Long.toHexString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
        String randomPart = (random.length() > 8 ? random.substring(random.length() - 8) : random);

        return prefix + "_" + randomPart + extension;
    }
}
