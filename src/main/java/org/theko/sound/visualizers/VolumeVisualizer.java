package org.theko.sound.visualizers;

import java.awt.*;
import javax.swing.*;
import org.theko.sound.AudioFormat;

public class VolumeVisualizer extends AudioVisualizer {
    protected float peakFallSpeed = 0.0002f;
    protected float fallSpeed = 0.005f;
    protected boolean smoothFall = true;

    protected float[] volume;
    protected float[] peakVolume;
    protected float[] velFall;
    protected float[] velPeak;
    protected VolumePanel volumePanel;

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
        if (level < 0.7f) {
            return new Color(0, 255, 0); // Зеленый
        } else if (level < 0.9f) {
            return new Color(255, 165, 0); // Оранжевый
        } else {
            return new Color(255, 0, 0); // Красный
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

                // Падение громкости
                if (volume[ch] < maxVal) {
                    velFall[ch] = 0;
                } else {
                    velFall[ch] += fallSpeed;
                }
                volume[ch] = smoothFall ? Math.max(volume[ch] - velFall[ch], maxVal) : maxVal;

                // Падение пика
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
