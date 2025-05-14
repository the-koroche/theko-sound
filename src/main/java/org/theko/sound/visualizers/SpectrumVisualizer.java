package org.theko.sound.visualizers;

import javax.swing.*;
import java.awt.*;

import org.theko.sound.AudioFormat;
import org.theko.sound.dsp.FFT;

public class SpectrumVisualizer extends AudioVisualizer {
    protected int fftSize = 1024;
    private float[] real;
    private float[] imag;
    private float[] magnitudes;

    protected SpectrumPanel spectrumPanel;

    private int numBins = -1; // -1 means auto
    private boolean useLogScale = true; // Логарифмическая шкала для отрисовки

    public SpectrumVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        this.real = new float[fftSize];
        this.imag = new float[fftSize];
        this.magnitudes = new float[fftSize / 2];
    }

    // Метод для установки количества бинов
    public void setNumBins(int numBins) {
        this.numBins = numBins;
    }

    // Метод для переключения между линейной и логарифмической шкалой
    public void setLogScale(boolean useLogScale) {
        this.useLogScale = useLogScale;
    }

    @Override
    public void initialize() {
        spectrumPanel = new SpectrumPanel();
        SwingUtilities.invokeLater(() -> spectrumPanel.setVisible(true));
    }

    @Override
    public float[][] process(float[][] samples) {
        if (samples.length > 0 && samples[0].length >= fftSize) {
            System.arraycopy(samples[0], 0, real, 0, fftSize);
            for (int i = 0; i < fftSize; i++) imag[i] = 0;

            FFT.fft(real, imag);

            for (int i = 0; i < fftSize / 2; i++) {
                float amplitude = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
                // Логарифмическое преобразование амплитуды для уменьшения шума
                if (useLogScale) {
                    magnitudes[i] = (float) (Math.log(amplitude + 1e-6) * 0.25); // Логарифм
                } else {
                    magnitudes[i] = amplitude; // Линейный
                }
            }
        }
        return samples;
    }

    private class SpectrumPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
    
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            int width = getWidth();
            int height = getHeight();
    
            // Расчет количества бинов для визуализации
            int bins = (numBins == -1) ? magnitudes.length : numBins;
            float binWidth = (float)(width) / bins;
    
            // Рисуем спектр по бинам
            for (int i = 0; i < bins; i++) {
                int binIndex = i * magnitudes.length / bins; // Индекс данных для текущего бина
                int binHeight = (int) (magnitudes[binIndex] * height); // Преобразуем амплитуду в высоту
    
                // Рисуем каждый бин с учетом ширины
                g2d.setColor(Color.GREEN);
                g2d.fillRect((int)(i * binWidth), height - binHeight, (int)Math.ceil(binWidth), binHeight);
            }
        }
    }

    @Override
    public void repaint() {
        SwingUtilities.invokeLater(() -> spectrumPanel.repaint());
    }

    @Override
    public JPanel getPanel() {
        return spectrumPanel;
    }

    @Override
    public void onEnd() {
        // Cleanup
    }
}
