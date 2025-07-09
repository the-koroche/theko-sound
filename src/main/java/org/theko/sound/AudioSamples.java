package org.theko.sound;

@Deprecated
public class AudioSamples {
    
    public final float[][] samples; // [channels][frames]
    public final int sampleRate;

    public AudioSamples(float[][] samples, int sampleRate) {
        this.samples = samples;
        this.sampleRate = sampleRate;
    }

    public AudioSamples reverse() {
        float[][] reversed = new float[samples.length][samples[0].length];
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                reversed[ch][i] = samples[ch][samples[ch].length - 1 - i];
            }
        }
        return new AudioSamples(reversed, sampleRate);
    }

    public AudioSamples reversePolarity() {
        float[][] inverted = new float[samples.length][samples[0].length];
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                inverted[ch][i] = -samples[ch][i];
            }
        }
        return new AudioSamples(inverted, sampleRate);
    }

    public AudioSamples gained(float volume) {
        float[][] gained = new float[samples.length][samples[0].length];
        for (int ch = 0; ch < samples.length; ch++) {
            for (int i = 0; i < samples[ch].length; i++) {
                gained[ch][i] = samples[ch][i] * volume;
            }
        }
        return new AudioSamples(gained, sampleRate);
    }

    public AudioSamples swapChannels() {
        float[][] swapped = new float[samples.length][samples[0].length];
        int channels = samples.length;
        for (int ch = 0; ch < channels; ch++) {
            int swappedCh = (channels - 1) - ch;
            System.arraycopy(samples[ch], 0, swapped[swappedCh], 0, samples[ch].length);
        }
        return new AudioSamples(swapped, sampleRate);
    }

    public AudioSamples copy() {
        float[][] copy = new float[samples.length][samples[0].length];
        for (int ch = 0; ch < samples.length; ch++) {
            System.arraycopy(samples[ch], 0, copy[ch], 0, samples[ch].length);
        }
        return new AudioSamples(copy, sampleRate);
    }

    public int getLength() {
        int maxLength = -1;
        for (int i = 0; i < samples.length; i++) {
            maxLength = Math.max(samples[i].length, maxLength);
        }
        return maxLength;
    }

    public int getChannels() {
        return samples.length;
    }
}
