package org.theko.sound.effects;

import java.awt.*;
import java.awt.geom.GeneralPath;

import javax.swing.*;
import org.theko.sound.AudioFormat;
import java.util.List;
import java.util.ArrayList;

public class OscilloscopeVisualizer extends VisualAudioEffect {
    protected List<Color> waveColors;  // List to store colors for each channel
    protected Color backgroundColor = Color.BLACK;

    private float[][] audioSamples; // Buffer for audio samples

    public OscilloscopeVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.audioSamples = new float[0][]; // Initialize empty buffer
        this.waveColors = new ArrayList<>();  // Initialize the list of colors
    }

    @Override
    public void initializeFrame() {
        frame = new JFrame("Oscilloscope Visualizer");

        frame.setSize(600, 300); // Set size for oscilloscope
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.add(new OscilloscopePanel());
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public void setWaveColors(List<Color> waveColors) {
        this.waveColors = waveColors;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public void repaint() {
        // This method should be called to update the oscilloscope
        // Update the samples to be displayed
        SwingUtilities.invokeLater(() -> frame.repaint());
    }

    @Override
    public float[][] process(float[][] samples) {
        // Store audio samples for visualization
        audioSamples = samples;
        repaint(); // Trigger repaint to refresh the visual
        return samples; // Return the samples unmodified
    }

    // Custom panel for drawing oscilloscope
    private class OscilloscopePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Set background color
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (audioSamples.length == 0) return;

            // Scale factor for adjusting the waveform to fit in the window
            float scaleX = (float) getWidth() / audioSamples[0].length;
            float scaleY = (float) getHeight() / 2;

            // Draw waveforms for each channel
            for (int channel = 0; channel < audioSamples.length; channel++) {
                // Assign color based on the channel index
                Color channelColor = (channel < waveColors.size()) ? waveColors.get(channel) : getDefaultColor(channel);
                g2d.setColor(channelColor);

                // Draw the waveform for the current channel
                GeneralPath path = new GeneralPath();
                path.moveTo(0, scaleY + audioSamples[channel][0] * scaleY);
                for (int i = 1; i < audioSamples[channel].length; i++) {
                    float x = i * scaleX;
                    float y = scaleY + audioSamples[channel][i] * scaleY;
                    path.lineTo(x, y);
                }
                g2d.draw(path);
            }
        }

        private Color getDefaultColor(int channelIndex) {
            // Assign default colors based on the channel index
            switch (channelIndex % 6) {
                case 0: return Color.GREEN;
                case 1: return Color.RED;
                case 2: return Color.BLUE;
                case 3: return Color.YELLOW;
                case 4: return Color.MAGENTA;
                default: return Color.CYAN;
            }
        }
    }
}
