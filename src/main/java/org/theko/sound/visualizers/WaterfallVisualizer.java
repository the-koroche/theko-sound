package org.theko.sound.visualizers;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.theko.sound.AudioFormat;
import org.theko.sound.fft.FFT;
import org.theko.sound.fft.FFTDecimator;
import org.theko.sound.fft.WindowFunction;
import org.theko.sound.fft.WindowType;

public class WaterfallVisualizer extends AudioVisualizer {
    protected int fftSize = 1024;
    protected int decimationFactor = 4;
    private float[] real;
    private float[] imag;
    private float[] magnitudes;

    protected WaterfallPanel waterfallPanel;
    protected BufferedImage waterfallImage;
    private int lastWidth = 600;
    private int lastHeight = 400;

    protected WaterfallColor waterfallColor;
    protected Orientation orientation = Orientation.RIGHT;
    protected boolean flipBands = true;
    protected boolean useLogScale = true;

    public enum Orientation {
        TOP, BOTTOM, LEFT, RIGHT
    }

    public interface WaterfallColor {
        Color getColor(float magnitude, float frequency);
    }

    public static final WaterfallColor INFERNO_COLOR = new WaterfallColor() {
        private Color[] colors = {
            new Color(250, 253, 161), // Светло-желтый
            new Color(251, 182, 26),  // Оранжевый
            new Color(237, 105, 37),  // Красный
            new Color(188, 55, 84),   // Темно-розовый
            new Color(120, 28, 109),  // Фиолетовый
            new Color(50, 10, 94),    // Темно-фиолетовый
            new Color(0, 0, 0)        // Черный
        };

        public Color getColor(float magnitude, float frequency) {
            float value = Math.min(1.0f, Math.max(0.0f, magnitude));
            Color color = AudioVisualizationUtilities.getGradient(1.0f-value, colors);

            //int a = magnitude > 0.01 ? 255 : 0;
            int a = (int)(value * 255);
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
        }
    };

    public static final WaterfallColor EMERALD_COLOR = new WaterfallColor() {
        private Color[] colors = {
            new Color(0, 0, 64), // Светло-желтый
            new Color(0, 0, 255),  // Оранжевый
            new Color(0, 128, 255),  // Красный
            new Color(0, 255, 255),   // Темно-розовый
            new Color(128, 255, 0),  // Фиолетовый
            new Color(255, 128, 0),    // Темно-фиолетовый
            new Color(255, 0, 0)        // Черный
        };

        public Color getColor(float magnitude, float frequency) {
            float value = Math.min(1.0f, Math.max(0.0f, magnitude));
            Color color = AudioVisualizationUtilities.getGradient(value, colors);

            //int a = magnitude > 0.01 ? 255 : 0;
            int a = (int)(value * 255);
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
        }
    };

    public static final WaterfallColor GRAY_COLOR = new WaterfallColor() {
        public Color getColor(float magnitude, float frequency) {
            float value = Math.min(1.0f, Math.max(0.0f, magnitude));

            //int a = magnitude > 0.01 ? 1.0f : 0.0f;
            return new Color(value, value, value, value);
        }
    };

