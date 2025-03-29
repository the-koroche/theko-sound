package org.theko.sound.effects;

import javax.swing.JFrame;
import javax.swing.Timer;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public abstract class VisualAudioEffect extends AudioEffect {
    protected JFrame frame;
    protected final Timer repaintTimer;

    public VisualAudioEffect(Type type, AudioFormat audioFormat) {
        super(type, audioFormat);
        initializeFrame();
        repaintTimer = new Timer(1000 / 60, e -> repaint());
        repaintTimer.start();
    }

    public abstract void initializeFrame();
    public abstract void repaint();

    public JFrame getFrame() {
        return frame;
    }
}
