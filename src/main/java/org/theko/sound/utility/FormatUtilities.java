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

package org.theko.sound.utility;

import org.theko.sound.properties.AudioSystemProperties.ThreadType;

/**
 * Utility class for formatting data in a human-readable way.
 * It includes methods for formatting bytes, thread information, and other.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class FormatUtilities {
    
    private FormatUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Formats bytes in a human-readable way using the specified unit system.
     * 
     * @param bytes The number of bytes to format.
     * @param binary True to use binary units, false to use decimal units.
     * @return The formatted string.
     */
    public static String formatBytes (long bytes, boolean binary) {
        return binary ? formatBytesBinary(bytes) : formatBytesDecimal(bytes);
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @return The formatted string.
     */
    public static String formatBytes (long bytes) {
        return formatBytesDecimal(bytes);
    }

    /**
     * Formats bytes in a human-readable way using binary units.
     * 
     * @param bytes The number of bytes to format.
     * @return The formatted string.
     */
    public static String formatBytesBinary (long bytes) {
        if (bytes < 1024) {
            return String.format("%d B", bytes);
        } else if (bytes < 1_048_576L) {
            return String.format("%.1f KiB", bytes / 1024.0);
        } else if (bytes < 1_073_741_824L) {
            return String.format("%.1f MiB", bytes / 1_048_576.0);
        } else if (bytes < 1_099_511_627_776L) {
            return String.format("%.1f GiB", bytes / 1_073_741_824.0);
        } else {
            return String.format("%.1f TiB", bytes / 1_099_511_627_776.0);
        }
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @return The formatted string.
     */
    public static String formatBytesDecimal (long bytes) {
        if (bytes < 1000) {
            return String.format("%d B", bytes);
        } else if (bytes < 1_000_000) {
            return String.format("%.1f KB", bytes / 1000.0);
        } else if (bytes < 1_000_000_000L) {
            return String.format("%.1f MB", bytes / 1_000_000.0);
        } else if (bytes < 1_000_000_000_000L) {
            return String.format("%.1f GB", bytes / 1_000_000_000.0);
        } else {
            return String.format("%.1f TB", bytes / 1_000_000_000_000.0);
        }
    }

    public static String formatPointer(long pointer) {
        return String.format("0x%016X", pointer);
    }

    /**
     * Formats thread information in a human-readable way.
     * 
     * @param isPlatform True if the thread is a platform thread, false if it's a virtual thread.
     * @param priority The priority of the thread.
     * @return The formatted string.
     */ 
    public static String formatThreadInfo (boolean isPlatform, int priority) {
        return String.format("Type: %s, Priority: %d", (isPlatform ? "Platform" : "Virtual"), priority);
    }

    /**
     * Formats thread information in a human-readable way.
     * 
     * @param type The type of the thread (virtual or platform).
     * @param priority The priority of the thread.
     * @return The formatted string.
     */ 
    public static String formatThreadInfo (ThreadType type, int priority) {
        return String.format("Type: %s, Priority: %d", (type == ThreadType.PLATFORM ? "Platform" : "Virtual"), priority);
    }
}
