/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.getScaledPositions;
import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.mapSpectrumCubic;
import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.mapSpectrumFixedWidth;
import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.mapSpectrumLinear;
import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.mapSpectrumNearest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;
import org.theko.sound.util.MathUtilities;

/**
 * Real-time audio spectrum visualizer that displays frequency content using FFT analysis.
 * Supports multiple interpolation modes, customizable coloring, and various spectrum processing options.
 * 
 * @see AudioVisualizer
 * @see #setFrequencyScale(float)
 * 
 * @since 0.2.2-beta
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
    private float finalSpectrumSmoothness = 0.6f;

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

            updateBuffers();

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

        private void updateBuffers() {
            int offset = getSamplesOffset();
            offset = Math.max(0, Math.min(offset, recentAudioWindow.length - fftWindowSize));
            
            fftInputBuffer = ensureBuffer(fftInputBuffer, fftWindowSize);
            System.arraycopy(recentAudioWindow, offset, fftInputBuffer, 0, Math.min(recentAudioWindow.length - offset, fftWindowSize));

            fftSpectrum = ensureBuffer(fftSpectrum, fftWindowSize / 2);
            getSpectrum(fftInputBuffer, fftSpectrum);

            interpolatedSpectrum = ensureBuffer(interpolatedSpectrum, getWidth());
            
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

            drawnSpectrum = ensureBuffer(drawnSpectrum, interpolatedSpectrum.length);

            float targetDecayFactor = spectrumDecayFactor;
            switch (spectrumDecayMode) {
                case MULTIPLY -> {
                    for (int i = 0; i < drawnSpectrum.length; i++) {
                        drawnSpectrum[i] = Math.max(drawnSpectrum[i] * targetDecayFactor, interpolatedSpectrum[i]);
                    }
                }
                case INTERPOLATE -> {
                    for (int i = 0; i < drawnSpectrum.length; i++) {
                        drawnSpectrum[i] = MathUtilities.lerp(
                            interpolatedSpectrum[i], drawnSpectrum[i], MathUtilities.clamp(targetDecayFactor, 0, 1));
                    }
                }
            }

            smoothSpectrum(drawnSpectrum, finalSpectrumSmoothness);
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

            real = ensureBuffer(real, fftLength);
            imag = ensureBuffer(imag, fftLength);
            
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
            float minNormalizerRoot = (float) Math.pow(minAmplitudeNormalizer, 1.0f / amplitudeExponent);
            
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
                case LINEAR -> mapSpectrumLinear(fftSpectrum, mappingPositions, interpolatedSpectrum);
                case EASING -> mapSpectrumCubic(fftSpectrum, mappingPositions, interpolatedSpectrum);
                case FIXED_WIDTH -> mapSpectrumFixedWidth(fftSpectrum, mappingPositions, getWidth(), fixedWidthBarCount, fixedWidthBarWidth, interpolatedSpectrum);
                case NEAREST -> mapSpectrumNearest(fftSpectrum, mappingPositions, interpolatedSpectrum);
            }
        }

        private void smoothSpectrum(float[] data, float smooth) {
            if (smooth <= 0.0f) return;
            int n = data.length;
            if (n <= 3) return;

            float k = MathUtilities.clamp(smooth, 0.0f, 1.0f);
            // forward pass
            for (int i = 1; i < n; i++) {
                data[i] = data[i - 1] * k + data[i] * (1f - k);
            }
            // backward pass
            for (int i = n - 2; i >= 0; i--) {
                data[i] = data[i + 1] * k + data[i] * (1f - k);
            }
        }

        private float[] ensureBuffer(float[] buffer, int size) {
            if (buffer == null || buffer.length != size) {
                buffer = new float[size];
            }
            return buffer;
        }
    }

    /**
     * Constructs a new {@code SpectrumVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     * @param resizeDelayMs The delay in milliseconds at which the render area is resized
     */
    public SpectrumVisualizer(float frameRate, int resizeDelay) {
        super(Type.REALTIME, frameRate, resizeDelay);
        addEffectControls(visualizerControls);
    }

    /**
     * Constructs a new {@code SpectrumVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     */
    public SpectrumVisualizer(float frameRate) {
        super(Type.REALTIME, frameRate);
        addEffectControls(visualizerControls);
    }

    /**
     * Constructs a new {@code SpectrumVisualizer}, with default frame rate.
     */
    public SpectrumVisualizer() {
        super(Type.REALTIME);
        addEffectControls(visualizerControls);
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
     * Sets the color used for the upper channel bar.
     *
     * @param color the color to use; must not be {@code null}
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public void setUpperBarColor(Color color) {
        this.upperBarColor = Objects.requireNonNull(color);
    }

    /**
     * Returns the color used for the upper channel bar.
     *
     * @return the upper bar color
     */
    public Color getUpperBarColor() {
        return upperBarColor;
    }

    /**
     * Sets the color used for the lower channel bar.
     *
     * @param color the color to use; must not be {@code null}
     * @throws NullPointerException if {@code color} is {@code null}
     */
    public void setLowerBarColor(Color color) {
        this.lowerBarColor = Objects.requireNonNull(color);
    }

    /**
     * Returns the color used for the lower channel bar.
     *
     * @return the lower bar color
     */
    public Color getLowerBarColor() {
        return lowerBarColor;
    }

    /**
     * Sets the volume color processor used by the visualizer.
     * Enables color processing automatically.
     *
     * @param volumeColorProcessor the processor to use; must not be {@code null}
     * @throws NullPointerException if {@code volumeColorProcessor} is {@code null}
     * @see ColorGradient
     * @see VolumeColorProcessor
     */
    public void setVolumeColorProcessor(VolumeColorProcessor volumeColorProcessor) {
        this.volumeColorProcessor = Objects.requireNonNull(volumeColorProcessor);
        this.useColorProcessor = true;
    }

    /**
     * Returns the volume color processor.
     *
     * @return the current volume color processor
     */
    public VolumeColorProcessor getVolumeColorProcessor() {
        return volumeColorProcessor;
    }

    /**
     * Enables or disables the use of a volume color processor.
     *
     * @param useColorProcessor {@code true} to use the processor, {@code false} otherwise
     */
    public void setUseColorProcessor(boolean useColorProcessor) {
        this.useColorProcessor = useColorProcessor;
    }

    /**
     * Returns the current value of the useColorProcessor flag.
     * 
     * @return true if the visualizer is using a color processor, false otherwise
     */
    public boolean isUsingColorProcessor() {
        return useColorProcessor;
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
     * Controls the final smoothing of the spectrum using a bidirectional IIR filter.
     * <p>0.0 – no smoothing (sharp, noisy spectrum)</p>
     * <p>1.0 – heavy smoothing (soft, blurred peaks)</p>
     * <p>Recommended values: 0.5–0.8</p>
     *
     * @param smoothness Smoothing strength in range [0..1]
     */
    public void setSpectrumSmoothness(float smoothness) {
        this.finalSpectrumSmoothness = MathUtilities.clamp(smoothness, 0, 1);
    }

    /**
     * Gets the final smoothing of the spectrum using a bidirectional IIR filter.
     * <p>0.0 – no smoothing (sharp, noisy spectrum)</p>
     * <p>1.0 – heavy smoothing (soft, blurred peaks)</p>
     *
     * @return Smoothing strength in range [0..1]
     */
    public float getSpectrumSmoothness() {
        return finalSpectrumSmoothness;
    }
    
    /**
    * Sets the minimum amplitude normalizer.
    * This value prevents the spectrum from disappearing when the signal is very quiet.
    * The actual normalizer used is scaled with the amplitude exponent.
    *
    * @param divider minimum amplitude normalizer, clamped between MIN_DIVIDER and MAX_DIVIDER
    */
    public void setMinAmplitudeNormalizer(float divider) {
        this.minAmplitudeNormalizer = MathUtilities.clamp(divider, MIN_DIVIDER, MAX_DIVIDER);
    }

    /**
     * Returns the minimum amplitude normalizer.
     *
     * @return minimum amplitude normalizer
     */
    public float getMinAmplitudeNormalizer() {
        return minAmplitudeNormalizer;
    }

    /**
     * Sets the amplitude exponent used to scale the spectrum.
     * Values > 1 emphasize stronger peaks, values < 1 make quieter components more visible.
     *
     * @param power amplitude exponent, clamped between MIN_AMPLITUDE_POWER and MAX_AMPLITUDE_POWER
     */
    public void setAmplitudeExponent(float power) {
        this.amplitudeExponent = MathUtilities.clamp(power, MIN_AMPLITUDE_POWER, MAX_AMPLITUDE_POWER);
    }

    /**
     * Returns the amplitude exponent.
     *
     * @return amplitude exponent
     */
    public float getAmplitudeExponent() {
        return amplitudeExponent;
    }

    /**
     * Sets the decay speed of the amplitude normalizer.
     * Determines how quickly the normalizer decreases over time:
     * - 0.0f: normalizer adapts instantly to new peaks (no decay)
     * - 1.0f: normalizer decreases quickly (fast decay)
     *
     * @param speed decay speed, clamped between MIN_DIVIDER_DECAY_SPEED and MAX_DIVIDER_DECAY_SPEED
     */
    public void setNormalizerDecaySpeed(float speed) {
        this.normalizerDecaySpeed = MathUtilities.clamp(speed, MIN_DIVIDER_DECAY_SPEED, MAX_DIVIDER_DECAY_SPEED);
    }

    /**
     * Returns the decay speed of the amplitude normalizer.
     *
     * @return decay speed
     */
    public float getNormalizerDecaySpeed() {
        return normalizerDecaySpeed;
    }

    /**
     * Sets the interpolation mode used to generate the visual spectrum.
     * <p>
     * The interpolation mode determines how the output spectrum values are computed
     * from the input samples. Different modes produce different visual results in terms of
     * smoothness, sharpness, and the preservation of peaks.
     * 
     * <p><b>Available Interpolation Modes:</b>
     * <ul>
     *   <li>
     *     <b>{@link InterpolationMode#FIXED_WIDTH}</b>: Divides the spectrum into fixed-width bars
     *     and assigns each bar the maximum value found within it. Produces a block-style
     *     spectrum with clearly defined peaks.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#NEAREST}</b>: Uses the nearest-neighbor method to map input
     *     samples to the output spectrum. Produces sharp, pixelated results with clearly defined peaks.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#LINEAR}</b>: Performs linear interpolation between neighboring
     *     samples. Produces a smoother spectrum than nearest-neighbor while preserving some sharp transitions.
     *     A balanced choice suitable for most real-time visualizations.
     *   </li>
     *   <li>
     *     <b>{@link InterpolationMode#EASING}</b>: Uses a smooth easing function (Catmull-Rom)
     *     to calculate intermediate values. Produces very smooth spectrum with minimal visual harshness.
     *     Best used when smoothness and aesthetics are prioritized over exact sample reproduction.
     *   </li>
     * </ul>
     *
     * @param mode the interpolation mode to use; must not be {@code null}
     * @throws NullPointerException if {@code mode} is {@code null}
     * @see InterpolationMode
     */
    public void setSpectrumInterpolationMode(InterpolationMode mode) {
        this.spectrumInterpolationMode = Objects.requireNonNull(mode);
    }

    /**
     * Returns the current interpolation mode used to generate the spectrum.
     * 
     * @return The current interpolation mode used.
     * @see #setSpectrumInterpolationMode(InterpolationMode)
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
        this.spectrumDecayMode = Objects.requireNonNull(decayMode);
    }

    /**
     * Returns the current decay mode used by the spectrum visualizer.
     * 
     * @return The current decay mode used.
     * @see #setSpectrumDecayMode(DecayMode)
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
        this.windowType = Objects.requireNonNull(type);
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
