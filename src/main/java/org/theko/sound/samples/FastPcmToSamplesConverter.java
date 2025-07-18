package org.theko.sound.samples;

import org.theko.sound.AudioFormat;

public class FastPcmToSamplesConverter {
    public static float[][] toSamples(byte[] data, AudioFormat audioFormat) {
        int channels = audioFormat.getChannels();
        int sampleSizeInBits = audioFormat.getSampleSizeInBits();
        boolean isBigEndian = audioFormat.isBigEndian();

        if (sampleSizeInBits != 16) {
            throw new IllegalArgumentException("Only 16-bit PCM supported for now");
        }

        int bytesPerSample = sampleSizeInBits / 8;
        int frameSize = bytesPerSample * channels;
        int totalFrames = data.length / frameSize;

        float[][] samples = new float[channels][totalFrames];

        for (int frame = 0; frame < totalFrames; frame++) {
            int frameStart = frame * frameSize;

            for (int ch = 0; ch < channels; ch++) {
                int sampleStart = frameStart + ch * bytesPerSample;

                int sample;
                if (isBigEndian) {
                    sample = (short) ((data[sampleStart] << 8) | (data[sampleStart + 1] & 0xFF));
                } else {
                    sample = (short) ((data[sampleStart + 1] << 8) | (data[sampleStart] & 0xFF));
                }

                // Нормируем в float [-1.0, 1.0]
                samples[ch][frame] = sample / 32768f;
            }
        }

        return samples;
    }
}
