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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;
import org.theko.sound.structs.Range;
import org.theko.sound.util.MathUtilities;

/**
 * A class that represents a spectrogram visualizer.
 * It can be used to display the spectrogram of an audio stream.
 *
 * @see AudioVisualizer
 * @see GainedAudioVisualizer
 *
 * @since 0.2.3-beta
 * @author Theko
 */
public class SpectrogramVisualizer extends GainedAudioVisualizer {

    private float frequencyScale = 1.0f;
    private float updateTime = 0.01f;
    private VolumeColorProcessor volumeColorProcessor = ColorGradient.INFERNO_COLOR_MAP.getVolumeColorProcessor();

    // FFT parameters
    private int channelToShow = 0;
    private WindowType windowType = WindowType.HANN;
    private int fftWindowSize = 2048;

    // FFT normalization parameters
    private float minNormalizer = 0.02f;
    private float normalizerThreshold = 0.9f;
    private float normalizerDecayFactor = 0.95f;
    private float spectrumExponent = 1.5f;
    private float currentNormalizer = minNormalizer;

    // Limits for parameters
    public static final Range<Integer> FFT_SIZE_RANGE = new Range<>(64, 32768);
    public static final Range<Float> UPDATE_TIME_RANGE = new Range<>(0.001f, 1.0f);
    public static final Range<Float> SPECTRUM_DECAY_FACTOR_RANGE = new Range<>(0.0f, 1.0f);
    public static final Range<Float> SPECTRUM_SMOOTHNESS_RANGE = new Range<>(0.0f, 1.0f);

    public static final Range<Float> NORMALIZER_PEAK_FACTOR_RANGE = new Range<>(1.0f, 2.0f);
    public static final Range<Float> NORMALIZER_THRESHOLD_RANGE = new Range<>(0.0f, 1.0f);
    public static final Range<Float> NORMALIZER_DECAY_FACTOR_RANGE = new Range<>(0.0f, 1.0f);
    public static final Range<Float> SPECTRUM_EXPONENT_RANGE = new Range<>(0.0f, 6.0f);

    public static final Range<Float> FIXED_WIDTH_BAR_WIDTH_RANGE = new Range<>(0.0f, 1.0f);

    private float[] fftRingBuffer = null;
    private final AtomicBoolean shouldRedraw = new AtomicBoolean(false);

    protected class SpectrogramRender extends AudioVisualizer.Render {

        private BufferedImage spectrogramBuffer;
        private int[] specPixels;

        private float[] fftSpectrum;
        private float[] mappingPositions;
        private float[] interpolatedSpectrum;
        private float[] real, imag;
        private float lastFrequencyScale = -1;

        private float timeSinceLastShift = 0f;
        private long lastRenderTime = System.nanoTime();

