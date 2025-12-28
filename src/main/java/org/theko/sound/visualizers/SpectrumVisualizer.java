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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;
import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.*;
import org.theko.sound.utility.MathUtilities;

/**
 * Real-time audio spectrum visualizer that displays frequency content using FFT analysis.
 * Supports multiple interpolation modes, customizable coloring, and various spectrum processing options.
 * 
 * @see AudioVisualizer
 * @see #setFrequencyScale(float)
 * 
 * @since 2.2.0
 * @author Theko
 */
public class SpectrumVisualizer extends AudioVisualizer {
    
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);
    private float frequencyScale = 1.0f;
    private Color upperBarColor = Color.WHITE;
    private Color lowerBarColor = Color.LIGHT_GRAY;
    private VolumeColorProcessor volumeColorProcessor = ColorGradient.BRIGHT_INFERNO_COLOR_MAP.getVolumeColorProcessor();
    protected boolean useColorProcessor = true;
    protected boolean drawDoubleBars = false;

    private InterpolationMode spectrumInterpolationMode = InterpolationMode.EASING;
    private int fixedWidthBarCount = 24;
    private float fixedWidthBarWidth = 0.75f;

    private int channelToShow = 0;
    private WindowType windowType = WindowType.HANN;
    private int fftWindowSize = 2048;
    private float minAmplitudeNormalizer = 1.0f;
    private float amplitudeExponent = 2.0f;

    private float currentAmplitudeNormalizer = minAmplitudeNormalizer;
    private float normalizerDecaySpeed = 0.0f;

    private float spectrumDecayFactor = 0.86f;
    private DecayMode spectrumDecayMode = DecayMode.MULTIPLY;

    protected static final float MIN_SCALE = 0.5f;
    protected static final float MAX_SCALE = 4.0f;
    protected static final float MIN_DECAY = 0.0f;
    protected static final float MAX_DECAY = 1.0f;
    protected static final float MIN_BAR_WIDTH = 0.0f;
    protected static final float MAX_BAR_WIDTH = 1.0f;

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

    /**
     * An enum that represents the interpolation mode, between bins.
     */
    public enum InterpolationMode {
        NEAREST,
        LINEAR,
        EASING,
        FIXED_WIDTH
    }

    /**
     * An enum that represents the decay mode, between bins.
     */
    public enum DecayMode {
        MULTIPLY,
        INTERPOLATE
    }

    /**
     * A class that represents a spectrum render.
     */
    protected class SpectrumRender extends AudioVisualizer.Render {

        private float[] fftInputBuffer;
        private float[] fftSpectrum;
        private float[] mappingPositions;
        private float[] interpolatedSpectrum;
        private float[] drawnSpectrum;
        private float[] real, imag;
        private float lastFrequencyScale = -1;

        public SpectrumRender(int width, int height) {
            super(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        protected void invalidate() {
            super.invalidate();
            lastFrequencyScale = -1;
            fftInputBuffer = null;
            fftSpectrum = null;
            mappingPositions = null;
            interpolatedSpectrum = null;
            drawnSpectrum = null;
            real = null; imag = null;
        }
        
        @Override
        protected void paint(Graphics2D g2d) {
            g2d.clearRect(0, 0, getWidth(), getHeight());

            // Do nothing if there is not enough samples
            if (recentAudioWindow == null || recentAudioWindow.length == 0) {
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
                lastFrequencyScale = frequencyScale;
                getScaledPositions(mappingPositions, getWidth(), frequencyScale);
            }
            
            if (lastFrequencyScale != frequencyScale) {
                lastFrequencyScale = frequencyScale;
                getScaledPositions(mappingPositions, getWidth(), frequencyScale);
            }
            mapSpectrum(fftSpectrum, mappingPositions, getWidth(), interpolatedSpectrum);

            if (drawnSpectrum == null) {
                drawnSpectrum = new float[interpolatedSpectrum.length];
            }

            switch (spectrumDecayMode) {
                case MULTIPLY -> {
                    for (int i = 0; i < drawnSpectrum.length; i++) {
                        drawnSpectrum[i] = Math.max(drawnSpectrum[i] * spectrumDecayFactor, interpolatedSpectrum[i]);
                    }
                }
                case INTERPOLATE -> {
                    for (int i = 0; i < drawnSpectrum.length; i++) {
                        drawnSpectrum[i] = MathUtilities.lerp(interpolatedSpectrum[i], drawnSpectrum[i], spectrumDecayFactor);
                    }
                }
            }

            int[] pixels = ((DataBufferInt) getRenderImage().getRaster().getDataBuffer()).getData();
            for (int x = 0; x < getWidth(); x++) {
                float maxBinAmplitude = 0f;

                int binStart = x;
                int binEnd = Math.min((int) (x + 1), drawnSpectrum.length);

                for (int j = binStart; j < binEnd; j++) {
                    maxBinAmplitude = Math.max(maxBinAmplitude, drawnSpectrum[j]);
                }

                int amplitudePixels = (int) (maxBinAmplitude * getHeight());
                int halfAmplitudePixels = amplitudePixels / 2;
                int centerY = getHeight() / 2;

                int targetUpperColor = (useColorProcessor ?
                        volumeColorProcessor.getColor(maxBinAmplitude) :
                        upperBarColor.getRGB());

                if (drawDoubleBars) {
                    int targetLowerColor = (useColorProcessor ?
                            volumeColorProcessor.getColor(maxBinAmplitude) :
                            lowerBarColor.getRGB());

                    // Upper bar
                    fillRect(x, centerY - halfAmplitudePixels, 1, halfAmplitudePixels, targetUpperColor, pixels);

                    // Lower bar
                    fillRect(x, centerY, 1, halfAmplitudePixels, targetLowerColor, pixels);
                } else {
                    int yPosition = getHeight() - amplitudePixels;
                    fillRect(x, yPosition, 1, amplitudePixels, targetUpperColor, pixels);
                }
            }
        }

        private void fillRect(int x, int y, int w, int h, int argb, int[] pixels) {
            if (x < 0 || y < 0 || x + w > getWidth() || y + h > getHeight()) return;
            int start = y * getWidth() + x;
            for (int row = 0; row < h; row++) {
                int rowStart = start + row * getWidth();
                Arrays.fill(pixels, rowStart, rowStart + w, argb);
            }
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

        private void mapSpectrum(float[] inputSpectrum, float[] positions, int width, float[] interpolatedSpectrumOut) {
            switch (spectrumInterpolationMode) {
                case LINEAR -> mapSpectrumInterpolate(fftSpectrum, mappingPositions, false, interpolatedSpectrum);
                case EASING -> mapSpectrumInterpolate(fftSpectrum, mappingPositions, true, interpolatedSpectrum);
                case FIXED_WIDTH -> mapSpectrumFixedWidth(fftSpectrum, mappingPositions, getWidth(), fixedWidthBarCount, fixedWidthBarWidth, interpolatedSpectrum);
                case NEAREST -> mapSpectrumNearest(fftSpectrum, mappingPositions, interpolatedSpectrum);
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
     * @throws NullPointerException if the color is null
     */
    public void setUpperBarColor(Color color) {
        Objects.requireNonNull(color);
        this.upperBarColor = color;
        this.useColorProcessor = false;
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
     * @throws NullPointerException if the color is null
     */
    public void setLowerBarColor(Color color) {
        Objects.requireNonNull(color);
        this.lowerBarColor = color;
        this.useColorProcessor = false;
    }

    /**
     * Sets the volume color processor used by the visualizer.
     * 
     * @param volumeColorProcessor The volume color processor used by the visualizer
     * @throws NullPointerException if the volume color processor is null
     * @see ColorGradient
     * @see VolumeColorProcessor
     */
    public void setVolumeColorProcessor(VolumeColorProcessor volumeColorProcessor) {
        Objects.requireNonNull(volumeColorProcessor);
        this.volumeColorProcessor = volumeColorProcessor;
        this.useColorProcessor = true;
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
     * Sets the number of fixed width bars to divide the window into for the fixed width
     * interpolation mode.
     * 
     * @param count The number of fixed width bars to divide the window into
     * @throws IllegalArgumentException if the count is less than or equal to 0
     */
    public void setFixedWidthBarCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Fixed width bar count must be greater than 0.");
        }
        this.fixedWidthBarCount = count;
    }

    /**
     * Returns the number of fixed width bars to divide the window into for the fixed width
     * interpolation mode.
     * 
     * @return The number of fixed width bars to divide the window into
     */
    public int getFixedWidthBarCount() {
        return fixedWidthBarCount;
    }

    /**
     * Sets the width of the fixed width bars as a fraction of the window width.
     * The width is clamped to the range [0.0, 1.0] if it is outside this range.
     * 
     * @param width The width of the fixed width bars as a fraction of the window width
     */
    public void setFixedWidthBarWidth(float width) {
        this.fixedWidthBarWidth = MathUtilities.clamp(width, MIN_BAR_WIDTH, MAX_BAR_WIDTH);
    }

    /**
     * Returns the width of the fixed width bars as a fraction of the window width.
     * This value is clamped to the range [0.0, 1.0] if it is outside this range.
     * 
     * @return The width of the fixed width bars as a fraction of the window width
     */
    public float getFixedWidthBarWidth() {
        return fixedWidthBarWidth;
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
     * <p>The interpolation mode determines how the spectrum values are calculated
     * from the input samples. Different modes affect the smoothness and sharpness 
     * of the resulting spectrum, as well as the risk of aliasing.
     * 
     * <p><b>Interpolation Modes</b>:
     * <ul>
     *   <li>
     *     <b>{@link InterpolationMode#FIXED_WIDTH}</b>: Uses a fixed-width window
     *     for interpolation. Produces a generally smooth spectrum, but may introduce
     *     aliasing artifacts at high frequencies. Recommended for continuous spectra.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#NEAREST}</b>: Uses the nearest neighbor method
     *     for interpolation. Produces a sharp, pixelated spectrum. Can introduce
     *     aliasing. Useful when exact sample positions are important.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#LINEAR}</b>: Performs linear interpolation
     *     between samples. Produces a spectrum that is smooth but preserves some
     *     sharp transitions. Good general-purpose choice.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#EASING}</b>: Uses an easing function to interpolate
     *     between samples. Produces very smooth spectra with minimal harsh edges.
     *     Best when visual smoothness is prioritized over exact sample representation.
     *   </li>
     * </ul>
     * 
     * @param mode the interpolation mode to use
     * @throws NullPointerException if mode is null
     * @see InterpolationMode
     */
    public void setSpectrumInterpolationMode(InterpolationMode mode) {
        Objects.requireNonNull(mode);
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
     * Defines how the spectrum falls off when the audio signal decreases.
     * This controls how quickly the visualized bars drop back down
     * when the input level goes down or disappears.
     *
     * @param decayMode the decay behavior to apply
     * @throws NullPointerException if decayMode is null
     * @see DecayMode
     */
    public void setSpectrumDecayMode(DecayMode decayMode) {
        Objects.requireNonNull(decayMode);
        this.spectrumDecayMode = decayMode;
    }

    /**
     * Returns the current decay mode used by the spectrum visualizer.
     * 
     * @return The current decay mode used.
     * @see DecayMode
     */
    public DecayMode getSpectrumDecayMode() {
        return spectrumDecayMode;
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
     * @throws NullPointerException if the window type is null
     */
    public void setWindowType(WindowType type) {
        Objects.requireNonNull(type);
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
        JPanel panel = getPanel();

        SpectrumRender render = new SpectrumRender(panel.getWidth(), panel.getHeight());
        setRender(render);
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
