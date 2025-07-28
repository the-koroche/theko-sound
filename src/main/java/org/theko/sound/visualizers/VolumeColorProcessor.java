package org.theko.sound.visualizers;

import java.awt.Color;

/**
 * A functional interface that processes a volume and returns a color.
 * 
 * @since v2.3.2
 * @author Theko
 */
@FunctionalInterface
public interface VolumeColorProcessor {
    public Color getColor (float volume);
}
