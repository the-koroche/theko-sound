package org.theko.sound.visualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.theko.sound.control.FloatControl;
import org.theko.sound.control.Vector3Control;

/**
 * A class that represents a waveform visualizer.
 * It can be used to display the waveform of an audio stream,
 * with a configurable gain, weight, color, and duration.
 * 
 * @see AudioVisualizer
 * 
 * @since v2.1.1
 * @author Theko
 */
public class WaveformVisualizer extends AudioVisualizer {

    protected final FloatControl gain = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    protected final FloatControl weigth = new FloatControl("Weight", 0.1f, 50.0f, 3.0f);
    protected final Vector3Control color = new Vector3Control("Color", 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    protected final FloatControl duration = new FloatControl("Duration", 0.005f, 10.0f, 0.04f);

    protected final List<float[][]> buffers = new ArrayList<>(20);

    /**
     * A class that represents a waveform panel.
     * It can be used to display the waveform of an audio stream,
     * with a configurable gain, weight, color, and duration.
     */
    protected class WaveformPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (samplesBuffer == null || samplesBuffer.length == 0 || samplesBuffer[0].length == 0) {
                drawIdleLine(g);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            float midY = height / 2f;
            float scaleY = midY * gain.getValue();

            float[][] samples = getRecentSamples();
            int channels = samples.length;
            int sampleCount = samples[0].length;

            if (sampleCount < width) {
                // Scale y axis if we have less samples than pixels
                scaleY *= (float)sampleCount / width;
            }

            // Calculate samples per pixel
            float samplesPerPixel = (float) sampleCount / width;

            g2d.setColor(new Color(color.getX(), color.getY(), color.getZ()));
            g2d.setStroke(new BasicStroke(weigth.getValue()));

            if (channels == 1) {
                // Mono - draw a line
                float prevY = midY;
                for (int x = 0; x < width; x++) {
                    int sampleIndex = Math.min((int)(x * samplesPerPixel), sampleCount - 1);
                    float sampleVal = clamp(samples[0][sampleIndex]);
                    float y = midY - sampleVal * scaleY;

                    if (x > 0) {
                        g2d.drawLine(x - 1, (int)prevY, x, (int)y);
                    }
                    prevY = y;
                }
            } else {
                // Stereo - draw the difference
                for (int x = 0; x < width; x++) {
                    int sampleIndex = Math.min((int)(x * samplesPerPixel), sampleCount - 1);
                    float y1 = midY - samples[0][sampleIndex] * scaleY;
                    float y2 = midY - samples[1][sampleIndex] * scaleY;

                    if (x > 0) {
                        g2d.drawLine(x - 1, (int)y1, x, (int)y2);
                    }
                }
            }
        }

        private void drawIdleLine(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setColor(Color.DARK_GRAY);
            int midY = getHeight() / 2;
            g2d.drawLine(0, midY, getWidth(), midY);
        }

        private float clamp(float val) {
            if (val > 1f) return 1f;
            if (val < -1f) return -1f;
            return val;
        }
    }

    private float[][] getRecentSamples() {
        int requiredSamples = (int)(duration.getValue() * sampleRate);
        int channels = buffers.get(0).length; // Assuming all buffers have the same number of channels

        List<float[][]> collected = new ArrayList<>();
        int totalFrames = 0;

        // Collect buffers until we have enough frames
        for (int i = buffers.size() - 1; i >= 0 && totalFrames < requiredSamples; i--) {
            float[][] buf = buffers.get(i);
            collected.add(0, buf); // Insert at the beginning
            totalFrames += buf[0].length;
        }

        // Create a result array 
        int resultFrames = Math.min(totalFrames, requiredSamples);
        float[][] result = new float[channels][resultFrames];

        int copied = 0;
        for (float[][] buf : collected) {
            int bufLength = buf[0].length;
            int toCopy = Math.min(bufLength, resultFrames - copied);

            for (int ch = 0; ch < channels; ch++) {
                System.arraycopy(buf[ch], bufLength - toCopy, result[ch], copied, toCopy);
            }

            copied += toCopy;
            if (copied >= resultFrames) break;
        }

        return result;
    }

    /**
     * Constructs a new {@code WaveformVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the repaint.
     */
    public WaveformVisualizer (float frameRate) {
        super(Type.REALTIME, frameRate);
        color.setX(1.0f);
        color.setY(1.0f);
        color.setZ(1.0f);
    }

    @Override
    protected void initialize() {
        panel = new WaveformPanel();
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    protected void repaint() {
        panel.repaint();
    }

    @Override
    protected void onBufferUpdate() {
        buffers.add(samplesBuffer);

        // Подсчитать, сколько всего сэмплов накоплено
        int totalSamples = 0;
        for (int i = buffers.size() - 1; i >= 0; i--) {
            totalSamples += buffers.get(i)[0].length; // только по одному каналу считаем
            if (totalSamples >= duration.getValue() * sampleRate) break;
        }

        // Удалить старые буферы, пока слишком много сэмплов
        while (totalSamples > duration.getValue() * sampleRate && !buffers.isEmpty()) {
            float[][] removed = buffers.remove(0);
            totalSamples -= removed[0].length;
        }
    }

    @Override
    protected void onEnd() {
        buffers.clear();
    }
}
