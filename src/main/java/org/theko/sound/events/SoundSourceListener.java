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
import org.theko.sound.SoundSource;

/**
 * Listener for {@link SoundSource} events.
 *
 * @since 0.2.4-beta
 * @author Theko
 */
public interface SoundSourceListener extends Listener<SoundSourceEvent> {

    /**
     * Called when a sound source is opened, and is ready to be played.
     * @param event the event with the sound source
     */
    default void onOpen(SoundSourceEvent event) { }

    /**
     * Called when a sound source is closed.
     * @param event the event with the sound source
     */
    default void onClose(SoundSourceEvent event) { }

    /**
     * Called when a sound source is started.
     * @param event the event with the sound source
     */
    default void onStart(SoundSourceEvent event) { }

    /**
     * Called when a sound source is stopped.
     * @param event the event with the sound source
     */
    default void onStop(SoundSourceEvent event) { }

    /**
     * Called when the volume of a sound source is changed.
     * @param event the event with the sound source
     */
    default void onVolumeChange(SoundSourceEvent event) { }

    /**
     * Called when the pan of a sound source is changed.
     * @param event the event with the sound source
     */
    default void onPanChange(SoundSourceEvent event) { }

    /**
     * Called when the speed of a sound source is changed.
     * @param event the event with the sound source
     */
    default void onSpeedChange(SoundSourceEvent event) { }

    /**
     * Called when the position of a sound source is changed.
     * @param event the event with the sound source
     */
    default void onPositionChange(SoundSourceEvent event) { }

    /**
     * Called when the sound source loops.
     * @param event the event with the sound source
     */
    default void onLoop(SoundSourceEvent event) { }

    /**
     * Called when the data of a sound source has ended.
     * @param event the event with the sound source
     */
    default void onDataEnd(SoundSourceEvent event) { }
}