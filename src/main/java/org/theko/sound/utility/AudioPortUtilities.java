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

package org.theko.sound.utility;

import java.util.HashMap;

import javax.sound.sampled.Mixer;

import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.javasound.JavaSoundBackend;
import org.theko.sound.backend.wasapi.WASAPINativeAudioPortHandle;
import org.theko.sound.backend.wasapi.WASAPISharedBackend;

/**
 * Utility class to map audio port link types to audio backends.
 * 
 * @since 2.3.2
 * @author Theko
 */
public final class AudioPortUtilities {

    private static final HashMap<Class<?>, Class<? extends AudioBackend>> backends = new HashMap<>();

    static {
        backends.put(Mixer.Info.class, JavaSoundBackend.class);
        backends.put(WASAPINativeAudioPortHandle.class, WASAPISharedBackend.class);
    }
    
    private AudioPortUtilities() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Returns the audio backend class associated with the specified audio port link type.
     * 
     * @param link The audio port link type.
     * @return The audio backend class, or null if not found.
     */
    public static Class<? extends AudioBackend> getBackendByLinkType(Class<?> link) {
        if (link == null) {
            return null;
        }
        for (Class<?> key : backends.keySet()) {
            if (key.isAssignableFrom(link)) {
                return backends.get(key);
            }
        }
        return null;
    }
}
