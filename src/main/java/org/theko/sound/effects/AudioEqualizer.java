package org.theko.sound.effects;

import java.util.ArrayList;
import java.util.List;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;
import org.theko.sound.dsp.FFT;
import org.theko.sound.dsp.WindowFunction;
import org.theko.sound.dsp.WindowType;

/**
 * The {@code AudioEqualizer} class represents an audio effect that applies equalization
 * to audio samples in real-time. It divides the audio spectrum into multiple bands
 * and allows individual control over each band's frequency, gain, power, bandwidth,
 * and type.
 * 
 * <p>This class extends {@code AudioEffect} and processes audio samples using
 * Fast Fourier Transform (FFT) and Inverse Fast Fourier Transform (IFFT) to
 * manipulate the frequency domain representation of the audio signal.</p>
 * 
 * <p>Currently, only the {@code PEAK} band type is supported for processing.</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Divides the audio spectrum into 8 bands by default.</li>
 *   <li>Each band has configurable properties such as frequency, gain, power, bandwidth, and type.</li>
 *   <li>Applies a Blackman-Harris window function to the audio samples before FFT.</li>
 *   <li>Processes audio in the frequency domain and applies gain adjustments to the bands.</li>
 *   <li>Supports symmetric mirroring for frequency domain adjustments.</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>
 * AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
 * AudioEqualizer equalizer = new AudioEqualizer(format);
 * List<AudioEqualizer.Band> bands = equalizer.getBands();
 * bands.get(0).setGain(6.0f); // Boost the first band by 6 dB
 * </pre>
 * 
 * <h2>Inner Classes:</h2>
 * <ul>
 *   <li>{@code Band}: Represents an individual frequency band with properties such as frequency, gain, power, bandwidth, and type.</li>
 *   <li>{@code BandType}: Enum defining the types of bands (e.g., PEAK, HIGH_SHELF, LOW_SHELF, HIGH_PASS, LOW_PASS).</li>
 * </ul>
 * 
 * <h2>Constructor:</h2>
 * <ul>
 *   <li>{@code AudioEqualizer(AudioFormat audioFormat)}: Initializes the equalizer with the specified audio format and creates default bands.</li>
 * </ul>
 * 
 * <h2>Methods:</h2>
 * <ul>
 *   <li>{@code List<Band> getBands()}: Returns the list of bands.</li>
 *   <li>{@code Band getBand(int index)}: Returns the band at the specified index.</li>
 *   <li>{@code float[][] process(float[][] samples)}: Processes the audio samples and applies equalization.</li>
 * </ul>
 * 
 * <h2>Processing Details:</h2>
 * <p>The {@code process} method performs the following steps:</p>
 * <ol>
 *   <li>Copies the audio samples into a buffer and applies zero-padding.</li>
 *   <li>Applies a Blackman-Harris window function to the samples.</li>
 *   <li>Performs FFT to convert the samples to the frequency domain.</li>
 *   <li>Adjusts the gain of the frequency bands based on their configuration.</li>
 *   <li>Performs IFFT to convert the samples back to the time domain.</li>
 *   <li>Writes the processed samples back to the original buffer.</li>
 * </ol>
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
public class AudioEqualizer extends AudioEffect {
    private final List<Band> bands;
    private static final int BANDS_COUNT = 8;

    public static class Band {
        private float frequency;
        private float gain;
        private float power;
        private float bandwidth;
        private BandType type;

        public Band(float frequency, float gain, float power, float bandwidth, BandType type) {
            setFrequency(frequency);
            setGain(gain);
            setPower(power);
            setBandwidth(bandwidth);
            setType(type);
        }

        public float getFrequency() {
            return frequency;
        }

        public void setFrequency(float frequency) {
            this.frequency = Math.max(0, Math.min(22000, frequency));
        }

        public float getGain() {
            return gain;
        }

        public void setGain(float gain) {
            this.gain = Math.max(-24, Math.min(24, gain));
        }

        public float getPower() {
            return power;
        }

        public void setPower(float power) {
            this.power = power;
        }

        public float getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(float bandwidth) {
            this.bandwidth = Math.max(1, Math.min(22000, bandwidth));
        }

        public BandType getType() {
            return type;
        }

        public void setType(BandType type) {
            this.type = type;
        }
    }

    public enum BandType {
        PEAK,
        HIGH_SHELF,
        LOW_SHELF,
        HIGH_PASS,
        LOW_PASS
    }

    public AudioEqualizer(AudioFormat audioFormat) {
        super(Type.REALTIME, audioFormat);
        bands = new ArrayList<>(BANDS_COUNT);

        int freqStep = 22000 / BANDS_COUNT;
        for (int i = 0; i < BANDS_COUNT; i++) {
            float freq = freqStep * i + freqStep / 2f; // центр полосы
            bands.add(new Band(freq, 0.0f, 1.0f, freqStep, BandType.PEAK));
        }
    }

    public List<Band> getBands() {
        return bands;
    }

    public Band getBand(int index) {
        return bands.get(index);
    }

    @Override
    protected float[][] process(float[][] samples) {
        int channels = samples.length;
        int frames = samples[0].length;
        int roundedFrames = 1 << (32 - Integer.numberOfLeadingZeros(frames - 1));
        int spectrumSize = roundedFrames / 2;

        float[] real = new float[roundedFrames];
        float[] imag = new float[roundedFrames];

        float sampleRate = getAudioFormat().getSampleRate();

        for (int ch = 0; ch < channels; ch++) {
            // Copy samples
            System.arraycopy(samples[ch], 0, real, 0, frames);
            for (int i = frames; i < roundedFrames; i++) real[i] = 0.0f;
            for (int i = 0; i < roundedFrames; i++) imag[i] = 0.0f;

            // Window
            WindowFunction.apply(real, WindowType.BLACKMAN_HARRIS);

            // FFT
            FFT.fft(real, imag);

            // Process each band
            for (Band band : bands) {
                if (band.getType() != BandType.PEAK) continue; // пока только PEAK поддерживается

                float gainFactor = (float) Math.pow(10.0, band.getGain() / 20.0);
                float freq = band.getFrequency();
                float bw = band.getBandwidth();

                int bandStart = Math.max(0, (int) (spectrumSize * (freq - bw / 2) / sampleRate));
                int bandEnd = Math.min(spectrumSize, (int) (spectrumSize * (freq + bw / 2) / sampleRate));

                for (int j = bandStart; j < bandEnd; j++) {
                    real[j] *= gainFactor;
                    imag[j] *= gainFactor;

                    // symmetric mirror
                    int mirror = roundedFrames - j;
                    if (mirror < roundedFrames && mirror > j && mirror != j) {
                        real[mirror] *= gainFactor;
                        imag[mirror] *= gainFactor;
                    }
                }
            }

            // IFFT
            FFT.ifft(real, imag);

            // Write back
            System.arraycopy(real, 0, samples[ch], 0, frames);
        }

        return samples;
    }
}
