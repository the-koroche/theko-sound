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

package org.theko.sound.controls;

import java.text.Normalizer;

import org.theko.events.EventDispatcher;
import org.theko.events.ListenersManager;
import org.theko.events.ListenersManagerProvider;
import org.theko.sound.events.AudioControlEvent;
import org.theko.sound.events.AudioControlEventType;
import org.theko.sound.events.AudioControlListener;

/**
 * The {@code AudioControl} class serves as an abstract base class for audio control components.
 * It provides a common structure for managing audio controls with a name property.
 * 
 * <p>This class is intended to be extended by specific types of audio controls.
 * Subclasses must implement additional functionality as required.
 * 
 * @see BooleanControl
 * @see FloatControl
 * @see Controllable
 * 
 * @since 1.2.0
 * @author Theko
 */
public abstract class AudioControl implements ListenersManagerProvider<AudioControlEvent, AudioControlListener, AudioControlEventType> {

    protected final String name;
    protected final EventDispatcher<AudioControlEvent, AudioControlListener, AudioControlEventType> eventDispatcher = new EventDispatcher<>();

    /**
     * Constructs a new AudioControl with the specified name.
     *
     * @param name The name of the audio control.
     */
    public AudioControl(String name) {
        this.name = name;
        var eventMap = eventDispatcher.createEventMap();
        eventMap.put(AudioControlEventType.VALUE_CHANGED, AudioControlListener::onValueChanged);
        eventDispatcher.setEventMap(eventMap);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toLowerCase()
                        .trim()
                        .replaceAll("\\s+", "");
    }

    /**
     * Retrieves a normalized version of the control's name.
     * This is useful for comparing names without worrying about whitespace, case, or diacritics.
     * 
     * <p>Normalization is performed using the {@link Normalizer} class with the NFD form.
     * 
     * @return The normalized name of the control.
     */
    public String getComparableName() {
        return normalizeName(this.name);
    }

    /**
     * Checks if the specified name matches the normalized name of the control.
     * This is useful for comparing names without worrying about whitespace, case, or diacritics.
     * 
     * @param name The name to compare with the control's name.
     * @return true if the names match, false otherwise.
     */
    public boolean matchesName(String name) {
        return getComparableName().equals(normalizeName(name));
    }

    @Override
    public ListenersManager<AudioControlEvent, AudioControlListener, AudioControlEventType> getListenersManager() {
        return eventDispatcher.getListenersManager();
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
