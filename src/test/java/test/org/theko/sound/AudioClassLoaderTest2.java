package test.org.theko.sound;

import org.reflections.Reflections;
import org.theko.sound.AudioClassLoader;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.direct.AudioDevice;

// Manual initialization test for AudioClassLoader
public class AudioClassLoaderTest2 {
    public static void main(String[] args) {
        // Initialize the AudioClassLoader
        AudioClassLoader.initialize();

        // Get the Reflections instance
        @SuppressWarnings("deprecation")
        Reflections reflections = AudioClassLoader.getReflections();
        if (reflections == null) {
            System.out.println("Reflections instance is null. Initialization failed.");
            return;
        }
        // Print the classes found in the specified packages
        System.out.println("Devices found in org.theko.sound.direct:");
        for (Class<?> clazz : reflections.getSubTypesOf(AudioDevice.class)) {
            System.out.println(clazz.getName());
        }
        System.out.println("Codecs found in org.theko.sound.direct:");
        for (Class<?> clazz : reflections.getSubTypesOf(AudioCodec.class)) {
            System.out.println(clazz.getName());
        }
    }
}
