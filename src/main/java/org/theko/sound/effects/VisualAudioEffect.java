package org.theko.sound.effects;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public abstract class VisualAudioEffect extends AudioEffect {
    protected final Timer repaintTimer;

    public VisualAudioEffect(Type type, AudioFormat audioFormat) {
        super(type, audioFormat);
        initializePanel();
        repaintTimer = new Timer(1000 / 60, e -> repaint());
        repaintTimer.start();
    }

    public abstract void initializePanel();
    public abstract JPanel getPanel();
    protected abstract void repaint();
}
