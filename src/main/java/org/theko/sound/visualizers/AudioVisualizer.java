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

import java.io.Closeable;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.effects.AudioEffect;

/**
 * The {@code AudioVisualizer} class serves as an abstract base class for creating
 * custom audio visualizers. It extends the {@code AudioEffect} class and provides
 * a framework for visualizing audio data in real-time.
 * 
 * @since 2.1.1
 * @author Theko
 */
public abstract class AudioVisualizer extends AudioEffect implements Closeable {
    
    private JPanel panel;
    private Timer repaintTimer;
    private final float frameRate;

    /** The audio samples buffer */
    private float[][] samplesBuffer;
    private int sampleRate;
    private int length;

    private long lastBufferUpdateTime;

    /**
     * Creates a new audio visualizer with the specified type and frame rate.
     * @param type The type of the audio visualizer (REALTIME or OFFLINE_PROCESSING)
     * @param frameRate The frame rate of the audio visualizer
     */
    public AudioVisualizer (Type type, float frameRate) {
        super(type);
        this.frameRate = frameRate;
        initialize();
        initializeTimer();
        repaintTimer.start();
    }

    private void initializeTimer () {
        repaintTimer = new Timer((int)(1000 / frameRate), e -> repaint());
    }

    /**
     * Clones the audio samples buffer and updates the sample rate and length.
     * @param samples The audio samples
     * @param sampleRate The sample rate of the audio samples
     * @param length The length of the audio samples
     */
    @Override
    public void effectRender (float[][] samples, int sampleRate) {
        int length = samples[0].length;
        samplesBuffer = new float[samples.length][length];
        for (int ch = 0; ch < samples.length; ch++) {
            System.arraycopy(samples[ch], 0, samplesBuffer[ch], 0, length);
        }
        this.sampleRate = sampleRate;
        this.length = samples[0].length;

        lastBufferUpdateTime = System.nanoTime();
        onBufferUpdate();
    }

    protected int getSamplesOffset () {
        long now = System.nanoTime();
        long delta = now - lastBufferUpdateTime;
        if (length > 0) {
            int elapsedSamples = (int) (delta * sampleRate / 1000000000f);
            return elapsedSamples % length;
        }
        return 0;
    }

    /**
     * Initializes the audio visualizer before the repaint timer starts.
     */
    protected abstract void initialize ();

    /**
     * Repaint task. Executes by the 'repaintTimer'.
     */
    protected abstract void repaint ();
    
    /**
     * Automatically called when the audio samples buffer is updated.
     */
    protected void onBufferUpdate () {}

    protected void onEnd () {}

    /**
     * Returns the panel of the audio visualizer.
     * @return The panel of the audio visualizer
     */
    public JPanel getPanel () {
        return panel;
    }

    /**
     * Returns the frame rate of the audio visualizer.
     * @return The frame rate of the audio visualizer
     */
    public float getFrameRate () {
        return frameRate;
    }

    /**
     * Closes the audio visualizer.
     */
    @Override
    public void close () {
        repaintTimer.stop();
        onEnd();
        panel.setVisible(false);
    }

    /**
     * Sets the panel of the audio visualizer.
     * @param panel The panel of the audio visualizer
     */
    protected void setPanel (JPanel panel) {
        this.panel = panel;
    }

    /**
     * Returns the repaint timer of the audio visualizer.
     * @return The repaint timer of the audio visualizer
     */
    protected Timer getRepaintTimer () {
        return repaintTimer;
    }

    /**
     * Returns the audio samples buffer.
     * @return The audio samples buffer
     */
    protected float[][] getSamplesBuffer () {
        return samplesBuffer;
    }

    /**
     * Returns the sample rate of the audio samples.
     * @return The sample rate of the audio samples
     */
    protected int getSampleRate () {
        return sampleRate;
    }

    /**
     * Returns the length of the audio samples.
     * @return The length of the audio samples
     */
    protected int getLength () {
        return length;
    }

    protected long getLastBufferUpdateTime () {
        return lastBufferUpdateTime;
    }
}
