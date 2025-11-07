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
 * Real-time audio spectrum visualizer that displays frequency content using FFT analysis.
 * Supports multiple interpolation modes, customizable coloring, and various spectrum processing options.
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Real-time FFT-based frequency analysis</li>
 *   <li>Multiple interpolation modes for smooth visualization</li>
 *   <li>Configurable window functions and FFT sizes</li>
 *   <li>Dynamic amplitude normalization with decay</li>
 *   <li>Dual/single bar display modes</li>
 * </ul>
 * 
 * @see AudioVisualizer
 * @see #setFrequencyScale(float)
 * 
 * @since 2.2.0
 * @author Theko
 */
public class SpectrumVisualizer extends AudioVisualizer {
    
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);

    protected float frequencyScale = 1.0f;
    protected Color upperBarColor = Color.LIGHT_GRAY;
    protected Color lowerBarColor = Color.GRAY;
    protected boolean drawDoubleBars = true;

    protected InterpolationMode spectrumInterpolationMode = InterpolationMode.EASING;

    protected int channelToShow = 0;
    protected WindowType windowType = WindowType.HANN;
    protected int fftWindowSize = 1024;
    protected float minAmplitudeNormalizer = 1.0f;
    protected float amplitudeExponent = 2.0f;

    protected float normalizerRecoverySpeed = 0.1f;
    protected float currentAmplitudeNormalizer = minAmplitudeNormalizer;
    protected float normalizerDecaySpeed = 0.0f;

    protected float spectrumDecayFactor = 0.8f;

    protected static final float MIN_SCALE = 0.5f;
    protected static final float MAX_SCALE = 4.0f;
    protected static final float MIN_WEIGHT = 0.1f;
    protected static final float MAX_WEIGHT = 10.0f;
    protected static final float MIN_DECAY = 0.0f;
    protected static final float MAX_DECAY = 1.0f;

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

    protected float[] displayedSpectrum = new float[channelToShow];

    /**
     * An enum that represents the interpolation mode, between bins.
     */
    public enum InterpolationMode {
        NONE,
        LINEAR,
        EASING
    }

    /**
     * A class that represents a spectrum panel.
     */
    protected class SpectrumPanel extends JPanel {

        private float[] fftInputBuffer;
        private float[] fftSpectrum;
        private float[] mappingPositions;
        private float[] interpolatedSpectrum;
        private float[] real, imag;

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
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            setupG2D(g2d);

            // Return if there is not enough samples
            if (recentAudioWindow == null) {
                return;
            }

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

            if (interpolatedSpectrum == null || interpolatedSpectrum.length != getWidth()) {
                interpolatedSpectrum = new float[getWidth()];
            }
            
            if (mappingPositions == null || mappingPositions.length != fftSpectrum.length) {
                mappingPositions = new float[fftSpectrum.length];
            }
            
            calculatePositions(mappingPositions, getWidth(), frequencyScale);
            mapSpectrum(fftSpectrum, mappingPositions, interpolatedSpectrum);

            // Process after mapping
            for (int i = 0; i < interpolatedSpectrum.length; i++) {
                interpolatedSpectrum[i] *= spectrumDecayFactor;
            }

            int binsPerPixel = (int) (interpolatedSpectrum.length / getWidth());

            g2d.setColor(upperBarColor);
            for (int x = 0; x < getWidth(); x++) {
                float maxBinAmplitude = 0f;

                int binStart = (int) (x * binsPerPixel);
                int binEnd = Math.min((int) ((x + 1) * binsPerPixel), interpolatedSpectrum.length);

                for (int j = binStart; j < binEnd; j++) {
                    maxBinAmplitude = Math.max(maxBinAmplitude, interpolatedSpectrum[j]);
                }

                int amplitudePixels = (int) (maxBinAmplitude * getHeight());
                int halfAmplitudePixels = amplitudePixels / 2;
                int centerY = getHeight() / 2;

                int xPosition = x;

                if (drawDoubleBars) {
                    // Upper bar
                    g2d.setColor(upperBarColor);
                    g2d.fillRect(xPosition, centerY - halfAmplitudePixels, 1, halfAmplitudePixels);

                    // Lower bar
                    g2d.setColor(lowerBarColor);
                    g2d.fillRect(xPosition, centerY, 1, halfAmplitudePixels);
                } else {
                    int yPosition = getHeight() - amplitudePixels;
                    g2d.setColor(upperBarColor);
                    g2d.fillRect(xPosition, yPosition, 1, amplitudePixels);
                }
            }
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
            boolean isEasing = spectrumInterpolationMode == InterpolationMode.EASING;
            boolean needToInterpolate = spectrumInterpolationMode == InterpolationMode.LINEAR || isEasing;
            
            for (int i = 0; i < positions.length - 1; i++) {
                int startX = (int) positions[i];
                int endX = (int) positions[i + 1];

                if (startX >= interpolatedSpectrumOut.length || endX >= interpolatedSpectrumOut.length) {
                    continue;
                }

                float startValue = inputSpectrum[i];
                float endValue = inputSpectrum[i + 1];

                for (int x = startX; x <= endX; x++) {
                    if (needToInterpolate) {
                        float t = (x - positions[i]) / (positions[i + 1] - positions[i] + 1e-6f); // Add epsilon to avoid division by zero
                        t = Math.max(0, Math.min(1, t)); // clamp
                        
                        // Ease In-Out
                        if (isEasing) {
                            t = t * t * (3 - 2 * t);
                        }

                        // Lerp
                        float value = startValue * (1 - t) + endValue * t;
                        if (value > interpolatedSpectrumOut[x]) {
                            interpolatedSpectrumOut[x] = value;
                        }
                    } else {
                        if (startValue > interpolatedSpectrumOut[x]) {
                            interpolatedSpectrumOut[x] = startValue;
                        }
                    }
                }
            }
        }
    }

    public SpectrumVisualizer(float frameRate) {
        super(Type.REALTIME, frameRate);
        addControls(visualizerControls);
    }

    /**
     * Returns the gain control for the spectrum visualizer.
     * The gain control allows adjusting the amplitude of the audio signal
     * that is used to generate the spectrum display.
     * 
     * @return The gain control of the spectrum visualizer.
     */
    public FloatControl getGainControl() {
        return gainControl;
    }

    /**
     * Sets the frequency scale of the spectrum visualizer.
     * The frequency scale is used to determine the frequency range of the
     * spectrum that is displayed. The frequency scale is relative to the
     * sampling rate of the audio signal, with a frequency scale of 1.0f
     * representing the full frequency range of the sampling rate and a
     * frequency scale of 0.5f representing half of the full frequency range.
     * A frequency scale of 2.0f would represent twice the full frequency range
     * of the sampling rate.
     *
     * @param frequencyScale The frequency scale of the spectrum visualizer.
     */
    public void setFrequencyScale(float frequencyScale) {
        this.frequencyScale = MathUtilities.clamp(frequencyScale, MIN_SCALE, MAX_SCALE);
    }

    /**
     * Returns the frequency scale of the spectrum visualizer.
     * The frequency scale is used to determine the frequency range of the
     * spectrum that is displayed.
     * 
     * @return The frequency scale of the spectrum visualizer.
     */
    public float getFrequencyScale() {
        return frequencyScale;
    }

    /**
     * Sets the color used to draw the upper bar when the visualizer is
     * configured to draw two bars representing the amplitude of the upper
     * and lower channel of the audio signal.
     * 
     * @param color The color used to draw the upper bar
     */
    public void setUpperBarColor(Color color) {
        this.upperBarColor = color;
    }

    /**
     * Gets the color used to draw the upper bar when the visualizer is
     * configured to draw two bars representing the amplitude of the upper
     * and lower channel of the audio signal.
     * 
     * @return The color used to draw the upper bar
     */
    public Color getUpperBarColor() {
        return upperBarColor;
    }

    /**
     * Sets the color used to draw the lower bar when the visualizer is
     * configured to draw two bars representing the amplitude of the upper
     * and lower channel of the audio signal.
     *
     * @param color The color used to draw the lower bar
     */
    public void setLowerBarColor(Color color) {
        this.lowerBarColor = color;
    }

    /**
     * Returns the color used to draw the lower bar when the visualizer is
     * configured to draw two bars representing the amplitude of the upper
     * and lower channel of the audio signal.
     *
     * @return the color used to draw the lower bar
     */
    public Color getLowerBarColor() {
        return lowerBarColor;
    }

    /**
     * Sets whether or not the visualizer should draw two bars representing the
     * amplitude of the upper and lower channel of the audio signal, or a single
     * bar representing the amplitude of the audio signal.
     * 
     * @param doubleBars true if the visualizer should draw two bars, false if it
     * should draw a single bar.
     */
    public void setDoubleBars(boolean doubleBars) {
        this.drawDoubleBars = doubleBars;
    }

    /**
     * Returns true if the visualizer is currently configured to draw two bars
     * representing the amplitude of the upper and lower channel of the audio
     * signal, and false if it is configured to draw a single bar representing
     * the amplitude of the audio signal.
     * 
     * @return true if the visualizer is configured to draw two bars, false
     * otherwise
     */
    public boolean isDoubleBars() {
        return drawDoubleBars;
    }

    /**
     * Returns true if the visualizer is currently configured to draw a single
     * bar representing the amplitude of the audio signal, and false if it is
     * configured to draw two bars, one for the upper and one for the lower
     * channel of the audio signal.
     * 
     * @return true if the visualizer is configured to draw a single bar,
     * false otherwise
     */
    public boolean isSingleBar() {
        return !drawDoubleBars;
    }

    /**
     * Sets the spectrum decay factor of the visualizer.
     * This value controls how quickly the spectrum decays back to zero.
     * 
     * @param decay The spectrum decay factor of the visualizer.
     */
    public void setSpectrumDecayFactor(float decay) {
        this.spectrumDecayFactor = MathUtilities.clamp(decay, MIN_DECAY, MAX_DECAY);
    }

    /**
     * Gets the spectrum decay factor of the visualizer.
     * This value controls how quickly the spectrum decays back to zero after the
     * audio signal has stopped.
     * 
     * @return The spectrum decay factor of the visualizer.
     */
    public float getSpectrumDecayFactor() {
        return spectrumDecayFactor;
    }

    /**
     * Sets the minimum amplitude normalizer of the visualizer.
     * This value is used to scale the amplitude of the spectrum before it is
     * displayed. The amplitude normalizer is calculated as follows:
     * amplitude = Math.max(minNormalizerRoot, maxAmplitude) * 1.25f;
     * where minNormalizerRoot is the square root of the minimum amplitude normalizer
     * and maxAmplitude is the maximum amplitude of the spectrum.
     * 
     * @param divider The minimum amplitude normalizer of the visualizer.
     */
    public void setMinAmplitudeNormalizer(float divider) {
        this.minAmplitudeNormalizer = MathUtilities.clamp(divider, MIN_DIVIDER, MAX_DIVIDER);
    }

    /**
     * Returns the minimum amplitude normalizer of the visualizer.
     * 
     * @return The minimum amplitude normalizer of the visualizer.
     */
    public float getMinAmplitudeNormalizer() {
        return minAmplitudeNormalizer;
    }

    /**
     * Sets the amplitude exponent of the visualizer.
     * This value is used to scale the amplitude of the spectrum before it is
     * displayed. The amplitude exponent is applied as follows: amplitude =
     * Math.pow(amplitude, amplitudeExponent).
     * 
     * @param power The amplitude exponent of the visualizer.
     */
    public void setAmplitudeExponent(float power) {
        this.amplitudeExponent = MathUtilities.clamp(power, MIN_AMPLITUDE_POWER, MAX_AMPLITUDE_POWER);
    }

    /**
     * Returns the amplitude exponent of the visualizer.
     * 
     * @return the amplitude exponent of the visualizer
     */
    public float getAmplitudeExponent() {
        return amplitudeExponent;
    }

    /**
     * Sets the decay speed of the amplitude normalizer.
     * A decay speed of 0.0f will cause the amplitude normalizer to recover
     * instantly, while a decay speed of 1.0f will cause the amplitude normalizer
     * to recover very slowly.
     * 
     * @param speed The decay speed of the amplitude normalizer.
     */
    public void setNormalizerDecaySpeed(float speed) {
        this.normalizerDecaySpeed = MathUtilities.clamp(speed, MIN_DIVIDER_DECAY_SPEED, MAX_DIVIDER_DECAY_SPEED);
    }

    /**
     * Returns the decay speed of the amplitude normalizer.
     * 
     * @return The decay speed of the amplitude normalizer.
     */
    public float getNormalizerDecaySpeed() {
        return normalizerDecaySpeed;
    }

    /**
     * Sets the interpolation mode used to generate the spectrum.
     * 
     * <p>
     * The interpolation mode determines how the spectrum is generated from the
     * input samples. If the interpolation mode is set to
     * {@link InterpolationMode#LINEAR}, the spectrum is generated by
     * linearly interpolating between the input samples. If the interpolation
     * mode is set to {@link InterpolationMode#EASING}, the spectrum is
     * generated by applying an easing function to the input samples.
     * Otherwise, if the interpolation mode is set to
     * {@link InterpolationMode#NONE}, the spectrum is generated by copying the
     * bin value until next bin.
     * 
     * @param mode The interpolation mode used to generate the spectrum.
     * @see InterpolationMode
     */
    public void setSpectrumInterpolationMode(InterpolationMode mode) {
        this.spectrumInterpolationMode = mode;
    }

    /**
     * Returns the current interpolation mode used to generate the spectrum.
     * 
     * @return The current interpolation mode used.
     * @see InterpolationMode
     */
    public InterpolationMode getSpectrumInterpolationMode() {
        return spectrumInterpolationMode;
    }

    /**
     * Sets the size of the FFT used to generate the spectrum.
     * A larger FFT size will provide a more detailed spectrum, but will also
     * increase the computational cost of generating the spectrum, and may cause
     * lower time resolution.
     * 
     * @param size The size of the FFT to use.
     */
    public void setFftWindowSize(int size) {
        this.fftWindowSize = MathUtilities.clamp(size, MIN_FFT_SIZE, MAX_FFT_SIZE);
    }

    /**
     * Returns the size of the FFT used to generate the spectrum.
     * 
     * @return the size of the FFT used.
     */
    public int getFftWindowSize() {
        return fftWindowSize;
    }

    /**
     * Sets the window type used before the FFT is applied.
     * 
     * <p>
     * Window functions are commonly applied to signals to reduce spectral leakage
     * when performing Fourier transforms. Each window type provides a different
     * trade-off between main lobe width and side lobe attenuation.
     * 
     * @param type the type of window function to apply before the FFT
     */
    public void setWindowType(WindowType type) {
        if (type == null) return;
        this.windowType = type;
    }

    /**
     * Returns the window type used before the FFT is applied.
     * 
     * @return The window type used.
     */
    public WindowType getWindowType() {
        return windowType;
    }

    /**
     * Sets the channel to show in the spectrogram visualizer.
     * 
     * @param channel The channel to show (0-based index)
     * @throws IllegalArgumentException if the channel is out of range (less than 0 or greater than the number of channels in the samples buffer)
     */
    public void setChannelToShow(int channel) {
        if (channel < 0 || channel > getSamplesBuffer().length) {
            throw new IllegalArgumentException("Channel must be between 0 and the number of channels in the samples buffer.");
        }
        this.channelToShow = channel;
    }

    /**
     * Returns the channel to show in the spectrogram visualizer.
     * 
     * @return The channel to show.
     * @see #setChannelToShow(int)
     */
    public int getChannelToShow() {
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
    protected void repaint() {
        getPanel().repaint();
    }
    
    @Override
    protected void onBufferUpdate() {
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
    protected int getSamplesOffset() {
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
