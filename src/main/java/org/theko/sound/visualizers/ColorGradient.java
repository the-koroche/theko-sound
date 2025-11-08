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

package org.theko.sound.visualizers;

import java.awt.Color;
import java.util.List;

/**
 * Represents a linear color gradient.
 * Allows retrieving interpolated colors based on a normalized value or a float index.
 * 
 * @since 2.3.2
 * @author Theko
 */
public class ColorGradient {

    private static final List<Color> GRAYSCALE_COLORS = List.of(
        new Color(0, 0, 0), // black
        new Color(255, 255, 255) // white
    );
    /**
     * Grayscale color map ranging from black to white.
     */
    public static final ColorGradient GRAYSCALE_COLOR_MAP = fromColors(GRAYSCALE_COLORS);

    private static final List<Color> HOT_COLORS = List.of(
        new Color(0, 0, 0),       // Black
        new Color(128, 0, 0),     // Dark red
        new Color(255, 0, 0),     // Red
        new Color(255, 128, 0),   // Orange
        new Color(255, 255, 0),   // Yellow
        new Color(255, 255, 255)  // White
    );
    /**
     * Classic "hot" color map progressing from black through red, orange, yellow to white.
     */
    public static final ColorGradient HOT_COLOR_MAP = fromColors(HOT_COLORS);

    private static final List<Color> INFERNO_COLORS = List.of(
        new Color(250, 253, 161), // Light yellow
        new Color(251, 182, 26),  // Orange
        new Color(237, 105, 37),  // Red
        new Color(188, 55, 84),   // Dark pink
        new Color(120, 28, 109),  // Purple
        new Color(50, 10, 94),    // Dark purple
        new Color(0, 0, 0)        // Black
    );
    /**
     * Inferno color map, perceptually uniform gradient from light yellow to deep purple and black.
     */
    public static final ColorGradient INFERNO_COLOR_MAP = fromColors(INFERNO_COLORS);

    private static final List<Color> BRIGHT_INFERNO_COLORS = List.of(
        new Color(255, 255, 180), // Light yellow
        new Color(255, 210, 100), // Yellow-orange
        new Color(255, 140, 80),  // Bright orange
        new Color(255, 80, 100),  // Pinkish red
        new Color(220, 60, 140),  // Magenta
        new Color(160, 80, 200)   // Light violet
    );
    /**
     * Bright Inferno color map, a vivid artistic variant of the Inferno palette.
     */
    public static final ColorGradient BRIGHT_INFERNO_COLOR_MAP = fromColors(BRIGHT_INFERNO_COLORS);

    private static final List<Color> PLASMA_COLORS = List.of(
        new Color(13, 8, 135),    // Deep blue
        new Color(75, 3, 161),    // Violet
        new Color(125, 3, 168),   // Purple
        new Color(168, 34, 150),  // Magenta
        new Color(203, 70, 121),  // Pink
        new Color(229, 107, 93),  // Salmon
        new Color(248, 148, 65),  // Orange
        new Color(253, 195, 40),  // Yellow-orange
        new Color(240, 249, 33)   // Bright yellow
    );
    /**
     * Plasma color map, a smooth gradient from deep blue to bright yellow.
     */
    public static final ColorGradient PLASMA_COLOR_MAP = fromColors(PLASMA_COLORS);

    private static final List<Color> MAGMA_COLORS = List.of(
        new Color(0, 0, 4),       // Almost black
        new Color(28, 16, 68),    // Deep purple
        new Color(79, 18, 123),   // Purple
        new Color(129, 37, 129),  // Magenta
        new Color(181, 54, 122),  // Pinkish red
        new Color(229, 80, 100),  // Reddish
        new Color(251, 135, 97),  // Orange-pink
        new Color(254, 194, 135), // Peach
        new Color(252, 253, 191)  // Light cream
    );
    /**
     * Magma color map, a perceptually uniform palette from deep purple to light cream.
     */
    public static final ColorGradient MAGMA_COLOR_MAP = fromColors(MAGMA_COLORS);

