package org.theko.sound.visualizers;

import java.awt.Color;

public class AudioVisualizationUtilities {
    private AudioVisualizationUtilities () {
    }

    public static Color getGradient(float x, Color... colors) {
        if (colors == null || colors.length == 0) {
            throw new IllegalArgumentException("Color array cannot be null, or empty.");
        }
        if (x <= 0) return colors[0];
        if (x >= 1) return colors[colors.length - 1];

        float pos = x * (colors.length - 1);
        int index = (int) pos;
        float ratio = pos - index;

        Color c1 = colors[index];
        Color c2 = colors[index + 1];

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * ratio);

        return new Color(r, g, b, a);
    }
}
