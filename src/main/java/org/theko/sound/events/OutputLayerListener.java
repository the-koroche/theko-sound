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

package org.theko.sound.events;

import org.theko.events.Listener;
import org.theko.sound.AudioOutputLayer;

/**
 * Listener for {@link AudioOutputLayer} events.
 * 
 * @since 2.4.0
 * @author Theko
 */
public interface OutputLayerListener extends Listener<OutputLayerEvent, OutputLayerEventType> {

    /**
     * Called when an output layer is opened, and is ready to be played.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOpened(OutputLayerEvent event) { }

    /**
     * Called when an output layer is re-opened.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onReopened(OutputLayerEvent event) { }

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

    /**
     * Called when an output layer's output thread catches an exception.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOutputException(OutputLayerEvent event) { }

    /**
     * Called when an output layer's device is invalidated or inactive.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onDeviceInvalidated(OutputLayerEvent event) { }

    /**
     * Called when an output layer's reopen attempt fails.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onReopenFailed(OutputLayerEvent event) { }
}
