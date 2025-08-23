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

    private List<Color> colors;
    
    /**
     * Constructs a ColorGradient from a list of colors.
     * @param colors A list of colors.
     */
    public ColorGradient (List<Color> colors) {
        if (colors == null || colors.isEmpty()) {
            throw new IllegalArgumentException("Color array cannot be null, or empty.");
        }
        this.colors = colors;
    }

    /**
     * Retrieves a color from a normalized value.
     * @param normalizedValue A value between 0 and 1.
     * @return An interpolated color.
     */
    public Color getColorFromNormalizedValue (float normalizedValue) {
        return getColor (normalizedValue * (colors.size() - 1));
    }

    /**
     * Retrieves a color from a float index.
     * @param floatIndex A float index.
     * @return An interpolated color.
     */
    public Color getColor(float floatIndex) {
        if (colors == null || colors.isEmpty()) {
            throw new IllegalArgumentException("Color array cannot be null, or empty.");
        }

        int index1 = (int) Math.floor(floatIndex);
        int index2 = Math.min(index1 + 1, colors.size() - 1);
        float frac = floatIndex - index1;

        Color c1 = colors.get(index1);
        Color c2 = colors.get(index2);

        int r = (int) (c1.getRed() + frac * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + frac * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + frac * (c2.getBlue() - c1.getBlue()));
        int a = (int) (c1.getAlpha() + frac * (c2.getAlpha() - c1.getAlpha()));

        return new Color(r, g, b, a);
    }
}
