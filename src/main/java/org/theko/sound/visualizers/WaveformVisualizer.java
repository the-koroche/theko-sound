package org.theko.sound.visualizers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
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

    // Audio-specific controls
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    
    // GUI fields
    protected float strokeWeight = 1.0f;
    protected Color waveformColor = Color.LIGHT_GRAY;
    protected float displayDuration = 0.1f;
    protected boolean splitStereo = false;
    protected boolean showIdleLine = true;

    protected static final float MIN_STROKE_WEIGHT = 0.1f;
    protected static final float MAX_STROKE_WEIGHT = 10.0f;
    protected static final float MIN_DISPLAY_DURATION = 0.001f;
    protected static final float MAX_DISPLAY_DURATION = 5.0f;

    protected final List<AudioControl> visualizerControls = List.of(gainControl);

    protected final List<float[][]> audioBuffers = new ArrayList<>(10);
    protected float[][] recentAudioWindow = new float[0][0];

    /**
     * A class that represents a waveform panel.
     * It can be used to display the waveform of an audio stream.
     * 
     * @since v2.1.1
     * @author Theko
     */
    protected class WaveformPanel extends JPanel {

        @Override
        protected void paintComponent (Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            setupG2D(g2d);

            if (audioBuffers.isEmpty()) {
                if (showIdleLine)
                    drawIdleLine(g2d);
                return;
            }

            int width = getWidth();
            int height = getHeight();

            float midY = height / 2f;
            float scaleY = midY * gainControl.getValue();

            float[][] samples = recentAudioWindow;
            int channels = samples.length;
            if (channels == 0) {
                if (showIdleLine)
                    drawIdleLine(g2d);
                return;
            }
            int sampleCount = samples[0].length;
            int toDrawCount = (int) (sampleCount * displayDuration);
            int sampleOffset = getSamplesOffset();

            // Calculate samples per pixel
            float samplesPerPixel = (float) toDrawCount / width;

            g2d.setColor(waveformColor);
            g2d.setStroke(new BasicStroke(strokeWeight));

            if (channels == 1) {
                drawMonoWaveform(g2d, samples[0], samplesPerPixel, sampleCount, sampleOffset, midY, width, scaleY);
            } else {
                if (!splitStereo) {
                    drawStereoWaveform(g2d, samples, samplesPerPixel, sampleCount, sampleOffset, midY, width, scaleY);
                } else {
                    scaleY *= 0.5;

                    float upperY = midY / 2;
                    float lowerY = midY + midY / 2;
                    drawMonoWaveform(g2d, samples[0], samplesPerPixel, sampleCount, sampleOffset, upperY, width, scaleY);
                    drawMonoWaveform(g2d, samples[1], samplesPerPixel, sampleCount, sampleOffset, lowerY, width, scaleY);
                }
            }
        }

        private void setupG2D (Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }

        private void drawMonoWaveform (
            Graphics2D g2d,
            float[] channelSamples,
            float samplesPerPixel,
            int totalSamples,
            int offsetSamples,
            float baselineY,
            float panelWidth,
            float amplitudeScale
            ) {
            if (samplesPerPixel >= 1f) {
                for (int x = 0; x < panelWidth; x++) {
                    int sampleIndex = Math.min((int)(x * samplesPerPixel) + offsetSamples, totalSamples - 1);
                    int end = (int)Math.min(channelSamples.length, sampleIndex + samplesPerPixel);

                    float min = 1f;
                    float max = -1f;

                    for (int i = sampleIndex; i < end; i++) {
                        float s = clamp(channelSamples[i]);
                        if (s < min) min = s;
                        if (s > max) max = s;
                    }

                    int y1 = (int)(baselineY - max * amplitudeScale);
                    int y2 = (int)(baselineY - min * amplitudeScale);

                    g2d.drawLine(x, y1, x, y2);
                }
            } else {
                // Interpolation between samples
                for (int x = 1; x < panelWidth; x++) {
                    float samplePos1 = (x - 1) * samplesPerPixel + offsetSamples;
                    float samplePos2 = x * samplesPerPixel + offsetSamples;

                    int index1 = (int) samplePos1;
                    int index2 = (int) samplePos2;

                    if (index2 >= totalSamples) {
                        break;
                    }

                    float frac1 = samplePos1 - index1;
                    float frac2 = samplePos2 - index2;

                    float s1 = MathUtilities.lerp(channelSamples[index1], channelSamples[Math.min(index1 + 1, totalSamples - 1)], frac1);
                    float s2 = MathUtilities.lerp(channelSamples[index2], channelSamples[Math.min(index2 + 1, totalSamples - 1)], frac2);

                    int y1 = (int)(baselineY - clamp(s1) * amplitudeScale);
                    int y2 = (int)(baselineY - clamp(s2) * amplitudeScale);

                    g2d.drawLine(x - 1, y1, x, y2);
                }
            }
        }

        private void drawStereoWaveform (
            Graphics2D g2d,
            float[][] samples, 
            float samplesPerPixel, 
            int totalSamples, 
            int offsetSamples, 
            float baselineY, 
            float panelWidth, 
            float amplitudeScale
            ) {
            if (samplesPerPixel >= 1f) {
                for (int x = 0; x < panelWidth; x++) {
                    int sampleIndex = Math.min((int)(x * samplesPerPixel) + offsetSamples, totalSamples - 1);
                    int end = (int)Math.min(samples[0].length, sampleIndex + samplesPerPixel);

                    float min = 1f;
                    float max = -1f;

                    for (int i = sampleIndex; i < end; i++) {
                        float sampleLeft = clamp(samples[0][i]);
                        float sampleRight = clamp(samples[1][i]);

                        float localMin = Math.min(sampleLeft, sampleRight);
                        float localMax = Math.max(sampleLeft, sampleRight);

                        if (localMin < min) min = localMin;
                        if (localMax > max) max = localMax;
                    }

                    float y1 = baselineY - max * amplitudeScale;
                    float y2 = baselineY - min * amplitudeScale;

                    g2d.drawLine(x, (int)y1, x, (int)y2);
                }
            } else {
                // Interpolation between samples
                for (int x = 1; x < panelWidth; x++) {
                    float samplePos1 = (x - 1) * samplesPerPixel + offsetSamples;
                    float samplePos2 = x * samplesPerPixel + offsetSamples;

                    int index1 = (int) samplePos1;
                    int index2 = (int) samplePos2;

                    if (index2 >= totalSamples) {
                        break;
                    }

                    float frac1 = samplePos1 - index1;
                    float frac2 = samplePos2 - index2;

                    // Interpolate
                    float sL1 = MathUtilities.lerp(samples[0][index1], samples[0][Math.min(index1 + 1, totalSamples - 1)], frac1);
                    float sR1 = MathUtilities.lerp(samples[1][index1], samples[1][Math.min(index1 + 1, totalSamples - 1)], frac1);
                    float sL2 = MathUtilities.lerp(samples[0][index2], samples[0][Math.min(index2 + 1, totalSamples - 1)], frac2);
                    float sR2 = MathUtilities.lerp(samples[1][index2], samples[1][Math.min(index2 + 1, totalSamples - 1)], frac2);

                    float y1max = clamp(Math.max(sL1, sR1));
                    float y2min = clamp(Math.min(sL2, sR2));

                    int yStart = (int)(baselineY - y1max * amplitudeScale);
                    int yEnd = (int)(baselineY - y2min * amplitudeScale);

                    g2d.drawLine(x - 1, yStart, x, yEnd);
                }
            }
        }

        private void drawIdleLine (Graphics2D g2d) {
            g2d.setColor(waveformColor);
            int midY = getHeight() / 2;
            if (!splitStereo) {
                g2d.drawLine(0, midY, getWidth(), midY);
            } else {
                g2d.drawLine(0, midY / 2, getWidth(), midY / 2);
                g2d.drawLine(0, midY + midY / 2, getWidth(), midY + midY / 2);
            }
        }

        private float clamp (float x) {
            return Math.max(-1f, Math.min(1f, x));
        }
    }

    /**
     * Constructs a new {@code WaveformVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the repaint.
     */
    public WaveformVisualizer (float frameRate) {
        super(Type.REALTIME, frameRate);

        addControls(visualizerControls);
    }

    @Override
    protected void initialize () {
        WaveformPanel panel = new WaveformPanel();
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setPanel(panel);
    }

    public FloatControl getGainControl () {
        return gainControl;
    }

    public void setSplitStereo (boolean separateStereo) {
        this.splitStereo = separateStereo;
    }

    public boolean isSplitStereo () {
        return splitStereo;
    }

    public void setShowIdleLine (boolean drawIdleLine) {
        this.showIdleLine = drawIdleLine;
    }

    public boolean isShowIdleLine () {
        return showIdleLine;
    }

    public void setDisplayDuration (float duration) {
        this.displayDuration = MathUtilities.clamp(duration, MIN_DISPLAY_DURATION, MAX_DISPLAY_DURATION);
    }

    public float getDisplayDuration () {
        return displayDuration;
    }

    public void setWaveformColor (Color color) {
        this.waveformColor = color;
    }

    public Color getWaveformColor () {
        return waveformColor;
    }

    public void setWeight (float weight) {
        this.strokeWeight = MathUtilities.clamp(weight, MIN_STROKE_WEIGHT, MAX_STROKE_WEIGHT);
    }

    public float getWeight () {
        return strokeWeight;
    }

    @Override
    protected void repaint () {
        getPanel().repaint();
    }

    @Override
    protected void onBufferUpdate () {
        audioBuffers.add(getSamplesBuffer());

        if (audioBuffers.size() == 1) return;

        int additionalSamples = getLength();

        // Calculate total samples
        int totalSamples = 0;
        for (int i = audioBuffers.size() - 1; i >= 0; i--) {
            totalSamples += audioBuffers.get(i)[0].length; // Assuming all buffers have the same number of channels
            if (totalSamples >= additionalSamples + displayDuration * getSampleRate()) break;
        }

        // Remove old buffers
        while (totalSamples > displayDuration * getSampleRate() + additionalSamples && audioBuffers.size() > 2) {
            float[][] removed = audioBuffers.remove(0);
            totalSamples -= removed[0].length;
        }

        int requiredSamples = (int)(displayDuration * getSampleRate() + additionalSamples);
        int channels = audioBuffers.get(0).length; // Assuming all buffers have the same number of channels

        List<float[][]> collected = new ArrayList<>();
        int totalFrames = 0;

        // Collect buffers until we have enough frames
        for (int i = audioBuffers.size() - 1; i >= 0 && totalFrames < requiredSamples; i--) {
            float[][] buf = audioBuffers.get(i);
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

        this.recentAudioWindow = result;
    }

    @Override
    protected int getSamplesOffset () {
        if (recentAudioWindow == null || recentAudioWindow.length == 0 || recentAudioWindow[0].length == 0) {
            return 0;
        }

        long now = System.nanoTime();
        long delta = now - getLastBufferUpdateTime();

        int offset = (int) (delta * getSampleRate() / 1000000000f);

        return offset;
    }
    

    @Override
    protected void onEnd () {
        audioBuffers.clear();
    }
}
