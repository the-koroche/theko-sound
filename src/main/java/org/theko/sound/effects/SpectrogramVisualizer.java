package org.theko.sound.effects;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;

import org.theko.sound.AudioFormat;
import org.theko.sound.fft.FFT;

public class SpectrogramVisualizer extends VisualAudioEffect {
    protected int fftSize = 1024;
    private float[] real;
    private float[] imag;
    private float[] magnitudes;

    protected SpectrogramPanel spectrogramPanel;
    protected BufferedImage spectrogramImage;
    protected Timer resizeTimer;

    protected SpectrogramColor spectrogramColor;
    protected Orientation orientation = Orientation.RIGHT;
    protected boolean reverse = true;

    public enum Orientation {
        TOP, BOTTOM, LEFT, RIGHT
    }

    public SpectrogramVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.real = new float[fftSize];
        this.imag = new float[fftSize];
        this.magnitudes = new float[fftSize / 2];
        this.spectrogramColor = new SpectrogramColor() {
            public Color getColor(float magnitude, float frequency) {
                // Нормализуем magnitude в диапазон 0..1
                float value = Math.min(1.0f, Math.max(0.0f, magnitude));
                
                // Палитра: черный -> темно-синий -> фиолетовый -> розовый -> белый
                float r, g, b;
                
                if (value < 0.25f) {
                    // Черный -> Темно-синий
                    r = 0.0f;
                    g = 0.0f;
                    b = value * 4.0f;
                } else if (value < 0.5f) {
                    // Темно-синий -> Фиолетовый
                    r = (value - 0.25f) * 4.0f;
                    g = 0.0f;
                    b = 1.0f;
                } else if (value < 0.75f) {
                    // Фиолетовый -> Розовый
                    r = 1.0f;
                    g = 0.0f;
                    b = 1.0f - (value - 0.5f) * 4.0f;
                } else {
                    // Розовый -> Белый
                    r = 1.0f;
                    g = (value - 0.75f) * 4.0f;
                    b = 1.0f;
                }
                
                return new Color(r, g, b);
            }
        };
    }

    @Override
    public void initializePanel() {
        spectrogramPanel = new SpectrogramPanel();
        
        resizeTimer = new Timer(true);
        resizeTimer.scheduleAtFixedRate(new TimerTask() {
            private int lastWidth = 600;
            private int lastHeight = 300;

            @Override
            public void run() {
                int newWidth = Math.max(1, spectrogramPanel.getWidth());
                int newHeight = Math.max(1, spectrogramPanel.getHeight());
                if (newWidth != lastWidth || newHeight != lastHeight) {
                    lastWidth = newWidth;
                    lastHeight = newHeight;
                    updateSpectrogramSize(newWidth, newHeight);
                }
            }
        }, 0, 500);
        
        SwingUtilities.invokeLater(() -> spectrogramPanel.setVisible(true));
    }

    private void updateSpectrogramSize(int width, int height) {
        if (spectrogramImage != null) {
            BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.drawImage(spectrogramImage, 0, 0, width, height, null);
            g2d.dispose();
            spectrogramImage = scaledImage;
        } else {
            spectrogramImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        spectrogramPanel.repaint();
    }

    @Override
    public void repaint() {
        SwingUtilities.invokeLater(() -> spectrogramPanel.repaint());
    }

    @Override
    public float[][] process(float[][] samples) {
        if (samples.length > 0 && samples[0].length >= fftSize) {
            System.arraycopy(samples[0], 0, real, 0, fftSize);
            for (int i = 0; i < fftSize; i++) imag[i] = 0;
    
            FFT.fft(real, imag);
    
            for (int i = 0; i < fftSize / 2; i++) {
                float amplitude = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
                magnitudes[i] = (float) (Math.log(amplitude + 1e-6) * 0.25);
            }
    
            updateSpectrogramImage();
        }
        return samples;
    }

    private void updateSpectrogramImage() {
        if (spectrogramImage == null || spectrogramImage.getWidth() <= 0 || spectrogramImage.getHeight() <= 0) {
            return;
        }
    
        Graphics2D g2d = spectrogramImage.createGraphics();
    
        boolean isHorz = orientation == Orientation.LEFT || orientation == Orientation.RIGHT;
        boolean isVert = orientation == Orientation.TOP || orientation == Orientation.BOTTOM;
    
        int width = spectrogramImage.getWidth();
        int height = spectrogramImage.getHeight();
    
        int targetAxis = (isVert ? width : height); // Используем ширину для вертикальной ориентации, высоту для горизонтальной
    
        // Массив пикселей, его длина зависит от targetAxis
        int[] pixels = new int[targetAxis];
        int maxBinIndex = fftSize / 2 - 1;
    
        for (int x = 0; x < targetAxis; x++) {
            double logPos = (double) x / (targetAxis - 1) * Math.log(maxBinIndex + 1);
            double binPos = Math.exp(logPos) - 1;
    
            int binLow = (int) Math.floor(binPos);
            int binHigh = (int) Math.ceil(binPos);
            float alpha = (float) (binPos - binLow);
    
            binLow = Math.min(Math.max(binLow, 0), maxBinIndex);
            binHigh = Math.min(binHigh, maxBinIndex);
    
            float magnitude = (binLow == binHigh) ? magnitudes[binLow] :
                    (1 - alpha) * magnitudes[binLow] + alpha * magnitudes[binHigh];
    
            Color color = spectrogramColor.getColor(magnitude, ((float)(x) / targetAxis) * 22000);
            if (reverse) {
                pixels[targetAxis-x-1] = color.getRGB();
            } else {
                pixels[x] = color.getRGB();
            }
        }
    
        // Создаём новое изображение с учетом ориентации
        BufferedImage currentLine = new BufferedImage(isHorz ? 1 : width, isVert ? 1 : height, BufferedImage.TYPE_INT_ARGB);
    
        if (isVert) {
            currentLine.setRGB(0, 0, width, 1, pixels, 0, width); // Вертикальная ориентация
        } else {
            currentLine.setRGB(0, 0, 1, height, pixels, 0, 1); // Горизонтальная ориентация
        }
    
        // Обновляем изображение в зависимости от ориентации
        switch (orientation) {
            case BOTTOM:
                g2d.copyArea(0, 0, width, height, 0, -1);
                g2d.drawImage(currentLine, 0, height - 1, null);
                break;
            case TOP:
                g2d.copyArea(0, 0, width, height, 0, 1);
                g2d.drawImage(currentLine, 0, 0, null);
                break;
            case RIGHT:
                g2d.copyArea(0, 0, width, height, -1, 0);
                g2d.drawImage(currentLine, width - 1, 0, null);
                break;
            case LEFT:
                g2d.copyArea(0, 0, width, height, 1, 0);
                g2d.drawImage(currentLine, 0, 0, null);
                break;
        }
    
        g2d.dispose();
    }
    

    private class SpectrogramPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (spectrogramImage != null) {
                g.drawImage(spectrogramImage, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    @Override
    public JPanel getPanel() {
        return spectrogramPanel;
    }

    public interface SpectrogramColor {
        Color getColor(float magnitude, float frequency);
    }
}
