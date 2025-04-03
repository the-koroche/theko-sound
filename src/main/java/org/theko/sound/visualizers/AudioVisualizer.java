package org.theko.sound.visualizers;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public abstract class AudioVisualizer extends AudioEffect {
    protected Timer repaintTimer;

    public AudioVisualizer(Type type, AudioFormat audioFormat) {
        super(type, audioFormat);
        initialize();
        repaintTimer = new Timer(1000 / 60, e -> repaint());
        repaintTimer.start();
    }

    /** Initialization task. Executes in this visualizer constructor. */
    public abstract void initialize();

    /** Repaint task. Executes by the 'repaintTimer'. */
    protected abstract void repaint();

    /** Finalization task. Executes by the parent handler. */
    public abstract void onEnd();

    public abstract JPanel getPanel();
}
