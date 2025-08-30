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
 * Marker interface for event type identifiers.
 * <p>
 * Any {@code enum} that represents a specific set of event kinds
 * (e.g. {@code SoundSourceNotifyType}, {@code PlaybackNotifyType})
 * should implement this interface to become compatible with
 * the {@link EventDispatcher}.
 * </p>
 *
 * <p>
 * The type parameter {@code <E>} binds the event type enum
 * to the concrete subclass of {@link Event} it produces,
 * ensuring compile-time type safety. For example:
 * </p>
 *
 * <pre>{@code
 * enum SoundSourceNotifyType implements EventType<SoundSourceEvent> {
 *     OPENED, CLOSED
 * }
 * }</pre>
 *
 * <p>
 * With this setup, the dispatcher will only accept
 * {@link SoundSourceEvent} instances when dispatching
 * {@code SoundSourceNotifyType} events, preventing type mismatches.
 * </p>
 *
 * @param <E> the concrete {@link Event} type associated with this notification
 *
 * @since 2.4.0
 * @author Theko
 */
public interface EventType<E extends Event> {
}