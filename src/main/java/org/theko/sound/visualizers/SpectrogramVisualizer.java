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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;
import org.theko.sound.utility.MathUtilities;

/**
 * A class that represents a spectrogram visualizer.
 * It can be used to display the spectrogram of an audio stream.
 * 
 * @see AudioVisualizer
 * 
 * @since 2.3.2
 * @author Theko
 */
public class SpectrogramVisualizer extends AudioVisualizer {
    
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);

    public static final VolumeColorProcessor GRAYSCALE_COLOR_MAP = v -> {
        float norm = MathUtilities.clamp(v, 0.0f, 1.0f);
        int intensity = (int) (norm * 255.0f);

        return (intensity << 16) | (intensity << 8) | intensity;
    };

    private static final ColorGradient INFERNO_COLOR_GRADIENT = ColorGradient.fromColors(
        List.of(
            new Color(250, 253, 161), // Light yellow
            new Color(251, 182, 26),  // Orange
            new Color(237, 105, 37),  // Red
            new Color(188, 55, 84),   // Dark pink
            new Color(120, 28, 109),  // Purple
            new Color(50, 10, 94),    // Dark purple
            new Color(0, 0, 0)        // Black
        )
    );

    public static final VolumeColorProcessor INFERNO_COLOR_MAP = v -> {
        float norm = MathUtilities.clamp(v, 0.0f, 1.0f);
        return INFERNO_COLOR_GRADIENT.getColorFromNormalizedValue(1.0f - norm);
    };

    protected float frequencyScale = 1.0f;
    protected float updateTime = 0.01f;
    protected VolumeColorProcessor volumeColorProcessor = INFERNO_COLOR_MAP;

    protected int channelToShow = 0;
    protected WindowType windowType = WindowType.BLACKMAN_HARRIS;
    protected int fftWindowSize = 8192;
    protected float minAmplitudeNormalizer = 1.0f;
    protected float amplitudeExponent = 2.0f;

    protected float normalizerRecoverySpeed = 0.1f;
    protected float currentAmplitudeNormalizer = minAmplitudeNormalizer;
    protected float normalizerDecaySpeed = 0.0f;

    protected static final float MIN_SCALE = 0.5f;
    protected static final float MAX_SCALE = 4.0f;
    protected static final float MIN_UPDATE_TIME = 0.001f;
    protected static final float MAX_UPDATE_TIME = 5.0f;

    protected static final int MIN_FFT_SIZE = 64;
    protected static final int MAX_FFT_SIZE = 16384;

    protected static final float MIN_DIVIDER = 0.0f;
    protected static final float MAX_DIVIDER = 1.0f;
    protected static final float MIN_AMPLITUDE_POWER = 0.25f;
    protected static final float MAX_AMPLITUDE_POWER = 10.0f;

    protected static final float MIN_DIVIDER_DECAY_SPEED = 0.0f;
    protected static final float MAX_DIVIDER_DECAY_SPEED = 1.0f;

    protected final List<AudioControl> visualizerControls = List.of(gainControl);

    protected final List<float[]> audioBuffers = new ArrayList<>(10);
    protected float[] recentAudioWindow = new float[channelToShow];
    private float[] window = null;
    
    protected SpectrumPanel spectrumPanel;
    protected BufferedImage spectrumImage;

    protected class SpectrumPanel extends JPanel {

        private float[] fftInputBuffer;
        private float[] fftSpectrum;
        private float[] mappingPositions;
        private float[] interpolatedSpectrum;
        private float[] real, imag;
        private BufferedImage line;
        private Graphics2D lineGraphics;

        private float timeSinceLastShift = 0f;
        private long lastRenderTime = System.nanoTime();

        @Override
        public void invalidate() {
            super.invalidate();
            // Recreate on paintComponent
            fftInputBuffer = null;
            fftSpectrum = null;
            mappingPositions = null;
            interpolatedSpectrum = null;
            real = null;
            imag = null;
            line = null;
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            if (lineGraphics != null) {
                lineGraphics.dispose();
                lineGraphics = null;
            }
            
            line = null;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            long now = System.nanoTime();
            float deltaTime = (now - lastRenderTime) / 1_000_000_000f;
            lastRenderTime = now;

            timeSinceLastShift += deltaTime;

            // Adaptive pixels shift
            int shiftPixels = 0;
            while (timeSinceLastShift >= updateTime) {
                timeSinceLastShift -= updateTime;
                shiftPixels++;
            }

            // Return if there is not enough samples
            if (recentAudioWindow == null) {
                if (spectrumImage != null) {
                    g.drawImage(spectrumImage, 0, 0, null);
                }
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            setupG2D(g2d);

            int offset = getSamplesOffset();
            offset = Math.max(0, Math.min(offset, recentAudioWindow.length - fftWindowSize));
            
            if (fftInputBuffer == null || fftInputBuffer.length != fftWindowSize) {
                fftInputBuffer = new float[fftWindowSize];
            }
            System.arraycopy(recentAudioWindow, offset, fftInputBuffer, 0, Math.min(recentAudioWindow.length - offset, fftWindowSize));

            if (fftSpectrum == null || fftSpectrum.length != fftWindowSize / 2) {
                fftSpectrum = new float[fftWindowSize / 2];
            }
            getSpectrum(fftInputBuffer, fftSpectrum);

            if (interpolatedSpectrum == null || interpolatedSpectrum.length != getHeight()) {
                interpolatedSpectrum = new float[getHeight()];
            }
            
            if (mappingPositions == null || mappingPositions.length != fftSpectrum.length) {
                mappingPositions = new float[fftSpectrum.length];
            }
            
            calculatePositions(mappingPositions, getHeight(), frequencyScale);
            mapSpectrum(fftSpectrum, mappingPositions, interpolatedSpectrum);

            if (line == null || line.getHeight() != getHeight()) {
                if (lineGraphics != null) lineGraphics.dispose();
                line = new BufferedImage(1, getHeight(), BufferedImage.TYPE_INT_ARGB);
                lineGraphics = line.createGraphics();
            }
            
            lineGraphics.setColor(Color.BLACK);
            lineGraphics.fillRect(0, 0, 1, getHeight());

            for (int y = 0; y < getHeight(); y++) {
                float currentAmplitude = MathUtilities.clamp(interpolatedSpectrum[y], 0.0f, 1.0f);
                int color = (volumeColorProcessor == null) ? 
                    GRAYSCALE_COLOR_MAP.getColor(currentAmplitude) : 
                    volumeColorProcessor.getColor(currentAmplitude);
                line.setRGB(0, getHeight() - y - 1, color);
            }

            if (spectrumImage == null || 
                spectrumImage.getWidth() != getWidth() || 
                spectrumImage.getHeight() != getHeight()) {
                spectrumImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g2dImage = spectrumImage.createGraphics();
            setupG2D(g2dImage);

            if (shiftPixels < getWidth()) {
                g2dImage.copyArea(shiftPixels, 0, getWidth() - shiftPixels, getHeight(), -shiftPixels, 0);
                g2dImage.setColor(Color.BLACK);
                g2dImage.fillRect(getWidth() - shiftPixels, 0, shiftPixels, getHeight());
            } else {
                g2dImage.clearRect(0, 0, getWidth(), getHeight());
            }

            for (int i = 0; i < shiftPixels; i++) {
                int x = getWidth() - shiftPixels + i;
                if (x >= 0 && x < getWidth()) {
                    g2dImage.drawImage(line, x, 0, null);
                }
            }

            g2dImage.dispose();
            g2d.drawImage(spectrumImage, 0, 0, null);
        }

        private void setupG2D(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }

        private void getSpectrum(float[] inputSamples, float[] outputSpectrum) {
            if (inputSamples.length == 0) return;
            
            int fftLength = fftWindowSize;

            if (real == null || real.length != fftLength) {
                real = new float[fftLength];
            }
            if (imag == null || imag.length != fftLength) {
                imag = new float[fftLength];
            }
            
            System.arraycopy(inputSamples, 0, real, 0, inputSamples.length);
            Arrays.fill(imag, 0.0f);

            if (Math.abs(gainControl.getValue() - 1.0f) > 1e-6f) {
                for (int i = 0; i < inputSamples.length; i++) {
                    real[i] *= gainControl.getValue();
                }
            }

            WindowFunction.applyInPlace(real, windowType);
            
            FFT.fft(real, imag);
            
            float maxAmplitude = 0.0f;
            float minNormalizerRoot = (float) Math.pow(minAmplitudeNormalizer, 1 / amplitudeExponent);
            
            int spectrumLength = Math.min(real.length / 2, outputSpectrum.length);
            for (int i = 0; i < spectrumLength; i++) {
                float magnitude = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
                outputSpectrum[i] = (float) Math.log1p(Math.pow(magnitude, amplitudeExponent));
                if (outputSpectrum[i] > maxAmplitude) maxAmplitude = outputSpectrum[i];
            }

            currentAmplitudeNormalizer -= normalizerDecaySpeed;
            maxAmplitude = Math.max(minNormalizerRoot, maxAmplitude) * 1.25f;

            if (currentAmplitudeNormalizer < maxAmplitude) {
                currentAmplitudeNormalizer = maxAmplitude;
                normalizerDecaySpeed = 0.0f;
            }

            for (int i = 0; i < spectrumLength; i++) {
                outputSpectrum[i] = MathUtilities.clamp(outputSpectrum[i] / currentAmplitudeNormalizer, 0.0f, 1.0f);
            }
        }

        private void calculatePositions(float[] outPositions, int height, float scale) {
            for (int i = 0; i < outPositions.length; i++) {
                double normalized = Math.log10(1 + i) / Math.log10(1 + outPositions.length);
                double scaled = Math.pow(normalized, scale);
                outPositions[i] = (float) (scaled * (height - 1));
            }
        }

        private void mapSpectrum(float[] inputSpectrum, float[] positions, float[] interpolatedSpectrumOut) {
            Arrays.fill(interpolatedSpectrumOut, 0.0f);
            
            for (int i = 0; i < positions.length - 1; i++) {
                int startX = (int) positions[i];
                int endX = (int) positions[i + 1];

                if (startX >= interpolatedSpectrumOut.length || endX >= interpolatedSpectrumOut.length) {
                    continue;
                }

                float startValue = inputSpectrum[i];
                float endValue = inputSpectrum[i + 1];

                for (int x = startX; x <= endX; x++) {
                    if (x >= interpolatedSpectrumOut.length) break;
                    
                    float t = (x - positions[i]) / (positions[i + 1] - positions[i] + 1e-6f);
                    t = MathUtilities.clamp(t, 0, 1);
                    float value = MathUtilities.lerp(startValue, endValue, t);
                    
                    if (value > interpolatedSpectrumOut[x]) {
                        interpolatedSpectrumOut[x] = value;
                    }
                }
            }
        }
    }

    public SpectrogramVisualizer (float frameRate) {
        super(Type.REALTIME, frameRate);
        addControls(visualizerControls);
    }

    public FloatControl getGainControl () {
        return gainControl;
    }

    public void setFrequencyScale (float frequencyScale) {
        this.frequencyScale = MathUtilities.clamp(frequencyScale, MIN_SCALE, MAX_SCALE);
    }

    public float getFrequencyScale () {
        return frequencyScale;
    }

    public void setUpdateTime (float updateTime) {
        this.updateTime = MathUtilities.clamp(updateTime, MIN_UPDATE_TIME, MAX_UPDATE_TIME);
    }

    public float getUpdateTime () {
        return updateTime;
    }

    public void setMinAmplitudeNormalizer (float divider) {
        this.minAmplitudeNormalizer = MathUtilities.clamp(divider, MIN_DIVIDER, MAX_DIVIDER);
    }

    public float getMinAmplitudeNormalizer () {
        return minAmplitudeNormalizer;
    }

    public void setAmplitudeExponent (float power) {
        this.amplitudeExponent = MathUtilities.clamp(power, MIN_AMPLITUDE_POWER, MAX_AMPLITUDE_POWER);
    }

    public float getAmplitudeExponent () {
        return amplitudeExponent;
    }

    public void setNormalizerDecaySpeed (float speed) {
        this.normalizerDecaySpeed = MathUtilities.clamp(speed, MIN_DIVIDER_DECAY_SPEED, MAX_DIVIDER_DECAY_SPEED);
    }

    public float getNormalizerDecaySpeed () {
        return normalizerDecaySpeed;
    }

    public void setFftWindowSize(int size) {
        this.fftWindowSize = MathUtilities.clamp(size, MIN_FFT_SIZE, MAX_FFT_SIZE);
        if (getPanel() != null) {
            getPanel().invalidate();
        }
    }

    public int getFftWindowSize () {
        return fftWindowSize;
    }

    public void setWindowType (WindowType type) {
        this.windowType = type;
    }

    public WindowType getWindowType () {
        return windowType;
    }

    public void setChannelToShow (int channel) {
        if (channel < 0 || channel > getSamplesBuffer().length) return;
        this.channelToShow = channel;
    }

    public int getChannelToShow () {
        return channelToShow;
    }

    @Override
    protected void initialize() {
        SpectrumPanel panel = new SpectrumPanel();
        panel.setOpaque(false);
        panel.setBackground(new Color(0, 0, 0, 0));
        setPanel(panel);
    }

    @Override
    protected void repaint () {
        getPanel().repaint();
    }
    
    @Override
    protected void onBufferUpdate () {
        float[] newSamples = getSamplesBuffer()[channelToShow];
        audioBuffers.add(newSamples);

        int requiredSamples = fftWindowSize + getLength();

        int totalSamples = 0;
        for (float[] buf : audioBuffers)
            totalSamples += buf.length;

        while (totalSamples > requiredSamples && audioBuffers.size() > 1) {
            totalSamples -= audioBuffers.remove(0).length;
        }

        if (window == null || window.length != requiredSamples) {
            window = new float[requiredSamples];
        }
        if (window.length > totalSamples) {
            Arrays.fill(window, 0.0f);
        }
        int pos = 0;
        int minLength = Math.min(requiredSamples, totalSamples);
        for (float[] buf : audioBuffers) {
            int copyLen = Math.min(buf.length, minLength - pos);
            System.arraycopy(buf, buf.length - copyLen, window, pos, copyLen);
            pos += copyLen;
            if (pos >= window.length) break;
        }

        this.recentAudioWindow = window;
    }

    @Override
    protected int getSamplesOffset () {
        if (recentAudioWindow == null || recentAudioWindow.length <= fftWindowSize)
            return 0;

        long now = System.nanoTime();
        long delta = now - getLastBufferUpdateTime();

        int offset = (int) (delta * getSampleRate() / 1_000_000_000L);

        // Limit offset
        offset = Math.max(0, Math.min(offset, recentAudioWindow.length - fftWindowSize));
        return offset;
    }
}
