package org.theko.sound.effects;

import javax.swing.JFrame;
import javax.swing.Timer;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

public abstract class VisualAudioEffect extends AudioEffect {
    protected JFrame frame;
    protected final Timer repaintTimer;

    protected volatile byte[] data;
    protected volatile byte[] prevData = new byte[0];

    public VisualAudioEffect(AudioFormat audioFormat) {
        super(audioFormat);
        initializeFrame();
        repaintTimer = new Timer(1000 / 60, e -> repaint());
        repaintTimer.start();
    }

    public abstract void initializeFrame();
    public abstract void repaint();

    public JFrame getFrame() {
        return frame;
    }

    @Override
    public byte[] process(byte[] data) {
        this.prevData = this.data;
        this.data = data.clone();
        onDataReceived();
        return data;
    }

    protected void onDataReceived() { }
}
