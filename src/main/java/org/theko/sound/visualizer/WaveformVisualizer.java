package org.theko.sound.visualizer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.BooleanControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.control.Vector3Control;
import org.theko.sound.utility.MathUtilities;

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
    protected final FloatControl weigth = new FloatControl("Weight", 0.1f, 50.0f, 1.5f);
    protected final Vector3Control color = new Vector3Control("Color", 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
    protected final FloatControl duration = new FloatControl("Duration", 0.001f, 100.0f, 0.01f);
    protected final BooleanControl separateStereo = new BooleanControl("Separate Stereo", false);
    protected final BooleanControl drawIdleLine = new BooleanControl("Draw Idle Line", true);

    protected final List<AudioControl> waveformControls = List.of(
        gain,
        weigth,
        color,
        duration,
        separateStereo,
        drawIdleLine
    );

    protected final List<float[][]> buffers = new ArrayList<>(10);

    /**
     * A class that represents a waveform panel.
     * It can be used to display the waveform of an audio stream,
     * with a configurable gain, weight, color, and duration.
     */
    protected class WaveformPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (buffers.isEmpty()) {
                if (drawIdleLine.isEnabled())
                    drawIdleLine(g2d);
                return;
            }

            int width = getWidth();
            int height = getHeight();
            float midY = height / 2f;
            float scaleY = midY * gain.getValue();

            float[][] samples = getRecentSamples();
            int channels = samples.length;
            if (channels == 0) {
                if (drawIdleLine.isEnabled())
                    drawIdleLine(g2d);
                return;
            }
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
                drawOscilloscopeLine(g2d, samples[0], samplesPerPixel, sampleCount, midY, width, scaleY);
            } else {
                if (separateStereo.isDisabled()) {
                    if (samplesPerPixel >= 1f) {
                        for (int x = 0; x < width; x++) {
                            int sampleIndex = Math.min((int)(x * samplesPerPixel), sampleCount - 1);
                            int end = (int)Math.min(samples[0].length, sampleIndex + samplesPerPixel);

                            float min = 1f;
                            float max = -1f;

                            for (int i = sampleIndex; i < end; i++) {
                                float sL = clamp(samples[0][i]);
                                float sR = clamp(samples[1][i]);

                                float localMin = Math.min(sL, sR);
                                float localMax = Math.max(sL, sR);

                                if (localMin < min) min = localMin;
                                if (localMax > max) max = localMax;
                            }

                            float y1 = midY - max * scaleY;
                            float y2 = midY - min * scaleY;

                            g2d.drawLine(x, (int)y1, x, (int)y2);
                        }
                    } else {
                        // Interpolation between samples
                        for (int x = 1; x < width; x++) {
                            float pos1 = (x - 1) * samplesPerPixel;
                            float pos2 = x * samplesPerPixel;

                            int i1 = (int) pos1;
                            int i2 = (int) pos2;

                            if (i2 >= sampleCount) break;

                            float frac1 = pos1 - i1;
                            float frac2 = pos2 - i2;

                            // Interpolate
                            float sL1 = MathUtilities.lerp(samples[0][i1], samples[0][Math.min(i1 + 1, sampleCount - 1)], frac1);
                            float sR1 = MathUtilities.lerp(samples[1][i1], samples[1][Math.min(i1 + 1, sampleCount - 1)], frac1);
                            float sL2 = MathUtilities.lerp(samples[0][i2], samples[0][Math.min(i2 + 1, sampleCount - 1)], frac2);
                            float sR2 = MathUtilities.lerp(samples[1][i2], samples[1][Math.min(i2 + 1, sampleCount - 1)], frac2);

                            float y1max = clamp(Math.max(sL1, sR1));
                            float y2min = clamp(Math.min(sL2, sR2));

                            int yStart = (int)(midY - y1max * scaleY);
                            int yEnd = (int)(midY - y2min * scaleY);

                            g2d.drawLine(x - 1, yStart, x, yEnd);
                        }
                    }
                } else {
                    scaleY *= 0.5;

                    float upperY = midY / 2;
                    float lowerY = midY + midY / 2;
                    drawOscilloscopeLine(g2d, samples[0], samplesPerPixel, sampleCount, upperY, width, scaleY);
                    drawOscilloscopeLine(g2d, samples[1], samplesPerPixel, sampleCount, lowerY, width, scaleY);
                }
            }
        }

        private void drawOscilloscopeLine(Graphics2D g2d, float[] samples, float samplesPerPixel, int sampleCount, float baseHeight, float width, float scaleY) {
            if (samplesPerPixel >= 1f) {
                for (int x = 0; x < width; x++) {
                    int sampleIndex = Math.min((int)(x * samplesPerPixel), sampleCount - 1);
                    int end = (int)Math.min(samples.length, sampleIndex + samplesPerPixel);

                    float min = 1f;
                    float max = -1f;

                    for (int i = sampleIndex; i < end; i++) {
                        float s = clamp(samples[i]);
                        if (s < min) min = s;
                        if (s > max) max = s;
                    }

                    int y1 = (int)(baseHeight - max * scaleY);
                    int y2 = (int)(baseHeight - min * scaleY);

                    g2d.drawLine(x, y1, x, y2);
                }
            } else {
                // Interpolation between samples
                for (int x = 1; x < width; x++) {
                    float samplePos1 = (x - 1) * samplesPerPixel;
                    float samplePos2 = x * samplesPerPixel;

                    int index1 = (int) samplePos1;
                    int index2 = (int) samplePos2;

                    if (index2 >= sampleCount) break;

                    float frac1 = samplePos1 - index1;
                    float frac2 = samplePos2 - index2;

                    float s1 = MathUtilities.lerp(samples[index1], samples[Math.min(index1 + 1, sampleCount - 1)], frac1);
                    float s2 = MathUtilities.lerp(samples[index2], samples[Math.min(index2 + 1, sampleCount - 1)], frac2);

                    int y1 = (int)(baseHeight - clamp(s1) * scaleY);
                    int y2 = (int)(baseHeight - clamp(s2) * scaleY);

                    g2d.drawLine(x - 1, y1, x, y2);
                }
            }
        }

        private void drawIdleLine(Graphics2D g2d) {
            g2d.setColor(new Color(color.getX(), color.getY(), color.getZ()));
            int midY = getHeight() / 2;
            if (separateStereo.isDisabled()) {
                g2d.drawLine(0, midY, getWidth(), midY);
            } else {
                g2d.drawLine(0, midY / 2, getWidth(), midY / 2);
                g2d.drawLine(0, midY + midY / 2, getWidth(), midY + midY / 2);
            }
        }

        private float clamp(float val) {
            if (val > 1f) return 1f;
            if (val < -1f) return -1f;
            return val;
        }
    }

    private boolean isIteratingBuffers = false;

    private float[][] getRecentSamples() {
        int requiredSamples = (int)(duration.getValue() * sampleRate);
        int channels = buffers.get(0).length; // Assuming all buffers have the same number of channels

        List<float[][]> collected = new ArrayList<>();
        int totalFrames = 0;

        // Collect buffers until we have enough frames
        isIteratingBuffers = true;
        for (int i = buffers.size() - 1; i >= 0 && totalFrames < requiredSamples; i--) {
            float[][] buf = buffers.get(i);
            collected.add(0, buf); // Insert at the beginning
            totalFrames += buf[0].length;
        }
        isIteratingBuffers = false;

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

        allControls.addAll(waveformControls);
    }

    @Override
    protected void initialize() {
        panel = new WaveformPanel();
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
    }

    public FloatControl getGain () {
        return gain;
    }

    public BooleanControl getSeparateStereo () {
        return separateStereo;
    }

    public FloatControl getDuration () {
        return duration;
    }

    public Vector3Control getColor () {
        return color;
    }

    public FloatControl getStrokeWidth () {
        return weigth;
    }

    @Override
    protected void repaint() {
        panel.repaint();
    }

    @Override
    protected void onBufferUpdate() {
        while (isIteratingBuffers) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        buffers.add(samplesBuffer);

        if (buffers.size() == 1) return;

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
