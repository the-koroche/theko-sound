package org.theko.sound.visualizers;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.theko.sound.AudioEffect;
import org.theko.sound.AudioFormat;

/**
 * The {@code AudioVisualizer} class serves as an abstract base class for creating
 * custom audio visualizers. It extends the {@code AudioEffect} class and provides
 * a framework for visualizing audio data in real-time.
 * 
 * <p>This class includes a timer that triggers the repaint task at a fixed frame
 * rate of 60 frames per second. Subclasses must implement the abstract methods
 * to define specific behavior for initialization, repainting, finalization, and
 * providing a visual panel.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Automatic repainting at a fixed frame rate using a {@code Timer}.</li>
 *   <li>Abstract methods for initialization, repainting, and cleanup tasks.</li>
 *   <li>Support for providing a custom {@code JPanel} for visualization.</li>
 * </ul>
 * 
 * <p>Subclasses are expected to:
 * <ul>
 *   <li>Implement the {@code initialize()} method to perform setup tasks.</li>
 *   <li>Implement the {@code repaint()} method to define the visual update logic.</li>
 *   <li>Implement the {@code onEnd()} method to handle cleanup when the visualizer ends.</li>
 *   <li>Implement the {@code getPanel()} method to return the visualizer's panel.</li>
 * </ul>
 * 
 * @see AudioEffect
 * @see Timer
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
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
