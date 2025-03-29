package org.theko.sound.effects;

import javax.swing.*;
import java.awt.*;
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

    private SpectrogramPanel spectrogramPanel;
    private BufferedImage spectrogramImage;
    private Timer resizeTimer;

    public SpectrogramVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.real = new float[fftSize];
        this.imag = new float[fftSize];
        this.magnitudes = new float[fftSize / 2];
    }

    @Override
    public void initializeFrame() {
        frame = new JFrame("Spectrogram Visualizer");
        frame.setSize(600, 300);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        
        spectrogramPanel = new SpectrogramPanel();
        frame.add(spectrogramPanel);
        
        resizeTimer = new Timer(true);
        resizeTimer.scheduleAtFixedRate(new TimerTask() {
            private int lastWidth = 600;
            private int lastHeight = 300;

            @Override
            public void run() {
                int newWidth = Math.max(1, frame.getWidth());
                int newHeight = Math.max(1, frame.getHeight());
                if (newWidth != lastWidth || newHeight != lastHeight) {
                    lastWidth = newWidth;
                    lastHeight = newHeight;
                    updateSpectrogramSize(newWidth, newHeight);
                }
            }
        }, 0, 500);
        
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
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
                magnitudes[i] = (float) (Math.log(amplitude + 1e-6) * 0.3);
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
        int width = spectrogramImage.getWidth();
        int height = spectrogramImage.getHeight();
    
        BufferedImage currentLine = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width];
        int maxBinIndex = fftSize / 2 - 1;
    
        for (int x = 0; x < width; x++) {
            double logPos = (double) x / (width - 1) * Math.log(maxBinIndex + 1);
            double binPos = Math.exp(logPos) - 1;
    
            int binLow = (int) Math.floor(binPos);
            int binHigh = (int) Math.ceil(binPos);
            float alpha = (float) (binPos - binLow);
    
            binLow = Math.min(Math.max(binLow, 0), maxBinIndex);
            binHigh = Math.min(binHigh, maxBinIndex);
    
            float magnitude = (binLow == binHigh) ? magnitudes[binLow] :
                    (1 - alpha) * magnitudes[binLow] + alpha * magnitudes[binHigh];
    
            int colorValue = (int) (Math.max(0, Math.min(255, magnitude * 255)));
            Color color = new Color(colorValue, colorValue / 2, 255 - colorValue);
            pixels[x] = color.getRGB();
        }
    
        currentLine.setRGB(0, 0, width, 1, pixels, 0, width);
    
        g2d.drawImage(spectrogramImage, 0, -1, width, height, null);
        g2d.drawImage(currentLine, 0, height - 1, null);
    
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
}
