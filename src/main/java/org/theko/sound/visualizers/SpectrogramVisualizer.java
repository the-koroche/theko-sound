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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

import static org.theko.sound.visualizers.SpectrumVisualizationUtilities.*;

/**
 * A class that represents a spectrogram visualizer.
 * It can be used to display the spectrogram of an audio stream.
 * 
 * @see AudioVisualizer
 * 
 * @since 0.2.3-beta
 * @author Theko
 */
public class SpectrogramVisualizer extends AudioVisualizer {
    
    protected final FloatControl gainControl = new FloatControl("Gain", 0.0f, 2.0f, 1.0f);

    private float frequencyScale = 1.0f;
    private float updateTime = 0.01f;
    private VolumeColorProcessor volumeColorProcessor = ColorGradient.INFERNO_COLOR_MAP.getVolumeColorProcessor();

    private int channelToShow = 0;
    private WindowType windowType = WindowType.BLACKMAN_HARRIS;
    private int fftWindowSize = 8192;
    private float minAmplitudeNormalizer = 1.0f;
    private float amplitudeExponent = 2.0f;

    private float currentAmplitudeNormalizer = minAmplitudeNormalizer;
    private float normalizerDecaySpeed = 0.0f;

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
    
    protected SpectrogramRender spectrogramRender;

    protected class SpectrogramRender extends AudioVisualizer.Render {

        private BufferedImage spectrogramBuffer;
        private int[] specPixels;

        private float[] fftInputBuffer;
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
            fftInputBuffer = null;
            fftSpectrum = null;
            mappingPositions = null;
            interpolatedSpectrum = null;
            real = null; imag = null;
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

