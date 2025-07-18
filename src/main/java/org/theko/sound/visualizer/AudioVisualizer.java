package org.theko.sound.visualizer;

import java.io.Closeable;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.effects.AudioEffect;

/**
 * The {@code AudioVisualizer} class serves as an abstract base class for creating
 * custom audio visualizers. It extends the {@code AudioEffect} class and provides
 * a framework for visualizing audio data in real-time.
 * 
 * @since v2.1.1
 * @author Theko
 */
public abstract class AudioVisualizer extends AudioEffect implements Closeable {
    
    protected JPanel panel;
    protected Timer repaintTimer;
    protected final float frameRate;

    /** The audio samples buffer */
    protected float[][] samplesBuffer;
    protected int sampleRate;
    protected int length;

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
    public void render (float[][] samples, int sampleRate, int length) {
        samplesBuffer = new float[samples.length][length];
        for (int ch = 0; ch < samples.length; ch++) {
            System.arraycopy(samples[ch], 0, samplesBuffer[ch], 0, length);
        }
        this.sampleRate = sampleRate;
        this.length = length;

        onBufferUpdate();
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
     * Closes the audio visualizer.
     */
    @Override
    public void close () {
        repaintTimer.stop();
        onEnd();
    }
}
