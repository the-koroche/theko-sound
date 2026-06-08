/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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
 * @since 0.2.4-beta
 * @author Theko
 */
public interface OutputLayerListener extends Listener<OutputLayerEvent> {

    /**
     * Called when an output layer is opened, and is ready to be played.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onOpen(OutputLayerEvent event) { }

    /**
     * Called when an output layer is re-opened.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onReopen(OutputLayerEvent event) { }

    /**
     * Called when an output layer is closed.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onClose(OutputLayerEvent event) { }

    /**
     * Called when an output layer is started.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onStart(OutputLayerEvent event) { }

    /**
     * Called when an output layer is stopped.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onStop(OutputLayerEvent event) { }

    /**
     * Called when an output layer is flushed.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onFlush(OutputLayerEvent event) { }

    /**
     * Called when an output layer is drained.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onDrain(OutputLayerEvent event) { }

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
     * Called when an output layer's playbacks thread is interrupted.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onPlaybackInterrupt(OutputLayerEvent event) { }

    /**
     * Called when an output layer's playback thread catches an exception.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onPlaybackException(OutputLayerEvent event) { }

    /**
     * Called when an output layer's device is invalidated or inactive.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onDeviceInvalidate(OutputLayerEvent event) { }

    /**
     * Called when an output layer's reopen attempt fails.
     * @param event the event carrying an immutable snapshot of the output layer state
     */
    default void onReopenFail(OutputLayerEvent event) { }
}
