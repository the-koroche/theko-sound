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

/**
 * A functional interface that represents the link between
 * a {@code listener} and an {@code event}.
 * <p>
 * An {@code EventHandler} is responsible for invoking the correct
 * listener method when a specific {@link Event} occurs.
 * </p>
 *
 * <p>
 * In practice, this interface is usually implemented via
 * a method reference (e.g. {@code SoundSourceListener::onOpened})
 * or a lambda expression. It allows the {@link EventDispatcher}
 * to call the proper method on each registered listener without
 * hardcoding the mapping.
 * </p>
 *
 * <pre>{@code
 * // Example: mapping an event type to a handler
 * eventMap.put(SoundSourceNotifyType.OPENED,
 *              SoundSourceListener::onOpened);
 * }</pre>
 *
 * @param <L> the type of listener that receives the event
 * @param <E> the type of {@link Event} being dispatched
 *
 * @since 2.4.0
 * @author Theko
 */
@FunctionalInterface
public interface EventHandler<L, E extends Event> {
    
    /**
     * Handles the given event by invoking the appropriate method
     * on the specified listener.
     *
     * @param listener the listener to notify
     * @param event    the event to deliver to the listener
     */
    void handle(L listener, E event);
}