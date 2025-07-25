package org.theko.sound.effects;

import java.util.List;

import org.theko.sound.control.AudioControl;
import org.theko.sound.control.FloatControl;

public class ChannelDelayEffect extends AudioEffect {

    protected final FloatControl delayLeft = new FloatControl("Delay Left", 0.0f, 5.0f, 0.0f);
    protected final FloatControl delayRight = new FloatControl("Delay Right", 0.0f, 5.0f, 0.0f);
    
    protected final List<AudioControl> delayControls = List.of(delayLeft, delayRight);

    // Internal delay buffers for each channel
    private float[] bufferLeft = new float[1];
    private float[] bufferRight = new float[1];
    private int writePos = 0;

    public ChannelDelayEffect () {
        super(Type.REALTIME);
        addControls(delayControls);
    }

    public FloatControl getDelayLeft () {
        return delayLeft;
    }

    public FloatControl getDelayRight () {
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