    public WaterfallVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.real = new float[fftSize];
        this.imag = new float[fftSize];
        this.magnitudes = new float[fftSize / 2];
        this.waterfallColor = INFERNO_COLOR;
    }

    @Override
    public void initialize() {
        waterfallPanel = new WaterfallPanel();
        waterfallPanel.setOpaque(false);
        waterfallPanel.setBackground(new Color(0, 0, 0, 0));
        SwingUtilities.invokeLater(() -> waterfallPanel.setVisible(true));
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        this.real = new float[fftSize];
        this.imag = new float[fftSize];
        this.magnitudes = new float[fftSize / 2];
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setDecimationFactor(int factor) {
        if (factor > 1) {
            this.decimationFactor = factor;
        }
    }
    
    public int getDecimationFactor() {
        return this.decimationFactor;
    }

    public void setFlipFrequencies(boolean b) {
        this.flipBands = b;
    }

    public void setWaterfallColor(WaterfallColor color) {
        this.waterfallColor = color;
    }

    public void setFFTSize(int fftSize) {
        if (fftSize > 0) {
            this.fftSize = fftSize;
        }
    }

    private void updateWaterfallSize(int width, int height) {
        if (waterfallImage != null) {
            BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.drawImage(waterfallImage, 0, 0, width, height, null);
            g2d.dispose();
            waterfallImage = scaledImage;
        } else {
            waterfallImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        waterfallPanel.repaint();
    }

    private int nextPowerOfTwo(int n) {
        if (n < 1) return 1;
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }

    @Override
    public float[][] process(float[][] samples) {
        if (samples.length == 0) return samples;

        float[] target = samples[0];
        if (target.length < 1) return samples;

        // Применяем децимацию, если коэффициент больше 1
        if (decimationFactor > 1) {
            target = FFTDecimator.decimate(target, decimationFactor);
        }

        // Обновляем fftSize, если он отличается от нового размера
        real = new float[fftSize];
        imag = new float[fftSize];
        magnitudes = new float[fftSize / 2];

        //System.out.println(fftSize);

        // Копируем данные в массив и обнуляем мнимую часть
        Arrays.fill(real, 0);
        Arrays.fill(imag, 0);
        System.arraycopy(target, 0, real, 0, Math.min(target.length, fftSize));

        // Применяем оконную функцию
        WindowFunction.apply(real, WindowType.HANN);

        // Выполняем FFT
        FFT.fft(real, imag);

        // Вычисляем амплитуды
        for (int i = 0; i < fftSize / 2; i++) {
            float amplitude = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
            magnitudes[i] = (float) (Math.log(amplitude + 1e-6) * 0.25);
        }

        updateWaterfallImage();
        return samples;
    }

    private void updateWaterfallImage() {
        if (waterfallImage == null || waterfallImage.getWidth() <= 0 || waterfallImage.getHeight() <= 0) {
            return;
        }
    
        Graphics2D g2d = waterfallImage.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
    
        boolean isHorz = orientation == Orientation.LEFT || orientation == Orientation.RIGHT;
        boolean isVert = orientation == Orientation.TOP || orientation == Orientation.BOTTOM;
    
        int width = waterfallImage.getWidth();
        int height = waterfallImage.getHeight();
    
        int targetAxis = (isVert ? width : height);
        int[] pixels = new int[targetAxis];
        int maxBinIndex = fftSize / 2 - 1;
    
        for (int x = 0; x < targetAxis; x++) {
            double binPos;
            if (useLogScale) {
                double logPos = (double) x / (targetAxis - 1) * Math.log(maxBinIndex + 1);
                binPos = Math.exp(logPos) - 1;
            } else {
                binPos = (double) x / (targetAxis - 1) * maxBinIndex;
            }
    
            int binLow = (int) Math.floor(binPos);
            int binHigh = (int) Math.ceil(binPos);
            float alpha = (float) (binPos - binLow);
    
            binLow = Math.min(Math.max(binLow, 0), maxBinIndex);
            binHigh = Math.min(binHigh, maxBinIndex);
    
            float magnitude = (binLow == binHigh) ? magnitudes[binLow] :
                    (1 - alpha) * magnitudes[binLow] + alpha * magnitudes[binHigh];
    
            Color color = waterfallColor.getColor(magnitude, ((float) x / targetAxis) * 22000);
            if (flipBands) {
                pixels[targetAxis - x - 1] = color.getRGB();
            } else {
                pixels[x] = color.getRGB();
            }
        }
    
        BufferedImage currentLine = new BufferedImage(isHorz ? 1 : width, isVert ? 1 : height, BufferedImage.TYPE_INT_ARGB);
        
        if (isVert) {
            currentLine.setRGB(0, 0, width, 1, pixels, 0, width);
        } else {
            currentLine.setRGB(0, 0, 1, height, pixels, 0, 1);
        }
    
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

    private class WaterfallPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (waterfallImage != null) {
                g.drawImage(waterfallImage, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

    @Override
    public void repaint() {
        int newWidth = Math.max(1, waterfallPanel.getWidth());
        int newHeight = Math.max(1, waterfallPanel.getHeight());
        if (newWidth != lastWidth || newHeight != lastHeight) {
            lastWidth = newWidth;
            lastHeight = newHeight;
            updateWaterfallSize(newWidth, newHeight);
        }
        SwingUtilities.invokeLater(() -> waterfallPanel.repaint());
    }

    @Override
    public JPanel getPanel() {
        return waterfallPanel;
    }

    @Override
    public void onEnd() {
        // Cleanup
    }
}
