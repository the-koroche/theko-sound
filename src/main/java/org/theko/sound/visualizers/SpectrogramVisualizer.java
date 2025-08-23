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

        return new Color(intensity, intensity, intensity);
    };

    public static final VolumeColorProcessor INFERNO_COLOR_MAP = v -> {
        final Color[] colors = {
            new Color(250, 253, 161), // Light yellow
            new Color(251, 182, 26),  // Orange
            new Color(237, 105, 37),  // Red
            new Color(188, 55, 84),   // Dark pink
            new Color(120, 28, 109),  // Purple
            new Color(50, 10, 94),    // Dark purple
            new Color(0, 0, 0)        // Black
        };
        final ColorGradient gradient = new ColorGradient(Arrays.asList(colors));

        float norm = MathUtilities.clamp(v, 0.0f, 1.0f);
        return gradient.getColorFromNormalizedValue(1.0f - norm);
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
    
    protected BufferedImage spectrumImage;

    protected class SpectrumPanel extends JPanel {

        private float timeSinceLastShift = 0f;
        private long lastRenderTime = System.nanoTime();
        
        @Override
        protected void paintComponent (Graphics g) {
            super.paintComponent(g);

            long now = System.nanoTime();
            float deltaTime = (now - lastRenderTime) / 1_000_000_000f;
            lastRenderTime = now;

            timeSinceLastShift += deltaTime;

            int shiftPixels = 0;
            while (timeSinceLastShift >= updateTime) {
                timeSinceLastShift -= updateTime;
                shiftPixels++;
            }

            if (shiftPixels == 0 && spectrumImage != null) {
                g.drawImage(spectrumImage, 0, 0, null);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            setupG2D(g2d);

            float[] sampleBuffer = recentAudioWindow;
            int offset = getSamplesOffset();
            offset = Math.max(0, Math.min(offset, sampleBuffer.length - fftWindowSize));
            float[] fftInputBuffer = Arrays.copyOfRange(sampleBuffer, offset, offset + fftWindowSize);
            float[] spectrum = getSpectrum(fftInputBuffer);
            float[] interpolatedSpectrum = mapSpectrum(spectrum, getHeight(), frequencyScale);

            BufferedImage line = new BufferedImage(1, getHeight(), BufferedImage.TYPE_INT_ARGB);
            int binsPerPixel = (int) (interpolatedSpectrum.length / getHeight());

            for (int y = 0; y < getHeight(); y++) {
                float currentAmplitude = MathUtilities.clamp(interpolatedSpectrum[y * binsPerPixel], 0.0f, 1.0f);
                
                Color color = (volumeColorProcessor == null) ? GRAYSCALE_COLOR_MAP.getColor(currentAmplitude) : volumeColorProcessor.getColor(currentAmplitude);

                line.setRGB(0, getHeight() - y - 1, color.getRGB());
            }

            if (spectrumImage == null || spectrumImage.getWidth() != getWidth() || spectrumImage.getHeight() != getHeight()) {
                spectrumImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g2dImage = spectrumImage.createGraphics();
            setupG2D(g2dImage);

            if (shiftPixels < getWidth()) {
                g2dImage.copyArea(shiftPixels, 0, getWidth() - shiftPixels, getHeight(), -shiftPixels, 0);
            } else {
                g2dImage.clearRect(0, 0, getWidth(), getHeight());
            }

            for (int i = 0; i < shiftPixels; i++) {
                int x = getWidth() - shiftPixels + i;
                if (x >= 0 && x < getWidth()) {
                    g2dImage.drawImage(line, x, 0, null);
                }
            }

            g2d.drawImage(spectrumImage, 0, 0, null);
        }

        private void setupG2D (Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        }

        private float[] mapSpectrum(float[] spectrum, int width, float scale) {
            float[] mapped = new float[width];
            float[] positions = new float[spectrum.length];

            // Calculate positions
            for (int i = 0; i < spectrum.length; i++) {
                double normalized = Math.log10(1 + i) / Math.log10(1 + spectrum.length);
                double scaled = Math.pow(normalized, scale);
                positions[i] = (float) (scaled * (width - 1));
            }

            // Interpolate between positions
            for (int i = 0; i < spectrum.length - 1; i++) {
                int startX = (int) positions[i];
                int endX = (int) positions[i + 1];

                float startValue = spectrum[i];
                float endValue = spectrum[i + 1];

                for (int x = startX; x <= endX; x++) {
                    float t = (x - positions[i]) / (positions[i + 1] - positions[i] + 1e-6f); // Add epsilon to avoid division by zero
                    t = MathUtilities.clamp(t, 0, 1);

                    float value = MathUtilities.lerp(startValue, endValue, t);
                    mapped[x] = value;
                }
            }

            return mapped;
        }

        private float[] getSpectrum (float[] samples) {
            if (samples.length == 0) return new float[channelToShow];
            
            int fftLength = fftWindowSize;

            float[] real = new float[fftLength];
            float[] imag = new float[fftLength];
            System.arraycopy(samples, 0, real, 0, samples.length);

            if (Math.abs(gainControl.getValue() - 1.0f) > 1e-6f) {
                for (int i = 0; i < samples.length; i++) {
                    real[i] *= gainControl.getValue();
                }
            }

            real = WindowFunction.apply(real, windowType);
            
            FFT.fft(real, imag);
            
            float maxAmplitude = 0.0f;
            float minNormalizerRoot = (float) Math.pow(minAmplitudeNormalizer, 1 / amplitudeExponent);
            
            float[] spectrum = new float[real.length / 2];
            for (int i = 0; i < real.length / 2; i++) {
                spectrum[i] = (float) Math.log1p(Math.pow(Math.sqrt(real[i] * real[i] + imag[i] * imag[i]), amplitudeExponent));
                if (spectrum[i] > maxAmplitude) maxAmplitude = spectrum[i];
            }

            currentAmplitudeNormalizer -= normalizerDecaySpeed;
 
            maxAmplitude = Math.max(minNormalizerRoot, maxAmplitude) * 1.25f;

            if (currentAmplitudeNormalizer < maxAmplitude) {
                currentAmplitudeNormalizer = maxAmplitude;
                normalizerDecaySpeed = 0.0f;
            }

            for (int i = 0; i < spectrum.length; i++) {
                spectrum[i] /= currentAmplitudeNormalizer;
            }
            return spectrum;
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

    public void setFftWindowSize (int size) {
        this.fftWindowSize = MathUtilities.clamp(size, MIN_FFT_SIZE, MAX_FFT_SIZE);
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
    protected void initialize () {
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

        float[] window = new float[Math.min(totalSamples, requiredSamples)];
        int pos = 0;
        for (float[] buf : audioBuffers) {
            int copyLen = Math.min(buf.length, window.length - pos);
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
