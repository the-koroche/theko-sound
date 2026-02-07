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

import java.util.Locale;

import org.theko.sound.properties.ThreadType;
import org.theko.sound.properties.ThreadConfiguration;

/**
 * Utility class for formatting data in a human-readable way.
 * It includes methods for formatting bytes, pointers, and thread information.
 * 
 * @since 2.3.2
 * @author Theko
 */
public final class FormatUtilities {

    public static final long KiBytes = 1024L;
    public static final long MiByets = 1_048_576L;
    public static final long GiByets = 1_073_741_824L;
    public static final long TiByets = 1_099_511_627_776L;
    public static final long PiByets = 1_125_899_906_842_624L;

    public static final long KByets = 1000L;
    public static final long MByets = 1_000_000L;
    public static final long GByets = 1_000_000_000L;
    public static final long TByets = 1_000_000_000_000L;
    public static final long PByets = 1_000_000_000_000_000L;

    public static final long KBits = 1000L;
    public static final long MBits = 1_000_000L;
    public static final long GBits = 1_000_000_000L;
    public static final long TBits = 1_000_000_000_000L;
    public static final long PBits = 1_000_000_000_000_000L;

    public static final long MICROS_NS = 1000;
    public static final long MILLIS_NS = 1_000_000;
    public static final long SECONDS_NS = 1_000_000_000L;
    public static final long MINUTES_NS = 60 * SECONDS_NS;
    public static final long HOURS_NS = 60 * MINUTES_NS;

    public static final long MINUTES = 60L;
    public static final long HOURS = 60 * MINUTES;
    
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
    public static String formatBytes(long bytes, boolean binary, int precision) {
        return binary ? formatBytesBinary(bytes, precision) : formatBytesDecimal(bytes, precision);
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytes(long bytes, int precision) {
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
        if (bytes < KiBytes) return bytes + " B";
        else if (bytes < MiByets) return formatAdaptive(bytes / (double) KiBytes, precision) + " KiB";
        else if (bytes < GiByets) return formatAdaptive(bytes / (double) MiByets, precision) + " MiB";
        else if (bytes < TiByets) return formatAdaptive(bytes / (double) GiByets, precision) + " GiB";
        else if (bytes < PiByets) return formatAdaptive(bytes / (double) TiByets, precision) + " TiB";
        else return formatAdaptive(bytes / (double) PiByets, precision) + " PiB";
    }

    /**
     * Formats bytes in a human-readable way using decimal units.
     * 
     * @param bytes The number of bytes to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBytesDecimal(long bytes, int precision) {
        if (bytes < KByets) return bytes + " B";
        else if (bytes < MByets) return formatAdaptive(bytes / (double) KByets, precision) + " KB";
        else if (bytes < GByets) return formatAdaptive(bytes / (double) MByets, precision) + " MB";
        else if (bytes < TByets) return formatAdaptive(bytes / (double) GByets, precision) + " GB";
        else if (bytes < PByets) return formatAdaptive(bytes / (double) TByets, precision) + " TB";
        else return formatAdaptive(bytes / (double) PByets, precision) + " PB";
    }

    /**
     * Formats bits in a human-readable way using decimal units.
     * 
     * @param bits The number of bits to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatBits(long bits, int precision) {
        if (bits < KBits) return bits + " b";
        else if (bits < MBits) return formatAdaptive(bits / (double) KBits, precision) + " Kb";
        else if (bits < GBits) return formatAdaptive(bits / (double) MBits, precision) + " Mb";
        else if (bits < TBits) return formatAdaptive(bits / (double) GBits, precision) + " Gb";
        else if (bits < PBits) return formatAdaptive(bits / (double) TBits, precision) + " Tb";
        else return formatAdaptive(bits / (double) PBits, precision) + " Pb";
    }

    /**
     * Formats a time in seconds to a human-readable string.
     * 
     * <p>
     * For times greater than or equal to 2 minutes, the format is "hh:mm:ss(.xxx)" or "mm:ss(.xxx)".
     * For times less than 2 minutes, the format is "xx.xxx ms", "xx.xxx us", or "xx.xxx ns".
     * 
     * 
     * @param ns The time in nanoseconds to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatTime(long ns, int precision) {
        double seconds = ns / (double) SECONDS_NS;

        // For longer times, use hh:mm:ss(.fff) or mm:ss(.fff)
        if (seconds >= 120) {
            int hours = (int) (seconds / HOURS);
            int minutes = (int) ((seconds % HOURS) / MINUTES);
            double secs = seconds % MINUTES;

            String secStr;
            if (precision > 0) {
                secStr = String.format(Locale.US, "%0" + (precision + 3) + "." + precision + "f", secs);
            } else {
                secStr = String.format(Locale.US, "%02d", (int) Math.round(secs));
            }

            if (precision > 0 && secStr.indexOf('.') == 1 && secs < 10)
                secStr = "0" + secStr;

            if (hours > 0) {
                // H:MM:SS(.xxx)
                return String.format(Locale.US, "%d:%02d:%s s", hours, minutes, secStr);
            } else {
                // MM:SS(.xxx)
                return String.format(Locale.US, "%d:%s s", minutes, secStr);
            }
        }

        if (ns < MICROS_NS) return ns + " ns";
        else if (ns < MILLIS_NS) return formatAdaptive(ns / (double) MICROS_NS, precision) + " us";
        else if (ns < SECONDS_NS) return formatAdaptive(ns / (double) MILLIS_NS, precision) + " ms";
        else if (ns < MINUTES_NS) return formatAdaptive(ns / (double) SECONDS_NS, precision) + " s";
        else if (ns < HOURS_NS) return formatAdaptive(ns / (double) MINUTES_NS, precision) + " min";
        else return formatAdaptive(ns / (double) HOURS_NS, precision) + " h";
    }

    /**
     * Formats time in a human-readable way from microseconds.
     * 
     * @param us The number of microseconds to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatTimeMicros(long us, int precision) {
        return formatTime(us * MICROS_NS, precision);
    }

    /**
     * Formats time in a human-readable way from milliseconds.
     * 
     * @param ms The number of milliseconds to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatTimeMillis(long ms, int precision) {
        return formatTime(ms * MILLIS_NS, precision);
    }

    /**
     * Formats time in a human-readable way from seconds.
     * 
     * @param sec The number of seconds to format.
     * @param precision The number of decimal places to use.
     * @return The formatted string.
     */
    public static String formatTimeSeconds(long sec, int precision) {
        return formatTime(sec * SECONDS_NS, precision);
    }

    /**
     * Returns a string representation of the given double value with adaptive formatting.
     * <p>
     * The number is rounded to the specified number of decimal places.  
     * Trailing zeros and an unnecessary decimal point are removed, so the result
     * may be shorter than the requested precision. If the rounded value is an integer,
     * it is formatted without a fractional part.
     * 
     *
     * <p>Examples (with {@code precision = 3}):
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
        String s = String.format(Locale.US, format, rounded);
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
     * @param threadConfig The thread config to format
     * @return The formatted string.
     */
    public static String formatThreadInfo(ThreadConfiguration threadConfig) {
        return String.format("(%s, %d)", threadConfig.threadType.toString(), threadConfig.priority);
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
        return String.format("(%s, %d)", type.toString(), priority);
    }
}
