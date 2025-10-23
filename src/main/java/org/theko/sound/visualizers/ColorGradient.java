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
 * Useful for visualizers, heatmaps, and other color-based representations./
 * 
 * @since 2.3.2
 * @author Theko
 */
public class ColorGradient {

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
     * Retrieves a color from a normalized value.
     * @param normalizedValue A value between 0 and 1.
     * @return An interpolated color.
     */
    public int getColorFromNormalizedValue(float normalizedValue) {
        return getColor(normalizedValue * (colors.length - 1));
    }

    /**
     * Retrieves a color from a float index.
     * @param floatIndex A float index.
     * @return An interpolated color, in 0xAARRGGBB format.
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
