/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

package org.theko.sound.util;

import java.io.File;

/**
 * Utility class for common file operations.
 *
 * @since 0.2.4-beta
 * @author Theko
 */
public final class FileUtilities {

    private FileUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Checks if any of the specified file names exist in the given directory.
     * <p>
     * If the {@code dir} is {@code null} or not a directory, this method
     * returns {@code false}.
     *
     * @param dir   the directory to search in (may be {@code null})
     * @param names the file names to check (must not be {@code null})
     * @return {@code true} if at least one file exists in the directory, {@code false} otherwise
     */
    public static boolean existsAny(File dir, String... names) {
        if (dir == null || !dir.isDirectory() || names == null) return false;
        for (String name : names) {
            File file = new File(dir, name);
            if (file.exists()) return true;
        }
        return false;
    }

    /**
     * Retrieves the file extension of the given file name.
     * <p>
     * If the file name is {@code null} or does not contain a dot (".") character,
     * this method returns an empty string.
     * <p>
     * If the file name ends with a dot (".") character, this method returns an empty string.
     *
     * @param fileName the file name to retrieve the extension from (may be {@code null})
     * @return the file extension, or an empty string if no extension is available
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return ""; // No extension or empty extension
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * Retrieves the file name without the extension of the given file name.
     * <p>
     * If the file name is {@code null} or does not contain a dot (".") character,
     * this method returns the original file name.
     * <p>
     * If the file name ends with a dot (".") character, this method returns the original file name.
     *
     * @param fileName the file name to retrieve the name without extension from (may be {@code null})
     * @return the file name without extension, or the original file name if no extension is available
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName; // No extension
        }
        return fileName.substring(0, lastDotIndex);
    }
}
