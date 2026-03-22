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

package org.theko.sound;

public final class AudioChannelsConverter {

    private AudioChannelsConverter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void convertChannels(float[][] inputSamples, float[][] outputSamples,
                                       int sourceChannels, int targetChannels) {
        if (inputSamples == null || outputSamples == null) {
            throw new IllegalArgumentException("Input and output samples must be non-null.");
        }
        if (sourceChannels <= 0) {
            throw new IllegalArgumentException("Source channels count must be greater than zero.");
        }
        if (targetChannels <= 0) {
            throw new IllegalArgumentException("Target channels count must be greater than zero.");
        }
        if (sourceChannels > inputSamples.length) {
            throw new IllegalArgumentException("Source channels count cannot exceed input samples channels.");
        }
        if (outputSamples.length != targetChannels) {
            throw new IllegalArgumentException("Output samples array must have same number of channels as target channels count.");
        }

        int length = inputSamples[0].length;
        for (int ch = 0; ch < sourceChannels; ch++) {
            if (inputSamples[ch].length != length) {
                throw new IllegalArgumentException("All input channels must have the same length.");
            }
        }
        for (float[] channel : outputSamples) {
            if (channel.length != length) {
                throw new IllegalArgumentException("All output channels must have the same length as input channels.");
            }
        }

        if (sourceChannels == targetChannels) {
            for (int i = 0; i < sourceChannels; i++) {
                System.arraycopy(inputSamples[i], 0, outputSamples[i], 0, length);
            }
            return;
        }

        // Stereo -> Mono
        if (sourceChannels > 1 && targetChannels == 1) {
            float[] targetChannel = outputSamples[0];
            for (int ch = 0; ch < sourceChannels; ch++) {
                for (int i = 0; i < length; i++) {
                    targetChannel[i] += inputSamples[ch][i];
                }
            }
            float inv = 1f / sourceChannels;
            for (int i = 0; i < length; i++) {
                targetChannel[i] *= inv;
            }
        }
        // Mono -> Stereo
        else if (sourceChannels == 1 && targetChannels > 1) {
            for (int ch = 0; ch < targetChannels; ch++) {
                System.arraycopy(inputSamples[0], 0, outputSamples[ch], 0, length);
            }
        }
        else if (sourceChannels > targetChannels) {
            for (int ch = 0; ch < targetChannels; ch++) {
                float[] targetChannel = outputSamples[ch];
                for (int i = 0; i < length; i++) {
                    float sum = 0f;
                    for (int inCh = ch; inCh < sourceChannels; inCh += targetChannels) {
                        sum += inputSamples[inCh][i];
                    }
                    targetChannel[i] = sum / (float) Math.ceil((double) sourceChannels / targetChannels);
                }
            }
        }
        else if (sourceChannels < targetChannels) {
            for (int ch = 0; ch < targetChannels; ch++) {
                int srcCh = ch % sourceChannels;
                System.arraycopy(inputSamples[srcCh], 0, outputSamples[ch], 0, length);
            }
        }
        else {
            throw new UnsupportedOperationException(
                "Conversion from " + sourceChannels + " to " + targetChannels + " channels is not supported."
            );
        }
    }

    public static float[][] convertChannels(float[][] inputSamples, int sourceChannels, int targetChannels) {
        if (inputSamples == null) {
            throw new IllegalArgumentException("Input samples must be non-null.");
        }
        if (sourceChannels <= 0 || targetChannels <= 0) {
            throw new IllegalArgumentException("Source and target channels count must be greater than zero.");
        }
        if (sourceChannels > inputSamples.length) {
            throw new IllegalArgumentException("Source channels count cannot exceed input samples channels.");
        }
        int length = inputSamples[0].length;
        float[][] outputSamples = new float[targetChannels][length];
        convertChannels(inputSamples, outputSamples, sourceChannels, targetChannels);
        return outputSamples;
    }
}