        @Override
        protected void paint(Graphics2D g2d) {
            long now = System.nanoTime();
            float deltaTime = (now - lastRenderTime) * 1e-9f;
            lastRenderTime = now;

            timeSinceLastShift += deltaTime;

            int shiftPixels = 0;
            while (timeSinceLastShift >= updateTime) {
                timeSinceLastShift -= updateTime;
                shiftPixels++;
            }

            if (recentAudioWindow == null) {
                return;
            }

            ensureSpectrogramBuffer();

            int w = getWidth();
            int h = getHeight();

            if (w <= 0 || h <= 0) return;

            int offset = getSamplesOffset();
            offset = Math.max(0, Math.min(offset, recentAudioWindow.length - fftWindowSize));

            if (fftInputBuffer == null || fftInputBuffer.length != fftWindowSize) {
                fftInputBuffer = new float[fftWindowSize];
            }
            System.arraycopy(recentAudioWindow, offset, fftInputBuffer, 0, fftWindowSize);

            if (fftSpectrum == null || fftSpectrum.length != fftWindowSize / 2)
                fftSpectrum = new float[fftWindowSize / 2];

            getSpectrum(fftInputBuffer, fftSpectrum);

            if (interpolatedSpectrum == null || interpolatedSpectrum.length != h)
                interpolatedSpectrum = new float[h];

            if (mappingPositions == null || mappingPositions.length != fftSpectrum.length) {
                mappingPositions = new float[fftSpectrum.length];
                getScaledPositions(mappingPositions, h, frequencyScale);
                lastFrequencyScale = frequencyScale;
            }

            if (lastFrequencyScale != frequencyScale) {
                getScaledPositions(mappingPositions, h, frequencyScale);
                lastFrequencyScale = frequencyScale;
            }

            mapSpectrumCubic(fftSpectrum, mappingPositions, interpolatedSpectrum);

            int[] pixels = specPixels;

            if (shiftPixels > 0) {
                int shift = Math.min(shiftPixels, w);

                for (int y = 0; y < h; y++) {
                    int base = y * w;

                    System.arraycopy(
                        pixels, base + shift,
                        pixels, base,
                        w - shift
                    );
                }
            }

            int lastX = w - 1;
            for (int y = 0; y < h; y++) {
                float amp = MathUtilities.clamp(interpolatedSpectrum[y], 0f, 1f);
                int col = volumeColorProcessor.getColor(amp);

                int row = (h - y - 1);
                pixels[row * w + lastX] = col;
            }

            g2d.drawImage(spectrogramBuffer, 0, 0, null);
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
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     * @param resizeDelayMs The delay in milliseconds at which the render area is resized
     */
    public SpectrogramVisualizer(float frameRate, int resizeDelay) {
        super(Type.REALTIME, frameRate, resizeDelay);
        addEffectControls(visualizerControls);
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer} with the specified frame rate.
     * @param frameRate The frame rate of the audio visualizer
     */
    public SpectrogramVisualizer(float frameRate) {
        super(Type.REALTIME, frameRate);
        addEffectControls(visualizerControls);
    }

    /**
     * Constructs a new {@code SpectrogramVisualizer}, with default frame rate.
     */
    public SpectrogramVisualizer() {
        super(Type.REALTIME);
        addEffectControls(visualizerControls);
    }

    /**
     * Returns the gain control of the spectogram visualizer.
     * The gain control allows adjusting the overall amplitude of the spectogram.
     * A value of 1.0f will not change the amplitude of the spectogram, while a value of 0.0f will mute it.
     * 
     * @return The gain control of the spectrogram visualizer.
     */
    public FloatControl getGainControl() {
        return gainControl;
    }

    /**
     * Sets the frequency scale of the spectogram visualizer.
     * The frequency scale is used to determine the frequency range of the
     * spectogram that is displayed. The frequency scale is relative to the
     * sampling rate of the audio signal, with a frequency scale of 1.0f
     * representing the full frequency range of the sampling rate and a
     * frequency scale of 0.5f representing half of the full frequency range.
     * A frequency scale of 2.0f would represent twice the full frequency range
     * of the sampling rate.
     * 
     * @param frequencyScale The frequency scale of the spectrogram visualizer.
     */
    public void setFrequencyScale(float frequencyScale) {
        this.frequencyScale = MathUtilities.clamp(frequencyScale, MIN_SCALE, MAX_SCALE);
    }

    /**
     * Returns the frequency scale of the spectogram visualizer.
     * 
     * @return The frequency scale of the spectogram visualizer.
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
     * @param updateTime The update time of the visualizer in milliseconds.
     * @see #getUpdateTime()
     */
    public void setUpdateTime(float updateTime) {
        this.updateTime = MathUtilities.clamp(updateTime, MIN_UPDATE_TIME, MAX_UPDATE_TIME);
    }

    /**
     * Returns the update time of the visualizer in milliseconds.
     * 
     * @return The update time of the visualizer in milliseconds.
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
     * Sets the minimum amplitude normalizer of the visualizer.
     * The minimum amplitude normalizer is used to scale the amplitude of the
     * spectrum before it is displayed. The amplitude normalizer is calculated
     * as follows: amplitude = Math.max(minNormalizerRoot, maxAmplitude) * 1.25f;
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
     * The amplitude exponent is used to scale the amplitude of the spectrum before it is
     * displayed. The amplitude exponent is applied as follows: amplitude =
     * Math.pow(amplitude, amplitudeExponent).
     * A higher amplitude exponent will result in a more dramatic amplitude scaling.
     * A lower amplitude exponent will result in a less dramatic amplitude scaling.
     * 
     * @param power The amplitude exponent of the visualizer.
     */
    public void setAmplitudeExponent(float power) {
        this.amplitudeExponent = MathUtilities.clamp(power, MIN_AMPLITUDE_POWER, MAX_AMPLITUDE_POWER);
    }

    /**
     * Returns the amplitude exponent of the visualizer.
     * 
     * @return the amplitude exponent of the visualizer.
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
     * Sets the size of the FFT used to generate the spectrogram.
     * A larger FFT size will provide a more detailed spectrogram, but will also
     * increase the computational cost of generating the spectrogram, and may cause
     * lower time resolution.
     * 
     * @param size The size of the FFT to use.
     */
    public void setFftWindowSize(int size) {
        this.fftWindowSize = MathUtilities.clamp(size, MIN_FFT_SIZE, MAX_FFT_SIZE);
        if (getPanel() != null) {
            getPanel().invalidate();
        }
    }

    /**
     * Returns the size of the FFT used to generate the spectrogram.
     * 
     * @return the size of the FFT used.
     */
    public int getFftWindowSize() {
        return fftWindowSize;
    }

    /**
     * Sets the window type used before the FFT is applied.
     * 
     * @param type the type of window function to apply
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
     * @return The channel to show (0-based index)
     * @see #setChannelToShow(int)
     */
    public int getChannelToShow() {
        return channelToShow;
    }

    @Override
    protected void initialize() {
        JPanel panel = getPanel();
        spectrogramRender = new SpectrogramRender(panel.getWidth(), panel.getHeight());
        setRender(spectrogramRender);
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