    private static final List<Color> VIRIDIS_COLORS = List.of(
        new Color(68, 1, 84),     // Dark violet
        new Color(71, 44, 122),   // Blue-violet
        new Color(59, 81, 139),   // Indigo
        new Color(44, 113, 142),  // Teal-blue
        new Color(33, 144, 141),  // Turquoise
        new Color(39, 173, 129),  // Green-turquoise
        new Color(92, 200, 99),   // Green
        new Color(170, 220, 50),  // Yellow-green
        new Color(253, 231, 37)   // Bright yellow
    );
    /**
     * Viridis color map, a perceptually uniform gradient from dark violet to bright yellow.
     */
    public static final ColorGradient VIRIDIS_COLOR_MAP = fromColors(VIRIDIS_COLORS);

    private static final List<Color> RAINBOW_COLORS = List.of(
        new Color(255, 0, 0),     // Red
        new Color(255, 127, 0),   // Orange
        new Color(255, 255, 0),   // Yellow
        new Color(0, 255, 0),     // Green
        new Color(0, 255, 255),   // Cyan
        new Color(0, 0, 255),     // Blue
        new Color(139, 0, 255)    // Violet
    );
    /**
     * Rainbow color map that covers the full visible spectrum from red through violet.
     */
    public static final ColorGradient RAINBOW_COLOR_MAP = fromColors(RAINBOW_COLORS);

    private static final List<Color> OCEAN_COLORS = List.of(
        new Color(0, 16, 64),     // Deep navy
        new Color(0, 48, 128),    // Deep blue
        new Color(0, 96, 192),    // Medium blue
        new Color(0, 160, 255),   // Light blue
        new Color(128, 255, 255)  // Aqua
    );
    /**
     * Ocean color map that transitions from deep navy through blue to light aqua.
     */
    public static final ColorGradient OCEAN_COLOR_MAP = fromColors(OCEAN_COLORS);

    private int[] colors;
    
    private ColorGradient() {}

    /**
     * Creates a color gradient from a list of colors in 0xAARRGGBB format.
     * @param colors A list of colors in 0xAARRGGBB format.
     * @return A color gradient.
     */
    public static ColorGradient fromIntColors(int[] colors) {
        if (colors == null || colors.length == 0) {
            throw new IllegalArgumentException("Color array cannot be null, or empty.");
        }
        ColorGradient gradient = new ColorGradient();
        gradient.colors = colors;
        return gradient;
    }

    /**
     * Creates a color gradient from a list of colors.
     * @param colors A list of colors.
     * @return A color gradient.
     */
    public static ColorGradient fromColors(List<Color> colors) {
        ColorGradient gradient = new ColorGradient();
        gradient.colors = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            gradient.colors[i] = colors.get(i).getRGB();
        }
        
        if (gradient.colors == null || gradient.colors.length == 0) {
            throw new IllegalArgumentException("Color array cannot be null, or empty.");
        }
        return gradient;
    }

    /**
     * Returns a volume color processor that maps a volume value to a color from the gradient.
     * @return A volume color processor.
     */
    public VolumeColorProcessor getVolumeColorProcessor() {
        return new VolumeColorProcessor() {
            @Override
            public int getColor(float volume) {
                return getColorFromNormalizedValue(volume);
            }
        };
    }

    /**
     * Retrieves a color from a normalized value.
     * @param normalizedValue A value between 0 and 1.
     * @return An interpolated color in 0xAARRGGBB format.
     */
    public int getColorFromNormalizedValue(float normalizedValue) {
        return getColor(normalizedValue * (colors.length - 1));
    }

    /**
     * Retrieves a color from a float index.
     * @param floatIndex A float index.
     * @return An interpolated color in 0xAARRGGBB format.
     */
    public int getColor(float floatIndex) {
        int index1 = (int) Math.floor(floatIndex);
        int index2 = Math.min(index1 + 1, colors.length - 1);
        float frac = floatIndex - index1;

        int c1 = colors[index1];
        int c2 = colors[index2];

        int c1a = (c1 >> 24) & 0xFF;
        int c1r = (c1 >> 16) & 0xFF;
        int c1g = (c1 >> 8) & 0xFF;
        int c1b = c1 & 0xFF;

        int c2a = (c2 >> 24) & 0xFF;
        int c2r = (c2 >> 16) & 0xFF;
        int c2g = (c2 >> 8) & 0xFF;
        int c2b = c2 & 0xFF;

        int r = (int) (c1r + frac * (c2r - c1r));
        int g = (int) (c1g + frac * (c2g - c1g));
        int b = (int) (c1b + frac * (c2b - c1b));
        int a = (int) (c1a + frac * (c2a - c1a));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
