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

package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.controls.AudioControl;
import org.theko.sound.controls.FloatControl;

/**
 * A real-time audio effect that delays the audio signal on each channel.
 * The delay amount can be adjusted for each channel.
 * 
 * @author Theko
 * @since 2.4.0
 */
public class ChannelDelayEffect extends AudioEffect {

    protected final FloatControl delayLeft = new FloatControl("Delay Left", 0.0f, 5.0f, 0.0f);
    protected final FloatControl delayRight = new FloatControl("Delay Right", 0.0f, 5.0f, 0.0f);
    
    protected final List<AudioControl> delayControls = List.of(delayLeft, delayRight);

    // Internal delay buffers for each channel
    private float[] bufferLeft = new float[1];
    private float[] bufferRight = new float[1];
    private int writePos = 0;

    public ChannelDelayEffect() {
        super(Type.REALTIME);
        addEffectControls(delayControls);
    }

    public FloatControl getDelayLeft() {
        return delayLeft;
    }

    public FloatControl getDelayRight() {
        return delayRight;
    }

    @Override
    public void effectRender(float[][] samples, int sampleRate) {
        if (samples.length < 2) return;

        int frameCount = samples[0].length;

        // Compute delay in samples
        int delaySamplesLeft = (int)(delayLeft.getValue() * sampleRate);
        int delaySamplesRight = (int)(delayRight.getValue() * sampleRate);
        int maxDelay = Math.max(delaySamplesLeft, delaySamplesRight);

        // Resize buffers if needed
        if (bufferLeft.length < maxDelay + frameCount) {
            bufferLeft = new float[maxDelay + frameCount];
            bufferRight = new float[maxDelay + frameCount];
            writePos = 0; // reset position to avoid index error
        }

        // Apply delay per channel
        for (int i = 0; i < frameCount; i++) {
            int readPosLeft = (writePos + bufferLeft.length - delaySamplesLeft) % bufferLeft.length;
            int readPosRight = (writePos + bufferRight.length - delaySamplesRight) % bufferRight.length;

            float inputL = samples[0][i];
            float inputR = samples[1][i];

            samples[0][i] = bufferLeft[readPosLeft];
            samples[1][i] = bufferRight[readPosRight];

            bufferLeft[writePos] = inputL;
            bufferRight[writePos] = inputR;

            writePos = (writePos + 1) % bufferLeft.length;
        }
    }
}
