package org.theko.sound.effects;

import java.awt.*;
import javax.swing.*;
import org.theko.sound.AudioFormat;

public class VolumeVisualizer extends VisualAudioEffect {
    private static final float PEAK_DECAY = 0.02f;
    private static final float FALL_SPEED = 0.05f;
    private boolean smoothFall = true;
    
    protected int channelsCount = 2;
    protected float[] volume;
    protected float[] peakVolume;
    protected VolumePanel volumePanel;

    public VolumeVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        volume = new float[channelsCount];
        peakVolume = new float[channelsCount];
    }

    @Override
    public void initializePanel() {
        volumePanel = new VolumePanel();
        SwingUtilities.invokeLater(() -> volumePanel.setVisible(true));
    }

    @Override
    public JPanel getPanel() {
        return volumePanel;
    }

    @Override
    protected void repaint() {
        volumePanel.repaint();
    }

    @Override
    public float[][] process(float[][] samples) {
        channelsCount = samples.length;
        if (volume.length != channelsCount) {
            volume = new float[channelsCount];
            peakVolume = new float[channelsCount];
        }

        for (int ch = 0; ch < channelsCount; ch++) {
            float maxVal = 0;
            for (float sample : samples[ch]) {
                maxVal = Math.max(maxVal, Math.abs(sample));
            }
            volume[ch] = smoothFall ? Math.max(volume[ch] - FALL_SPEED, maxVal) : maxVal;
            if (maxVal > peakVolume[ch]) {
                peakVolume[ch] = maxVal;
            } else {
                peakVolume[ch] -= PEAK_DECAY;
            }
        }
        return samples;
    }

    private Color getVolumeColor(float level) {
        float r = Math.min(1, Math.max(0, (level - 0.7f) * 5));
        float g = Math.min(1, Math.max(0, (1.0f - level) * 5));
        return new Color(r, g, 0);
    }

    private class VolumePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (volume == null || volume.length == 0) return;
            
            int bars = channelsCount;
            int barWidth = getWidth() / bars;
            
            for (int ch = 0; ch < channelsCount; ch++) {
                int x = ch * barWidth;
                int barHeight = (int) (volume[ch] * getHeight());
                int peakHeight = (int) (peakVolume[ch] * getHeight());
                
                g.setColor(getVolumeColor(volume[ch]));
                g.fillRect(x, getHeight() - barHeight, barWidth - 2, barHeight);
                
                g.setColor(Color.WHITE);
                g.fillRect(x, getHeight() - peakHeight, barWidth - 2, 2);
            }
        }
    }
}