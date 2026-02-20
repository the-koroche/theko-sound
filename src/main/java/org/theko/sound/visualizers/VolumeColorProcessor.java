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

package org.theko.sound.visualizers;

/**
 * A functional interface that returns a color based on a volume.
 * 
 * @since 0.2.3-beta
 * @author Theko
 */
@FunctionalInterface
public interface VolumeColorProcessor {

    /**
     * Returns a color based on a volume.
     * 
     * @param volume The volume to process.
     * @return A color based on the volume, in 0xAARRGGBB format.
     */
    public int getColor(float volume);
}
