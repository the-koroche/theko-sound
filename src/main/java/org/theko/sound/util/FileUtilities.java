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
}
