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

package org.theko.sound.backends.wasapi;

import org.theko.sound.backends.AudioPortLink;

/**
 * String identifier representation of a native audio port handle.
 * Used to identify audio ports link type in the WASAPI backend.
 * 
 * @since 2.3.2
 * @author Theko
 */
public class WASAPIPortHandle implements AudioPortLink {
    
    private final String handle;

    /**
     * Creates a new WASAPIPortHandle instance.
     * 
     * @param handle the native audio port handle identifier
     */
    public WASAPIPortHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Returns the native audio port handle identifier.
     * 
     * @return the native audio port handle identifier
     */
    public String getHandle() {
        return handle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WASAPIPortHandle that = (WASAPIPortHandle) o;
        return handle.equals(that.handle);
    }

    @Override
    public String toString() {
        return String.format("WASAPIPortHandle{handle='%s'}", handle);
    }
}