        public SpectrogramRender(int width, int height) {
            super(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        protected void invalidate() {
            super.invalidate();
            lastFrequencyScale = -1;
            fftSpectrum = null;
            mappingPositions = null;
            interpolatedSpectrum = null;
            real = null; imag = null;
        }

        @Override
        protected void paint(Graphics2D g2d) {
            // Do not count the time spent while stopped, to prevent big time delta after resuming
            if (!shouldRedraw.get()) {
                lastRenderTime = System.nanoTime();
            }

            // Do nothing if there is not enough samples
            if (fftRingBuffer == null || fftRingBuffer.length == 0 || !shouldRedraw.get()) {
                if (spectrogramBuffer != null)
                    g2d.drawImage(spectrogramBuffer, 0, 0, null);
                return;
            }

            long now = System.nanoTime();
            float deltaTime = (now - lastRenderTime) * 1e-9f;
            lastRenderTime = now;

            timeSinceLastShift += deltaTime;

            int shiftPixels = 0;
            while (timeSinceLastShift >= updateTime) {
                timeSinceLastShift -= updateTime;
                shiftPixels++;
            }

            int w = getWidth();
            int h = getHeight();

            if (w <= 0 || h <= 0) return;

            ensureBuffers();
            int[] pixels = specPixels;


            int shift = Math.min(shiftPixels, w);
            if (shiftPixels > 0) {
                for (int y = 0; y < h; y++) {
                    int base = y * w;

                    System.arraycopy(
                        pixels, base + shift,
                        pixels, base,
                        w - shift
                    );
                }
            }

            for (int x = 0; x < shift; x++) { // x = 0..shift-1
                int targetX = w - shift + x;
                for (int y = 0; y < h; y++) {
                    float amp = MathUtilities.clamp(interpolatedSpectrum[y], 0f, 1f);
                    int row = h - y - 1;
                    pixels[row * w + targetX] = volumeColorProcessor.getColor(amp);
                }
            }

            g2d.drawImage(spectrogramBuffer, 0, 0, null);
            shouldRedraw.set(false);
        }

        private void ensureBuffers() {
            ensureSpectrogramBuffer();

            fftSpectrum = ensureBuffer(fftSpectrum, fftWindowSize / 2);
            getSpectrum(fftRingBuffer, fftSpectrum);

            interpolatedSpectrum = ensureBuffer(interpolatedSpectrum, getHeight());

            if (mappingPositions == null || mappingPositions.length != fftSpectrum.length) {
                mappingPositions = new float[fftSpectrum.length];
                lastFrequencyScale = frequencyScale;
                getScaledPositions(mappingPositions, getHeight(), frequencyScale);
            }

            if (lastFrequencyScale != frequencyScale) {
                lastFrequencyScale = frequencyScale;
                getScaledPositions(mappingPositions, getHeight(), frequencyScale);
            }
            mapSpectrumCubic(fftSpectrum, mappingPositions, interpolatedSpectrum);
        }

        private void ensureSpectrogramBuffer() {
            int w = getWidth();
            int h = getHeight();

            if (w <= 0 || h <= 0)
                return;

            if (spectrogramBuffer == null) {
                spectrogramBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                specPixels = ((DataBufferInt) spectrogramBuffer.getRaster().getDataBuffer()).getData();
                return;
            }

            if (spectrogramBuffer.getWidth() != w || spectrogramBuffer.getHeight() != h) {

                BufferedImage old = spectrogramBuffer;
                BufferedImage newBuf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = newBuf.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(old, 0, 0, w, h, null);
                g.dispose();

                spectrogramBuffer = newBuf;
                specPixels = ((DataBufferInt) newBuf.getRaster().getDataBuffer()).getData();
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

            for (int i = 0; i < fftLength / 2; i++) {
                float magnitude = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);

                float value = (float) Math.log1p(magnitude);
                outputSpectrum[i] = (float) Math.pow(value, spectrumExponent);
                if (outputSpectrum[i] > maxAmplitude) maxAmplitude = outputSpectrum[i];
            }

            maxAmplitude = Math.max(minNormalizer, maxAmplitude);

            if (maxAmplitude > currentNormalizer) {
                currentNormalizer = maxAmplitude;
            }
            if (currentNormalizer > maxAmplitude * normalizerThreshold) {
                currentNormalizer *= normalizerDecayFactor;
            }
            if (currentNormalizer < minNormalizer) currentNormalizer = minNormalizer;

            for (int i = 0; i < fftLength / 2; i++) {
                outputSpectrum[i] = MathUtilities.clamp(outputSpectrum[i] / currentNormalizer, 0.0f, 1.0f);
            }
        }
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     * @param resizeDelay The delay in milliseconds at which the render area is resized
     */
    public SpectrogramVisualizer(float frameRate, int resizeDelay) {
        super(Type.REALTIME, frameRate, resizeDelay);
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     */
    public SpectrogramVisualizer(float frameRate) {
        super(Type.REALTIME, frameRate);
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer}, with default frame rate.
     */
    public SpectrogramVisualizer() {
        super(Type.REALTIME);
    }

    // Overridden methods

    @Override
    protected void initialize() {
        JPanel panel = getPanel();
        SpectrogramRender render = new SpectrogramRender(panel.getWidth(), panel.getHeight());
        setRender(render);
    }

    @Override
    protected void onBufferUpdate() {
        float[] newSamples = getSamplesBuffer()[channelToShow];
        fftRingBuffer = ensureBuffer(fftRingBuffer, fftWindowSize);
        int bufferLength = fftRingBuffer.length;

        int copyLen = Math.min(newSamples.length, bufferLength);

        // Move old data to the left, freeing up space for new data
        if (copyLen < bufferLength) {
            System.arraycopy(fftRingBuffer, copyLen, fftRingBuffer, 0, bufferLength - copyLen);
        }

        // Copy the new samples to the end of the buffer
        System.arraycopy(newSamples, newSamples.length - copyLen, fftRingBuffer, bufferLength - copyLen, copyLen);

        shouldRedraw.set(true);
    }

    private float[] ensureBuffer(float[] buffer, int size) {
        if (buffer == null || buffer.length != size) {
            buffer = new float[size];
        }
        return buffer;
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
     * @param frequencyScale The frequency scale of the spectrum visualizer, must be non-negative
     */
    public void setFrequencyScale(float frequencyScale) {
        this.frequencyScale = Math.max(frequencyScale, 0.0f);
    }

    /**
     * Returns the frequency scale of the spectrum visualizer.
     * The frequency scale is used to determine the frequency range of the
     * spectrum that is displayed.
     *
     * @return The frequency scale of the spectrum visualizer
     */
    public float getFrequencyScale() {
        return frequencyScale;
    }

    /**
     * Sets the update time of the visualizer in milliseconds.
     * The update time is the time interval between consecutive updates of the
     * visualizer. The update time is used to control how often the visualizer
     * is updated.
     *
     * @param updateTime The update time of the visualizer in milliseconds
     * @see #getUpdateTime()
     */
    public void setUpdateTime(float updateTime) {
        this.updateTime = UPDATE_TIME_RANGE.clamp(updateTime);
    }

    /**
     * Returns the update time of the visualizer in milliseconds.
     *
     * @return The update time of the visualizer in milliseconds
     */
    public float getUpdateTime() {
        return updateTime;
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
    }

    /**
     * Returns the volume color processor used by the visualizer.
     *
     * @return The volume color processor used by the visualizer
     */
    public VolumeColorProcessor getVolumeColorProcessor() {
        return volumeColorProcessor;
    }

    /**
    * Sets the minimum amplitude normalizer.
    * This value prevents the spectrum from disappearing when the signal is very quiet.
    * The actual normalizer used is scaled with the amplitude exponent.
    *
    * @param min minimum amplitude normalizer
    * @throws IllegalArgumentException if the divider is less than or equal to 0
    */
    public void setMinNormalizer(float min) {
        if (min <= 0) {
            throw new IllegalArgumentException("Minimum normalizer must be greater than 0.");
        }
        this.minNormalizer = min;
    }

    /**
     * Returns the minimum amplitude normalizer.
     *
     * @return minimum amplitude normalizer
     */
    public float getMinNormalizer() {
        return minNormalizer;
    }

    /**
     * Sets the amplitude exponent used to scale the spectrum.
     * Values &gt; 1 emphasize stronger peaks, values &lt; 1 make quieter components more visible.
     *
     * @param power amplitude exponent, clamped in range [0.25, 6.0]
     */
    public void setSpectrumExponent(float power) {
        this.spectrumExponent = SPECTRUM_EXPONENT_RANGE.clamp(power);
    }

    /**
     * Returns the amplitude exponent.
     *
     * @return amplitude exponent
     */
    public float getSpectrumExponent() {
        return spectrumExponent;
    }

    /**
     * Sets the decay speed of the amplitude normalizer.
     * Determines how quickly the normalizer decreases over time:
     * <p>- 0.0f: normalizer adapts instantly to new peaks (no decay)
     * <p>- 1.0f: normalizer decreases quickly (fast decay)
     *
     * @param decay decay speed, clamped in range [0.0, 1.0]
     */
    public void setNormalizerFallSpeed(float decay) {
        this.normalizerDecayFactor = NORMALIZER_DECAY_FACTOR_RANGE.clamp(decay);
    }

    /**
     * Returns the decay speed of the amplitude normalizer.
     *
     * @return decay speed
     */
    public float getNormalizerDecayFactor() {
        return normalizerDecayFactor;
    }

    /**
     * Sets the size of the FFT used to generate the spectrum.
     * A larger FFT size will provide a more detailed spectrum, but will also
     * increase the computational cost of generating the spectrum, and may cause
     * lower time resolution.
     *
     * @param size The size of the FFT to use, must be a power of 2, in range [64, 32768]
     */
    public void setFftWindowSize(int size) {
        if (!MathUtilities.isPowerOf2(size)) {
            throw new IllegalArgumentException("FFT size must be a power of 2");
        }
        this.fftWindowSize = FFT_SIZE_RANGE.clamp(size);
    }

    /**
     * Returns the size of the FFT used to generate the spectrum.
     *
     * @return the size of the FFT used
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
     * @return The window type used
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
     * @return The channel to show (0-based index)
     * @see #setChannelToShow(int)
     */
    public int getChannelToShow() {
        return channelToShow;
    }
}
