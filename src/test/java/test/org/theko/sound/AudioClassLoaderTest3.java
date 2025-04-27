package test.org.theko.sound;

import org.theko.sound.AudioClassLoader;

import javax.swing.*;
import java.awt.*;

// Test the resource loading functionality of AudioClassLoader
public class AudioClassLoaderTest3 {
    public static void main(String[] args) {
        // Initialize the AudioClassLoader
        AudioClassLoader.getResourceAsFilePath("wave.png"); // Test loading a resource file

        // Create a frame and show the loaded image
        JFrame frame = new JFrame("AudioClassLoader Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        frame.setLayout(new BorderLayout());
        JLabel label = new JLabel(new ImageIcon(AudioClassLoader.getResourceAsFilePath("wave.png")));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        frame.add(label, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
