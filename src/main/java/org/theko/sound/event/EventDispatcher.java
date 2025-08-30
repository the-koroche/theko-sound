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

package org.theko.sound.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic event dispatcher that manages listeners and routes events
 * to the appropriate handler methods.
 *
 * <p>
 * This class decouples event producers (who call {@link #dispatch(Enum, Event)})
 * from event consumers (listeners). Each dispatcher is configured with:
 * </p>
 * <ul>
 *   <li>An {@link Event} type {@code E}, describing the data carried by events.</li>
 *   <li>A listener interface type {@code L}, which declares handler methods
 *       (e.g. {@code SoundSourceListener}).</li>
 *   <li>An enum type {@code T} that implements {@link EventType}, which acts
 *       as the set of event kinds (e.g. {@code SoundSourceNotifyType}).</li>
 * </ul>
 *
 * <p>
 * The dispatcher maintains a list of listeners and a mapping from event kinds
 * ({@code T}) to handler functions ({@link EventHandler}). When an event is
 * dispatched, the corresponding handler is invoked on each registered listener.
 * </p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * enum SoundSourceNotifyType implements Notifiable<SoundSourceEvent> {
 *     OPENED, CLOSED
 * }
 *
 * interface SoundSourceListener {
 *     void onOpened(SoundSourceEvent e);
 *     void onClosed(SoundSourceEvent e);
 * }
 *
 * EventDispatcher<SoundSourceEvent, SoundSourceListener, SoundSourceNotifyType> dispatcher =
 *     new EventDispatcher<>();
 *
 * Map<SoundSourceNotifyType, EventHandler<SoundSourceListener, SoundSourceEvent>> map = new HashMap<>();
 * map.put(SoundSourceNotifyType.OPENED, SoundSourceListener::onOpened);
 * map.put(SoundSourceNotifyType.CLOSED, SoundSourceListener::onClosed);
 *
 * dispatcher.setEventMap(map);
 *
 * dispatcher.addListener(new SoundSourceListener() {
 *     public void onOpened(SoundSourceEvent e) {
 *         System.out.println("Source opened at " + e.getTimestamp());
 *     }
 *     public void onClosed(SoundSourceEvent e) {
 *         System.out.println("Source closed at " + e.getTimestamp());
 *     }
 * });
 *
 * dispatcher.dispatch(SoundSourceNotifyType.OPENED, new SoundSourceEvent(...));
 * }</pre>
 *
 * @param <E> the base type of all events handled by this dispatcher
 * @param <L> the listener interface that defines event handling methods
 * @param <T> the enum of event kinds, implementing {@link EventType}
 *
 * @since 2.4.0
 * @author Theko
 */
public class EventDispatcher<E extends Event, L, T extends Enum<T> & EventType<E>> {

    private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final List<L> listeners = new ArrayList<>();
    private final Map<T, EventHandler<L, E>> eventMap = new HashMap<>();

    /**
     * Replaces the current event-handler mapping with the given one.
     *
     * @param map mapping from event kinds to handler functions
     */
    public void setEventMap(Map<T, EventHandler<L, E>> map) {
        eventMap.clear();
        eventMap.putAll(map);
    }

    /**
     * Registers a new listener.
     *
     * @param listener the listener to add
     */
    public void addListener(L listener) {
        logger.debug("Adding listener: {}", listener);
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(L listener) {
        logger.debug("Removing listener: {}", listener);
        listeners.remove(listener);
    }

    /**
     * Returns an unmodifiable view of all currently registered listeners.
     *
     * @return the registered listeners
     */
    public List<L> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Dispatches an event of the given type to all registered listeners.
     * <p>
     * If no handler is registered for the specified event type, this call
     * has no effect.
     * </p>
     *
     * @param eventType the type of event (enum constant)
     * @param event the event instance to pass to listeners
     */
    public void dispatch(T eventType, E event) {
        EventHandler<L, E> consumer = eventMap.get(eventType);
        logger.debug("Dispatching event: {}", eventType.name());
        if (consumer != null) {
            for (L listener : listeners) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Dispatching event to listener: {}", listener);
                }
                consumer.handle(listener, event);
            }
        }
    }
}