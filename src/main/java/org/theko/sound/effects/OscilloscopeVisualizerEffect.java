package org.theko.sound.effects;

import java.awt.*;
import java.awt.geom.GeneralPath;

import javax.swing.*;
import org.theko.sound.AudioFormat;
import org.theko.sound.InnerResourceLoader;

public class OscilloscopeVisualizerEffect extends VisualAudioEffect {
    protected OscilloscopePanel panel;

    protected Color waveColor = Color.GREEN;
    protected Color backgroundColor = Color.BLACK;
    protected int transitionDuration = 200;

    protected long lastUpdateTime = System.nanoTime();

    // Добавим параметр ширины волны
    private int waveWidth = 1; // 1 - стандартная ширина, больше - более растянутая волна

    public OscilloscopeVisualizerEffect(AudioFormat audioFormat) {
        super(audioFormat);
    }

    @Override
    public void initializeFrame() {
        frame = new JFrame("Oscilloscope Visualizer");
        panel = new OscilloscopePanel();

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setIconImage(new ImageIcon(InnerResourceLoader.class.getResource("icons\\oscilloscope_icon.png")).getImage());
        frame.add(panel);
        panel.setDoubleBuffered(true);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    @Override
    public void repaint() {
        panel.repaint();
    }

    @Override
    public void onDataReceived() {
        lastUpdateTime = System.nanoTime();

        SwingUtilities.invokeLater(panel::repaint);
    }

    public void setWaveColor(Color waveColor) {
        this.waveColor = waveColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTransitionDuration(int transitionDuration) {
        this.transitionDuration = transitionDuration;
    }

    // Метод для установки ширины волны
    public void setWaveWidth(int width) {
        this.waveWidth = Math.max(1, width); // Убедимся, что ширина волны всегда больше или равна 1
    }

    private class OscilloscopePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(backgroundColor);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int midY = height / 2;

            long elapsedMillis = (System.nanoTime() - lastUpdateTime) / 1_000_000;
            float progress = Math.min(elapsedMillis / (float) transitionDuration, 1.0f);

            // Draw previous buffer sliding out
            if (prevData != null && prevData.length > 0) {
                float offsetX = -width * progress;
                drawBuffer(g2d, prevData, midY, width, offsetX);
            }

            // Draw current buffer sliding in
            if (data != null && data.length > 0) {
                float offsetX = width * (1 - progress);
                drawBuffer(g2d, data, midY, width, offsetX);
            }
        }

        private void drawBuffer(Graphics2D g2d, byte[] data, int midY, int width, 
                               float offsetX) {
            g2d.setColor(waveColor);

            int sampleSize = audioFormat.getBitsPerSample() / 8;
            int sampleCount = data.length / sampleSize;
            int step = Math.max(1, sampleCount / (width * waveWidth)); // Используем waveWidth для изменения шага

            // Use a path for drawing the waveform
            GeneralPath wavePath = new GeneralPath();
            boolean first = true;

            for (int x = 0; x < width; x++) {
                int index = Math.min(x * step * sampleSize, data.length - sampleSize);
                int sample = getSample(data, index, sampleSize);
                int y = midY - (sample * midY / 32768);

                float xPos = x + offsetX;
                if (xPos >= 0 && xPos < width) {
                    if (first) {
                        wavePath.moveTo(xPos, y);
                        first = false;
                    } else {
                        wavePath.lineTo(xPos, y);
                    }
                }
            }

            g2d.draw(wavePath);
        }

        private int getSample(byte[] data, int index, int sampleSize) {
            // Normalize for 8-bit, 16-bit PCM data
            if (sampleSize == 1) {
                return (data[index] & 0xFF) - 128;  // Normalize to range -128 to 127
            } else if (sampleSize == 2) {
                return (short) ((data[index + 1] << 8) | (data[index] & 0xFF)); // PCM 16-bit
            }
            return 0;
        }
    }
}
