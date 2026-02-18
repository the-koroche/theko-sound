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

package org.theko.sound.util;

import java.util.Locale;
import java.util.Objects;

/**
 * Utility class for platform detection and native library name formatting.
 * <p>
 * Provides methods to detect the current operating system and to format
 * native library names according to platform-specific conventions.
 * For example, on Windows libraries typically end with {@code .dll},
 * while on macOS they end with {@code .dylib}, and on Unix-like systems
 * with {@code .so}.
 * 
 * 
 * @since 2.3.2
 * @author Theko
 */
public final class PlatformUtilities {

    private static final Platform platform;
    private static final Architecture architecture;

    /**
     * Enumeration of supported platforms.
     */
    public enum Platform {
        WINDOWS,
        MAC,
        LINUX,
        OTHER_UNIX,
        UNKNOWN
    }

    /**
     * Enumeration of supported architectures.
     */
    public enum Architecture {
        X86_32(32),
        X86_64(64),
        ARM_32(32),
        ARM_64(64),
        UNKNOWN(-1);

        private final int bits;
        Architecture(int bits) { this.bits = bits; }
        public int getBits() { return bits; }
    }
    
    private PlatformUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    static {
        platform = detectPlatform();
        architecture = detectArchitecture();
    }

    private static Platform detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (os.contains("win")) {
            return Platform.WINDOWS;
        } else if (os.contains("mac") || os.contains("darwin")) {
            return Platform.MAC;
        } else if (os.contains("nux") || os.contains("nix") || os.contains("aix")) {
            return Platform.LINUX;
        } else if (os.contains("sunos") || os.contains("bsd") || os.contains("unix")) {
            return Platform.OTHER_UNIX;
        } else {
            return Platform.UNKNOWN;
        }
    }

    private static Architecture detectArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

        switch (arch) {
            case "x86":
            case "i386":
            case "i486":
            case "i586":
            case "i686":
                return Architecture.X86_32;

            case "x86_64":
            case "amd64":
                return Architecture.X86_64;

            case "arm":
            case "arm32":
                return Architecture.ARM_32;

            case "aarch64":
            case "arm64":
                return Architecture.ARM_64;

            default:
                return Architecture.UNKNOWN;
        }
    }

    /**
     * Retrieves the detected platform.
     * 
     * @return The detected platform.
     */
    public static Platform getPlatform() {
        return platform;
    }

    /**
     * Retrieves the detected architecture.
     * 
     * @return The detected architecture.
     */
    public static Architecture getArchitecture() {
        return architecture;
    }

    /**
     * Retrieves the default library extension for the current platform ("dll", "dylib", "so", etc.).
     * 
     * @return The default library extension.
     */
    public static String getDefaultLibraryExtension() {
        switch (platform) {
            case WINDOWS: return "dll";
            case MAC:     return "dylib";
            case LINUX:
            case OTHER_UNIX:
            case UNKNOWN:
            default:      return "so";
        }
    }

    /**
     * Retrieves the default library prefix for the current platform ("", "lib", etc.).
     * 
     * @return The default library prefix.
     */
    public static String getDefaultLibraryPrefix() {
        switch (platform) {
            case WINDOWS: return "";
            case MAC:     return "lib";
            case LINUX:
            case OTHER_UNIX:
            case UNKNOWN:
            default:      return "lib";
        }
    }

    /**
     * Formats a library name based on the current platform.
     * Rules (shortened truth table):
     * <pre>
     * - only name → prefix + name + "." + suffix
     * - prefix + name → name + "." + suffix
     * - prefix + name + "." → name + suffix
     * - prefix + name + "." + correct suffix → name
     * - prefix + name + "." + incorrect suffix → name (w/o incorrect suffix) + suffix
     * - name + "." → prefix + name + suffix
     * - name + "." + correct suffix → prefix + name
     * - name + "." + incorrect suffix → prefix + name (w/o incorrect suffix) + suffix
     * - otherwise → name
     * </pre>
     * 
     * @param name The library name to format.
     * @return The formatted library name.
     */
    public static String formatLibraryName(String name) {
        Objects.requireNonNull(name);
        String prefix = getDefaultLibraryPrefix();
        String suffix = getDefaultLibraryExtension();

        boolean hasPrefix = name.startsWith(prefix) || name.startsWith("lib");
        int dotIndex = name.lastIndexOf('.');
        boolean hasDot = dotIndex != -1;

        String base = hasDot ? name.substring(0, dotIndex) : name;
        String ext = hasDot ? name.substring(dotIndex + 1) : "";

        // case: only name
        if (!hasPrefix && !hasDot) {
            return prefix + name + "." + suffix;
        }

        // case: prefix + name
        if (hasPrefix && !hasDot) {
            return name + "." + suffix;
        }

        // case: prefix + name + "."
        if (hasPrefix && hasDot && ext.isEmpty()) {
            return name + suffix;
        }

        // case: prefix + name + "." + correct suffix
        if (hasPrefix && hasDot && ext.equals(suffix)) {
            return name;
        }

        // case: prefix + name + "." + incorrect suffix
        if (hasPrefix && hasDot && !ext.equals(suffix)) {
            return base + "." + suffix;
        }

        // case: name + "."
        if (!hasPrefix && hasDot && ext.isEmpty()) {
            return prefix + name + suffix;
        }

        // case: name + "." + correct suffix
        if (!hasPrefix && hasDot && ext.equals(suffix)) {
            return prefix + base;
        }

        // case: name + "." + incorrect suffix
        if (!hasPrefix && hasDot && !ext.equals(suffix)) {
            return prefix + base + "." + suffix;
        }

        // fallback: return as-is
        return name;
    }
}
