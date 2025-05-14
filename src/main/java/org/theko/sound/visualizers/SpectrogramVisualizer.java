package org.theko.sound.visualizers;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import org.theko.sound.AudioFormat;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;

public class SpectrogramVisualizer extends AudioVisualizer {
    protected int fftSize[];
    protected int stepSize[]; // For each fftSize
    protected final int fftSections;
    protected final int nyquist;
    protected WindowType windowType;

    protected float[] fftBuffer; // Long-size buffer beacuse of small samples portions in process().
    protected float[][] magnitudes; // Magnitudes for each 'fftSize'.
    protected float[][] real, imag;

    protected JPanel spectrogramPanel;

    public SpectrogramVisualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        fftSections = 6;
        fftSize = new int[fftSections];
        stepSize = new int[fftSections];
        int sampleRate = audioFormat.getSampleRate();
        nyquist = sampleRate/2;
        windowType = WindowType.HANN;
        initializeFFTSize();

        int fftLargestSize = fftSize[0]; // Assuming the 0 fftSize is the biggest one.
        fftBuffer = new float[fftLargestSize];
        magnitudes = new float[fftSections][nyquist];
        real = new float[fftSections][];
        imag = new float[fftSections][];
    }

    @Override
    public void initialize() {
        spectrogramPanel = new SpectrogramPanel();
        // Make the panel transparent
        spectrogramPanel.setOpaque(false);
        spectrogramPanel.setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    public float[][] process(float[][] samples) {
        // Do some useful stuff
        appendBuffer(fftBuffer, samples[0]);
        processFFT(fftBuffer);
        return samples; // Don't forget to return samples. Usually unmodified
    }

    @Override
    protected void repaint() {
        spectrogramPanel.repaint(); // Just repaint the panel.
    }

    @Override
    public void onEnd() {
        // Nothing to close, or end here
    }

    @Override
    public JPanel getPanel() {
        return spectrogramPanel;
    }

    // Something protected
    protected void processFFT(float[] audio) {
        for (int i = 0; i < fftSections; i++) {
            // Cut audio data
            int currentFftSize = fftSize[i];
            real[i] = new float[currentFftSize];
            imag[i] = new float[currentFftSize];
            System.arraycopy(audio, 0, real[i], 0, currentFftSize);

            // Perform window
            WindowFunction.apply(real[i], windowType);
            // Perform FFT
            FFT.fft(real[i], imag[i]);

            for (int j = 0; j < currentFftSize / 2; j++) {
                float amplitude = (float) Math.sqrt(real[i][j] * real[i][j] + imag[i][j] * imag[i][j]);
                magnitudes[i][j] = (float) (Math.log(amplitude + 1e-6) * 0.25);
            }
        }
    }

    protected float[] getCombinedMagnitudes() {
        float[] combined = new float[nyquist]; // Итоговый массив
    
        int sectionWidth = nyquist / fftSections; // Сколько частот покрывает одна секция
    
        for (int section = 0; section < fftSections; section++) {
            for (int i = 0; i < sectionWidth; i++) {
                int index = section * sectionWidth + i;
                if (index < nyquist && i < magnitudes[section].length) {
                    combined[index] = magnitudes[section][i];
                }
            }
        }
    
        return combined;
    }

    protected void appendBuffer(float[] buffer, float[] toAppend) {
        System.arraycopy(buffer, toAppend.length, buffer, 0, buffer.length - toAppend.length);
        System.arraycopy(toAppend, 0, buffer, buffer.length - toAppend.length, toAppend.length);
    }

    protected void initializeFFTSize() {
        fftSize[0] = 8192;
        fftSize[1] = 4096;
        fftSize[2] = 2048;
        fftSize[3] = 1024;
        fftSize[4] = 512;
        fftSize[5] = 256;
    }

    protected class SpectrogramPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
    
            float[] combined = getCombinedMagnitudes();
    
            for (int i = 0; i < combined.length; i++) {
                int y = (int)(((float)i) / combined.length * getHeight());
    
                int intensity = (int)(combined[i] * 255f);
                intensity = Math.min(Math.max(intensity, 0), 255);
    
                g2d.setColor(new Color(intensity, intensity, intensity));
                g2d.fillRect(0, y, getWidth(), 1); // Использовать getWidth() вместо 1 для растягивания по X
            }
        }
    }
    
}
