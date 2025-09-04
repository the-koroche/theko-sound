package org.theko.sound.event;

import org.theko.sound.AudioOutputLayer;

/**
 * Listener for {@link AudioOutputLayer} events.
 * 
 * @since 2.4.0
 * @author Theko
 */
public interface OutputLayerListener {

    /**
     * Called when an output layer is opened, and is ready to be played.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOpened(OutputLayerEvent event) { }

    /**
     * Called when an output layer is closed.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onClosed(OutputLayerEvent event) { }

    /**
     * Called when an output layer is started.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onStarted(OutputLayerEvent event) { }

    /**
     * Called when an output layer is stopped.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onStopped(OutputLayerEvent event) { }

    /**
     * Called when an output layer is flushed.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onFlushed(OutputLayerEvent event) { }

    /**
     * Called when an output layer is drained.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onDrained(OutputLayerEvent event) { }

    /**
     * Called when an output layer is underrun.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onUnderrun(OutputLayerEvent event) { }

    /**
     * Called when an output layer is overrun.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOverrun(OutputLayerEvent event) { }

    /**
     * Called when length mismatch is detected in the processing thread.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onLengthMismatch(OutputLayerEvent event) { }

    /**
     * Called when an output layer's backend is closed, while the processing thread is running.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onUncheckedClose(OutputLayerEvent event) { }

    /**
     * Called when an output layer's processing is interrupted.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onProcessingInterrupted(OutputLayerEvent event) { }

    /**
     * Called when an output layer's output thread is interrupted.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOutputInterrupted(OutputLayerEvent event) { }

    /**
     * Called when an output layer's processing thread catches an exception.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onRenderException(OutputLayerEvent event) { }
}
