/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.theko.sound.visualizers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @since 2.1.1
 * @author Theko
 */
public class WaveformVisualizer extends AudioVisualizer {

    private static final Logger logger = LoggerFactory.getLogger(WaveformVisualizer.class);

    // Audio-specific controls
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    
    // GUI fields
    protected float strokeWeight = 1.0f;
    protected Color waveformColor = Color.LIGHT_GRAY;
    protected float displayDuration = 0.1f;
    protected boolean splitStereo = false;
    protected boolean showIdleLine = true;
    protected boolean interpolateStereo = false;

    protected static final float MIN_STROKE_WEIGHT = 0.1f;
    protected static final float MAX_STROKE_WEIGHT = 10.0f;
    protected static final float MIN_DISPLAY_DURATION = 0.001f;
    protected static final float MAX_DISPLAY_DURATION = 5.0f;

    protected final List<AudioControl> visualizerControls = List.of(gainControl);

    protected final List<float[][]> audioBuffers = new ArrayList<>(4);

    private List<float[][]> collectedBuffers = new ArrayList<>();
    private float[][] recentAudioWindow = null;
    private int lastRequiredSamples = -1;

    /**
     * A class that represents a waveform panel.
     * It can be used to display the waveform of an audio stream.
     */
    protected class WaveformPanel extends JPanel {

        private BasicStroke stroke = null;
        private float[] interpolatedSamples = null;

        @Override
        public void invalidate() {
            super.invalidate();
            logger.trace("Invalidated.");
            stroke = null;
            interpolatedSamples = null;
        }

