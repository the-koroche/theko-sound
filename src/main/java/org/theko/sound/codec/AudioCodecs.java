package org.theko.sound.codec;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

public class AudioCodecs {
    private AudioCodecs() { }

    // A collection to hold all registered audio codecs
    private static final Collection<AudioCodecInfo> audioCodecs = Collections.synchronizedSet(new LinkedHashSet<>());


    static {
        registerCodecs();
    }

    /**
     * Register all audio codecs that are annotated with AudioCodecType
     */
    private static void registerCodecs() {
        // Use Reflections to find all classes implementing AudioCodec
        Reflections reflections = null;
        try {
            reflections = new Reflections(new ConfigurationBuilder()
                    .forPackages("") // Сканировать всё
                    .addScanners(Scanners.SubTypes) // Искать подтипы
            );
        } catch (ReflectionsException ex) {
            ex.printStackTrace();
            reflections = new Reflections(new ConfigurationBuilder()
                .forPackages("org.theko.sound.codec.formats") // Сканировать только установленные классы (пропустить пользовательские)
                .addScanners(Scanners.SubTypes) // Искать подтипы
            );
        }
        audioCodecs.clear();
        Set<Class<? extends AudioCodec>> allAudioCodecs = reflections.getSubTypesOf(AudioCodec.class);

        for (Class<? extends AudioCodec> audioCodecClass : allAudioCodecs) {
            if (audioCodecClass.isAnnotationPresent(AudioCodecType.class)) {
                AudioCodecInfo deviceInfo = new AudioCodecInfo(audioCodecClass);
                audioCodecs.add(deviceInfo);
                System.out.println(deviceInfo);
            }
        }
    }

    /**
     * Get an AudioCodecInfo by its name
     */
    public static AudioCodecInfo fromName(String name) throws AudioCodecNotFoundException {
        for (AudioCodecInfo audioCodec : audioCodecs) {
            if (audioCodec.getName().equalsIgnoreCase(name)) {
                return audioCodec;
            }
        }
        throw new AudioCodecNotFoundException("No audio codecs found by name: '" + name + "'.");
    }

    /**
     * Get an AudioCodecInfo by its exception
     * @param name File extension without a dot or an asterisk.
     */
    public static AudioCodecInfo fromExtension(String extension) throws AudioCodecNotFoundException {
        for (AudioCodecInfo audioCodec : audioCodecs) {
            if (audioCodec.getExtension().equalsIgnoreCase(extension)) {
                return audioCodec;
            }
        }
        throw new AudioCodecNotFoundException("No audio codecs found by extension: '" + extension + "'.");
    }

    public static AudioCodec getCodec(AudioCodecInfo codecInfo) throws AudioCodecCreationException {
        try {
            return codecInfo.getCodecClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new AudioCodecCreationException(e);
        }
    }

    /**
     * Get all registered audio codecs
     */
    public static Collection<AudioCodecInfo> getAllCodecs() {
        return audioCodecs;
    }
}
