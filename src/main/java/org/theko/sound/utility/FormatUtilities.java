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
 * It includes methods for formatting bytes, pointers, and thread information.
 * 
 * @since 2.3.2
 * @author Theko
 */
public final class FormatUtilities {
    
    private FormatUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Formats bytes in a human-readable way using the specified unit system.
     * 
     * @param bytes The number of bytes to format.
     * @param binary True to use binary units, false to use decimal units.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytes (long bytes, boolean binary, int precision) {
        return binary ? formatBytesBinary(bytes, precision) : formatBytesDecimal(bytes, precision);
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytes (long bytes, int precision) {
        return formatBytesDecimal(bytes, precision);
    }

    /**
     * Formats bytes in a human-readable way using binary units.
     * 
     * @param bytes The number of bytes to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytesBinary(long bytes, int precision) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1_048_576L) return formatAdaptive(bytes / 1024.0, precision) + " KiB";
        else if (bytes < 1_073_741_824L) return formatAdaptive(bytes / 1_048_576.0, precision) + " MiB";
        else if (bytes < 1_099_511_627_776L) return formatAdaptive(bytes / 1_073_741_824.0, precision) + " GiB";
        else return formatAdaptive(bytes / 1_099_511_627_776.0, precision) + " TiB";
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytesDecimal(long bytes, int precision) {
        if (bytes < 1000) return bytes + " B";
        else if (bytes < 1_000_000) return formatAdaptive(bytes / 1000.0, precision) + " KB";
        else if (bytes < 1_000_000_000L) return formatAdaptive(bytes / 1_000_000.0, precision) + " MB";
        else if (bytes < 1_000_000_000_000L) return formatAdaptive(bytes / 1_000_000_000.0, precision) + " GB";
        else return formatAdaptive(bytes / 1_000_000_000_000.0, precision) + " TB";
    }

    /**
     * Returns a string representation of the given double value with adaptive formatting.
     * <p>
     * The number is rounded to the specified number of decimal places.  
     * Trailing zeros and an unnecessary decimal point are removed, so the result
     * may be shorter than the requested precision. If the rounded value is an integer,
     * it is formatted without a fractional part.
     * </p>
     *
     * <p>Examples (with {@code precision = 3}):</p>
     * <ul>
     *   <li>{@code 12.3456 -> "12.346"}</li>
     *   <li>{@code 12.3000 -> "12.3"}</li>
     *   <li>{@code 12.0000 -> "12"}</li>
     * </ul>
     *
     * @param value     the value to format
     * @param precision the maximum number of decimal digits to keep;
     *                  if negative, treated as {@code 0}
     * @return the formatted string without unnecessary trailing zeros
     */
    public static String formatAdaptive(double value, int precision) {
        if (precision < 0) precision = 0;

        double factor = Math.pow(10, precision);
        double rounded = Math.round(value * factor) / factor;
        
        if (rounded == (long) rounded) {
            return String.format("%d", (long) rounded);
        }
        String format = "%." + precision + "f";
        String s = String.format(format, rounded);
        while (s.contains(".") && s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Formats a pointer in a human-readable way, using the format "0xXXXXXXXXXXXXXXXX".
     * 
     * @param pointer The pointer to format.
     * @return The formatted string.
     */
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
    public static String formatThreadInfo(boolean isPlatform, int priority) {
        return String.format("(%s, %d)", (isPlatform ? "Platform" : "Virtual"), priority);
    }

    /**
     * Formats thread information in a human-readable way.
     * 
     * @param type The type of the thread (virtual or platform).
     * @param priority The priority of the thread.
     * @return The formatted string.
     */ 
    public static String formatThreadInfo(ThreadType type, int priority) {
        return String.format("(%s, %d)", (type == ThreadType.PLATFORM ? "Platform" : "Virtual"), priority);
    }
}
