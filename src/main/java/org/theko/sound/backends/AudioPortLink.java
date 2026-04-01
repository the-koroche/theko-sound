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

package org.theko.sound.backends;

import org.theko.sound.AudioPort;

/**
 * Interface for objects that hold backend-specific data used to identify an audio port.
 * <p>This helps link a port to the audio backend it belongs to.
 *
 * <p>Example:
 * <pre>
 * public class MyAudioBackendPortLink implements AudioPortLink {
 *     private final String portId;
 *
 *     public MyAudioBackendPortLink(String portId) {
 *         this.portId = portId;
 *     }
 *
 *     &#64;Override
 *     public Class&lt;? extends AudioBackend&gt; getRelatedBackend() {
 *         return MyAudioBackend.class;
 *     }
 * }
 * </pre>
 *
 * @see AudioPort
 *
 * @since 0.2.4-beta
 * @author Theko
 */
public interface AudioPortLink {

    /**
     * Returns the related audio backend class that is associated with this port link.
     * This method is used to determine which audio backend is associated with a given port link.
     *
     * @return the related audio backend class
     */
    Class<? extends AudioBackend> getRelatedBackend();
}
