package org.theko.sound.visualizers;

import java.awt.*;
import java.awt.geom.GeneralPath;

import javax.swing.*;
import org.theko.sound.AudioFormat;

public class OscilloscopeVisualizer extends AudioVisualizer {
    protected OscilloscopePanel oscilloscopePanel;
    protected Color waveColor;
    protected Color backgroundColor = Color.BLACK;
    protected int channelsCount = 0;

    private float[][] audioSamples; // Buffer for audio samples

    public OscilloscopeVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.audioSamples = new float[0][];
        this.waveColor = Color.WHITE;
    }

    @Override
    public void initialize() {
        SwingUtilities.invokeLater(() -> oscilloscopePanel.setVisible(true));
    }

    public void setWaveColor(Color waveColor) {
        this.waveColor = waveColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public float[][] process(float[][] samples) {
        audioSamples = samples;
        return samples;
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
                g2d.setColor(waveColor);

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
    }

    @Override
    public void repaint() {
        channelsCount = audioSamples.length;
        SwingUtilities.invokeLater(() -> oscilloscopePanel.repaint());
    }

    @Override
    public JPanel getPanel() {
        return oscilloscopePanel;
    }

    @Override
    public void onEnd() {
        // Cleanup
    }
}
