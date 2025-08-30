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

package org.theko.sound.control;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.theko.sound.event.AudioControlEvent;
import org.theko.sound.event.AudioControlListener;
import org.theko.sound.event.EventDispatcher;
import org.theko.sound.event.EventHandler;
import org.theko.sound.event.EventType;

/**
 * The {@code AudioControl} class serves as an abstract base class for audio control components.
 * It provides a common structure for managing audio controls with a name property.
 * 
 * <p>This class is intended to be extended by specific types of audio controls.
 * Subclasses must implement additional functionality as required.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Stores a name for the audio control.</li>
 *   <li>Provides a method to retrieve the name.</li>
 *   <li>Overrides {@code toString()} to provide a string representation of the audio control.</li>
 * </ul>
 * 
 * @see BooleanControl
 * @see FloatControl
 * @see Controllable
 * 
 * @since 1.2.0
 * @author Theko
 */
public abstract class AudioControl {

    protected final String name;
    protected final EventDispatcher<AudioControlEvent, AudioControlListener, AudioControlNotifyType> eventDispatcher = new EventDispatcher<>();

    protected enum AudioControlNotifyType implements EventType<AudioControlEvent> {
        VALUE_CHANGE
    }

    /**
     * Constructs a new AudioControl with the specified name.
     *
     * @param name The name of the audio control.
     */
    public AudioControl(String name) {
        this.name = name;
        Map<AudioControlNotifyType, EventHandler<AudioControlListener, AudioControlEvent>> eventMap = new HashMap<>();
        eventMap.put(AudioControlNotifyType.VALUE_CHANGE, AudioControlListener::onValueChanged);
        eventDispatcher.setEventMap(eventMap);
    }


    /**
     * Registers a new listener for audio control events.
     * @param listener The listener to register.
     */
    public void addListener(AudioControlListener listener) {
        eventDispatcher.addListener(listener);
    }

    /**
     * Registers a new consumer when an audio control value changes.
     * @param consumer The consumer to register.
     */
    public void addChangeListener(Consumer<AudioControlEvent> consumer) {
        eventDispatcher.addListener(new AudioControlListener() {
            @Override
            public void onValueChanged(AudioControlEvent event) {
                consumer.accept(event);
            }
        });
    }

    /**
     * Removes a previously registered listener for audio control events.
     * @param listener The listener to remove.
     */
    public void removeListener(AudioControlListener listener) {
        eventDispatcher.removeListener(listener);
    }

    /**
     * Retrieves a list of registered audio control listeners.
     *
     * @return An unmodifiable list of audio control listeners.
     */
    public List<AudioControlListener> getListeners() {
        return eventDispatcher.getListeners();
    }

    /**
     * Retrieves the name of this audio control.
     *
     * @return The name of the audio control.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of the AudioControl object,
     * displaying the name of the control.
     *
     * @return A string that represents the audio control.
     */
    @Override
    public String toString() {
        return String.format("AudioControl{Name: %s}", name);
    }
}
