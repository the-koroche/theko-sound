package org.theko.sound.visualizers;

import java.awt.*;
import javax.swing.*;
import org.theko.sound.AudioFormat;

/**
 * The VolumeVisualizer class is responsible for visualizing audio volume levels
 * in real-time. It extends the AudioVisualizer class and provides a graphical
 * representation of the volume and peak levels for each audio channel.
 * 
 * <p>Features include:
 * <ul>
 *   <li>Real-time volume visualization for multiple audio channels.</li>
 *   <li>Gradient-based volume bar coloring based on volume levels.</li>
 *   <li>Smooth fall behavior for volume and peak levels.</li>
 *   <li>Customizable fall speeds for volume and peak levels.</li>
 * </ul>
 * 
 * <p>Usage:
 * <ol>
 *   <li>Create an instance of VolumeVisualizer with the desired AudioFormat.</li>
 *   <li>Call the {@link #initialize()} method to set up the visualizer.</li>
 *   <li>Feed audio samples to the {@link #process(float[][])} method for processing.</li>
 *   <li>Retrieve the visualization panel using {@link #getPanel()} and add it to your UI.</li>
 * </ol>
 * 
 * <p>Key Components:
 * <ul>
 *   <li>{@code volume}: The current volume levels for each channel.</li>
 *   <li>{@code peakVolume}: The peak volume levels for each channel.</li>
 *   <li>{@code velFall}: The fall velocity for volume levels.</li>
 *   <li>{@code velPeak}: The fall velocity for peak levels.</li>
 *   <li>{@code VolumePanel}: A custom JPanel for rendering the volume bars.</li>
 * </ul>
 * 
 * <p>Color Coding:
 * <ul>
 *   <li>Green: Low volume levels (below 0.7).</li>
 *   <li>Orange: Medium volume levels (between 0.7 and 0.9).</li>
 *   <li>Red: High volume levels (above 0.9).</li>
 * </ul>
 * 
 * <p>Note: The visualizer assumes that audio samples are provided as a 2D float
 * array, where each sub-array represents the samples for a specific channel.
 * 
 * @see AudioVisualizer
 * 
 * @since v1.3.0
 * 
 * @author Theko
 */
public class VolumeVisualizer extends AudioVisualizer {
    /** The speed at which the peak volume falls down. */
    protected float peakFallSpeed = 0.0002f;

    /** The speed at which the volume falls down. */
    protected float fallSpeed = 0.005f;

    /** Whether to smoothly fall down the volume. */
    protected boolean smoothFall = true;

    /** * The current volume of each channel. */
    protected float[] volume;

    /** The current peak volume of each channel. */
    protected float[] peakVolume;

    /** The current fall velocity of each channel. */
    protected float[] velFall;

    /** The current peak fall velocity of each channel. */
    protected float[] velPeak;

    /** The panel to display the volume. */
    protected VolumePanel volumePanel;

    /** The audio samples to process. */
    protected float[][] samples;

    public VolumeVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        initializeArrays();
    }

    private void initializeArrays() {
        int channels = audioFormat.getChannels();
        volume = new float[channels];
        peakVolume = new float[channels];
        velFall = new float[channels];
        velPeak = new float[channels];
    }

    @Override
    public void initialize() {
        volumePanel = new VolumePanel();
        volumePanel.setOpaque(false);
        volumePanel.setBackground(new Color(0, 0, 0, 0)); // translucent
        SwingUtilities.invokeLater(() -> volumePanel.setVisible(true));
    }

    @Override
    public float[][] process(float[][] samples) {
        if (volume.length != audioFormat.getChannels()) {
            initializeArrays();
        }
        this.samples = samples;
        return samples;
    }

    private Color getVolumeColor(float level) {
        // The color of the volume bar
        if (level < 0.7f) {
            return new Color(0, 255, 0); // Green
        } else if (level < 0.9f) {
            return new Color(255, 165, 0); // Orange
        } else {
            return new Color(255, 0, 0); // Red
        }
    }

    private class VolumePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (volume == null || volume.length == 0) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setBackground(this.getBackground());
            int bars = audioFormat.getChannels();
            int barWidth = getWidth() / bars;

            for (int ch = 0; ch < audioFormat.getChannels(); ch++) {
                int x = ch * barWidth;
                int barHeight = (int) (volume[ch] * getHeight());
                int peakHeight = (int) (peakVolume[ch] * getHeight());

                Color startColor = getVolumeColor(0.0f);
                Color endColor = getVolumeColor(volume[ch]);
                GradientPaint gradient = new GradientPaint(x, (int) (getHeight() * 0.3), startColor, x, getHeight() - barHeight, endColor);
                g2d.setPaint(gradient);
                g2d.fillRect(x, getHeight() - barHeight, barWidth - 2, barHeight);

                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, getHeight() - peakHeight, barWidth - 2, 2);
            }
        }
    }

    @Override
    public JPanel getPanel() {
        return volumePanel;
    }

    @Override
    protected void repaint() {
        if (samples != null && samples.length > 0) {
            for (int ch = 0; ch < audioFormat.getChannels(); ch++) {
                float maxVal = 0;
                for (float sample : samples[ch]) {
                    maxVal = Math.max(maxVal, Math.abs(sample));
                }

                // Fall down the volume
                if (volume[ch] < maxVal) {
                    velFall[ch] = 0;
                } else {
                    velFall[ch] += fallSpeed;
                }
                volume[ch] = smoothFall ? Math.max(volume[ch] - velFall[ch], maxVal) : maxVal;

                // Fall down the peak
                if (volume[ch] > peakVolume[ch]) {
                    peakVolume[ch] = volume[ch];
                    velPeak[ch] = 0;
                } else {
                    peakVolume[ch] -= velPeak[ch];
                    velPeak[ch] += peakFallSpeed;
                }
            }
        }
        volumePanel.repaint();
    }

    @Override
    public void onEnd() {
        // Cleanup
    }
}
