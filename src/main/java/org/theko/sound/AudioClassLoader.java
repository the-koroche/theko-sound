package org.theko.sound;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

public class AudioClassLoader {
    private static final Reflections reflections = createReflections();

    private static Reflections createReflections() {
        Reflections reflections = null;
        try {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("") // Scan all packages.
                    .addScanners(Scanners.SubTypes) // Look for subtypes.
            );
        } catch (ReflectionsException ex) {
            ex.printStackTrace();
            reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound.direct", "org.theko.sound.codec.formats") // Fallback: scan only predefined classes.
                .addScanners(Scanners.SubTypes)
            );
        }
        return reflections;
    }

    public static Reflections getReflections() {
        return reflections;
    }
}