        @Override
        protected void paintComponent(Graphics g) {
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
            if (samples == null || samples.length == 0) {
                if (showIdleLine)
                    drawIdleLine(g2d);
                return;
            }
            int channels = samples.length;
            if (channels == 0) {
                if (showIdleLine)
                    drawIdleLine(g2d);
                return;
            }
            int sampleCount = samples[0].length;
            int toDrawCount = (int) (sampleCount * displayDuration);
            int sampleOffset = getSamplesOffset();

            if (interpolatedSamples == null || interpolatedSamples.length != sampleCount) {
                logger.trace("New interpolatedSamples: {}. Old size: {}", sampleCount, interpolatedSamples == null ? -1 : interpolatedSamples.length);
                interpolatedSamples = new float[sampleCount];
            }

            // Calculate samples per pixel
            float samplesPerPixel = (float) toDrawCount / width;

            if (stroke == null || stroke.getLineWidth() != strokeWeight) {
                logger.trace("New stroke: {}", strokeWeight);
                stroke = new BasicStroke(strokeWeight);
            }

            g2d.setColor(waveformColor);
            g2d.setStroke(stroke);

            if (channels == 1) {
                drawMonoWaveform(g2d, samples[0], samplesPerPixel, sampleCount, sampleOffset, midY, width, scaleY);
            } else {
                if (!splitStereo && !interpolateStereo) {
                    drawStereoWaveform(g2d, samples, samplesPerPixel, sampleCount, sampleOffset, midY, width, scaleY);
                } else if (!splitStereo && interpolateStereo) {
                    for (int i = 0; i < sampleCount; i++) {
                        interpolatedSamples[i] = (samples[0][i] + samples[1][i]) / 2f;
                    }
                    drawMonoWaveform(g2d, interpolatedSamples, samplesPerPixel, sampleCount, sampleOffset, midY, width, scaleY);
                } else if (splitStereo) {
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

        private void drawMonoWaveform(
            Graphics2D g2d,
            float[] channelSamples,
            float samplesPerPixel,
            int totalSamples,
            int offsetSamples,
            float baselineY,
            int panelWidth,
            float amplitudeScale
        ) {
            float prevY = baselineY - clamp(channelSamples[offsetSamples]) * amplitudeScale;

            for (int x = 1; x < panelWidth; x++) {
                float samplePos = x * samplesPerPixel + offsetSamples;
                if (samplePos >= totalSamples - 1) break;

                int index = (int) samplePos;
                float frac = samplePos - index;

                float sampleValue = MathUtilities.lerp(channelSamples[index], channelSamples[index + 1], frac);
                float y = baselineY - clamp(sampleValue) * amplitudeScale;

                g2d.drawLine(x - 1, (int) prevY, x, (int) y);
                prevY = y;
            }
        }

        private void drawStereoWaveform(
            Graphics2D g2d,
            float[][] samples,
            float samplesPerPixel,
            int totalSamples,
            int offsetSamples,
            float baselineY,
            int panelWidth,
            float amplitudeScale
        ) {
            // Get the first vertical range (min, max) at x=0
            float samplePos0 = offsetSamples;
            int index0 = (int) samplePos0;
            float frac0 = samplePos0 - index0;

            // Check for out of bounds
            if (index0 >= totalSamples - 1) return;

            float left0 = MathUtilities.lerp(samples[0][index0], samples[0][Math.min(index0 + 1, totalSamples - 1)], frac0);
            float right0 = MathUtilities.lerp(samples[1][index0], samples[1][Math.min(index0 + 1, totalSamples - 1)], frac0);

            float minPrev = clamp(Math.min(left0, right0));
            float maxPrev = clamp(Math.max(left0, right0));

            for (int x = 1; x < panelWidth; x++) {
                float samplePos = x * samplesPerPixel + offsetSamples;
                if (samplePos >= totalSamples - 1) break;

                int index = (int) samplePos;
                float frac = samplePos - index;

                float leftSample = MathUtilities.lerp(samples[0][index], samples[0][Math.min(index + 1, totalSamples - 1)], frac);
                float rightSample = MathUtilities.lerp(samples[1][index], samples[1][Math.min(index + 1, totalSamples - 1)], frac);

                float minCurr = clamp(Math.min(leftSample, rightSample));
                float maxCurr = clamp(Math.max(leftSample, rightSample));

                int yMinPrev = (int) (baselineY - maxPrev * amplitudeScale);
                int yMaxPrev = (int) (baselineY - minPrev * amplitudeScale);
                int yMinCurr = (int) (baselineY - maxCurr * amplitudeScale);
                int yMaxCurr = (int) (baselineY - minCurr * amplitudeScale);

                // Draw 4 lines to create filled rectangle
                // Left vertical line (connects x-1 point)
                g2d.drawLine(x - 1, yMinPrev, x - 1, yMaxPrev);
                // Right line (connects x point)
                g2d.drawLine(x, yMinCurr, x, yMaxCurr);
                // Top line (connects top points)
                g2d.drawLine(x - 1, yMinPrev, x, yMinCurr);
                // Bottom line (connects bottom points)
                g2d.drawLine(x - 1, yMaxPrev, x, yMaxCurr);

                minPrev = minCurr;
                maxPrev = maxCurr;
            }
        }

        private void drawIdleLine(Graphics2D g2d) {
            g2d.setColor(waveformColor);
            int midY = getHeight() / 2;
            if (!splitStereo) {
                g2d.drawLine(0, midY, getWidth(), midY);
            } else {
                g2d.drawLine(0, midY / 2, getWidth(), midY / 2);
                g2d.drawLine(0, midY + midY / 2, getWidth(), midY + midY / 2);
            }
        }

        private float clamp(float x) {
            return Math.max(-1f, Math.min(1f, x));
        }
    }

    /**
     * Constructs a new {@code WaveformVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the repaint.
     */
    public WaveformVisualizer(float frameRate) {
        super(Type.REALTIME, frameRate);

        addControls(visualizerControls);
    }

    @Override
    protected void initialize() {
        WaveformPanel panel = new WaveformPanel();
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setPanel(panel);
    }

    public FloatControl getGainControl() {
        return gainControl;
    }

    public void setSplitStereo(boolean separateStereo) {
        this.splitStereo = separateStereo;
    }

    public boolean isSplitStereo() {
        return splitStereo;
    }

    public void setShowIdleLine(boolean drawIdleLine) {
        this.showIdleLine = drawIdleLine;
    }

    public boolean isShowIdleLine() {
        return showIdleLine;
    }

    public void setDisplayDuration(float duration) {
        this.displayDuration = MathUtilities.clamp(duration, MIN_DISPLAY_DURATION, MAX_DISPLAY_DURATION);
    }

    public float getDisplayDuration() {
        return displayDuration;
    }

    public void setWaveformColor(Color color) {
        this.waveformColor = color;
    }

    public Color getWaveformColor() {
        return waveformColor;
    }

    public void setWeight(float weight) {
        this.strokeWeight = MathUtilities.clamp(weight, MIN_STROKE_WEIGHT, MAX_STROKE_WEIGHT);
    }

    public float getWeight() {
        return strokeWeight;
    }

    public void setInterpolateStereo(boolean interpolate) {
        this.interpolateStereo = interpolate;
    }

    public boolean isInterpolateStereo() {
        return interpolateStereo;
    }

    @Override
    protected void repaint() {
        getPanel().repaint();
    }

    @Override
    protected void onBufferUpdate() {
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

        collectedBuffers.clear();
        int totalFrames = 0;

        // Collect buffers until we have enough frames
        for (int i = audioBuffers.size() - 1; i >= 0 && totalFrames < requiredSamples; i--) {
            float[][] buf = audioBuffers.get(i);
            collectedBuffers.add(0, buf); // Insert at the beginning
            totalFrames += buf[0].length;
        }

        // Create a result array 
        int resultFrames = Math.min(totalFrames, requiredSamples);
        if (recentAudioWindow == null || lastRequiredSamples != resultFrames || recentAudioWindow.length != channels) {
            logger.trace("Creating recent audio window: {}x{}", channels, resultFrames);
            recentAudioWindow = new float[channels][resultFrames];
            lastRequiredSamples = resultFrames;
        }

        int copied = 0;
        for (float[][] buf : collectedBuffers) {
            int bufLength = buf[0].length;
            int toCopy = Math.min(bufLength, resultFrames - copied);

            for (int ch = 0; ch < channels; ch++) {
                System.arraycopy(buf[ch], bufLength - toCopy, recentAudioWindow[ch], copied, toCopy);
            }

            copied += toCopy;
            if (copied >= resultFrames) break;
        }
    }

    @Override
    protected int getSamplesOffset() {
        if (recentAudioWindow == null || recentAudioWindow.length == 0 || recentAudioWindow[0].length == 0) {
            return 0;
        }

        long now = System.nanoTime();
        long delta = now - getLastBufferUpdateTime();

        int offset = (int) (delta * getSampleRate() / 1000000000f);

        return offset;
    }
    

    @Override
    protected void onEnd() {
        audioBuffers.clear();
    }
}
