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

package org.theko.sound.visualizers;

import java.awt.Color;
import java.util.List;

/**
 * Represents a linear color gradient.
 * Allows retrieving interpolated colors based on a normalized value or a float index.
 * 
 * @since 0.2.3-beta
 * @author Theko
 */
public class ColorGradient {

    private static final List<Color> GRAYSCALE_COLORS = List.of(
        new Color(0, 0, 0),      // Black   #000000
        new Color(255, 255, 255) // White   #ffffff
    );
    /**
     * Grayscale color map ranging from black to white.
     */
    public static final ColorGradient GRAYSCALE_COLOR_MAP = fromColors(GRAYSCALE_COLORS);

    private static final List<Color> HOT_COLORS = List.of(
        new Color(0, 0, 0),       // Black          #000000
        new Color(128, 0, 0),     // Dark red       #800000
        new Color(255, 0, 0),     // Red            #ff0000
        new Color(255, 128, 0),   // Orange         #ff8000
        new Color(255, 255, 0),   // Yellow         #ffff00
        new Color(255, 255, 255)  // White          #ffffff
    );
    /**
     * Classic "hot" color map progressing from black through red, orange, yellow to white.
     */
    public static final ColorGradient HOT_COLOR_MAP = fromColors(HOT_COLORS);

    private static final List<Color> INFERNO_COLORS = List.of(
        new Color(0, 0, 0),       // Black          #000000
        new Color(50, 10, 94),    // Dark purple    #320a5e
        new Color(120, 28, 109),  // Purple         #781c6d
        new Color(188, 55, 84),   // Dark pink      #bc3754
        new Color(237, 105, 37),  // Red            #ed6925
        new Color(251, 182, 26),  // Orange         #fbb61a
        new Color(250, 253, 161)  // Light yellow   #fafda1
    );
    /**
     * Inferno color map, perceptually uniform gradient from light yellow to deep purple and black.
     */
    public static final ColorGradient INFERNO_COLOR_MAP = fromColors(INFERNO_COLORS);

    private static final List<Color> BRIGHT_INFERNO_COLORS = List.of(
        new Color(255, 255, 180), // Light yellow   #ffffb4
        new Color(255, 210, 100), // Yellow-orange  #ffd264
        new Color(255, 140, 80),  // Bright orange  #ff8c50
        new Color(255, 80, 100),  // Pinkish red    #ff5064
        new Color(220, 60, 140),  // Magenta        #dc3c8c
        new Color(160, 80, 200)   // Light violet   #a050c8
    );
    /**
     * Bright Inferno color map, a vivid artistic variant of the Inferno palette.
     */
    public static final ColorGradient BRIGHT_INFERNO_COLOR_MAP = fromColors(BRIGHT_INFERNO_COLORS);

    private static final List<Color> PLASMA_COLORS = List.of(
        new Color(13, 8, 135),    // Deep blue      #0d0877
        new Color(75, 3, 161),    // Violet         #4b03a1
        new Color(125, 3, 168),   // Purple         #7d03a8
        new Color(168, 34, 150),  // Magenta        #a82296
        new Color(203, 70, 121),  // Pink           #cb4679
        new Color(229, 107, 93),  // Salmon         #e56b5d
        new Color(248, 148, 65),  // Orange         #f89441
        new Color(253, 195, 40),  // Yellow-orange  #fdc528
        new Color(240, 249, 33)   // Bright yellow  #f0f921
    );
    /**
     * Plasma color map, a smooth gradient from deep blue to bright yellow.
     */
    public static final ColorGradient PLASMA_COLOR_MAP = fromColors(PLASMA_COLORS);

    private static final List<Color> MAGMA_COLORS = List.of(
        new Color(0, 0, 4),       // Almost black   #000004
        new Color(28, 16, 68),    // Deep purple    #1c1044
        new Color(79, 18, 123),   // Purple         #4f126b
        new Color(129, 37, 129),  // Magenta        #812589
        new Color(181, 54, 122),  // Pinkish red    #b5367a
        new Color(229, 80, 100),  // Reddish        #e45064
        new Color(251, 135, 97),  // Orange-pink    #fb8761
        new Color(254, 194, 135), // Peach          #fec28b
        new Color(252, 253, 191)  // Light cream    #fcfdd7
    );
    /**
     * Magma color map, a perceptually uniform palette from deep purple to light cream.
     */
    public static final ColorGradient MAGMA_COLOR_MAP = fromColors(MAGMA_COLORS);

    private static final List<Color> VIRIDIS_COLORS = List.of(
        new Color(68, 1, 84),     // Dark violet        #440154
        new Color(71, 44, 122),   // Blue-violet        #472c7a
        new Color(59, 81, 139),   // Indigo             #3b518b
        new Color(44, 113, 142),  // Teal-blue          #2c718e
        new Color(33, 144, 141),  // Turquoise          #218d8d
        new Color(39, 173, 129),  // Green-turquoise    #27ad81
        new Color(92, 200, 99),   // Green              #5ec863
        new Color(170, 220, 50),  // Yellow-green       #aadc32
        new Color(253, 231, 37)   // Bright yellow      #fde725
    );
    /**
     * Viridis color map, a perceptually uniform gradient from dark violet to bright yellow.
     */
    public static final ColorGradient VIRIDIS_COLOR_MAP = fromColors(VIRIDIS_COLORS);

    private static final List<Color> RAINBOW_COLORS = List.of(
        new Color(255, 0, 0),     // Red     #ff0000
        new Color(255, 127, 0),   // Orange  #ff7f00
        new Color(255, 255, 0),   // Yellow  #ffff00
        new Color(0, 255, 0),     // Green   #00ff00
        new Color(0, 255, 255),   // Cyan    #00ffff
        new Color(0, 0, 255),     // Blue    #0000ff
        new Color(139, 0, 255)    // Violet  #8b00ff
    );
    /**
     * Rainbow color map that covers the full visible spectrum from red through violet.
     */
    public static final ColorGradient RAINBOW_COLOR_MAP = fromColors(RAINBOW_COLORS);

    private static final List<Color> OCEAN_COLORS = List.of(
        new Color(0, 16, 64),     // Deep navy      #001040
        new Color(0, 48, 128),    // Deep blue      #003080
        new Color(0, 96, 192),    // Medium blue    #0060c0
        new Color(0, 160, 255),   // Light blue     #00a0ff
        new Color(128, 255, 255)  // Aqua           #80ffff
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
     * Returns a new color gradient that is the reverse of the current one.
     * The order of the colors in the returned gradient is the same as the order of the colors in the current gradient, but in reverse.
     * @return A new reversed color gradient
     */
    public ColorGradient reverse() {
        int len = colors.length;
        int[] reversedColors = new int[len];
        for (int i = 0; i < len; i++) {
            reversedColors[i] = colors[len - 1 - i];
        }
        return fromIntColors(reversedColors);
    }

    /**
     * Returns a copy of the color gradient.
     *
     * @return A copy of the color gradient.
     */
    public ColorGradient copy() {
        return fromIntColors(colors);
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
