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

package org.theko.sound.effects;

/**
 * This exception is thrown when multiple effects of varying sizes are added
 * to an audio mixer.
 * <p>It indicates that the effects cannot be processed together due to their
 * differing output sizes.
 * 
 * @since v2.0.0
 * @author Theko
 */
public class MultipleVaryingSizeEffectsException extends Exception {
    
    public MultipleVaryingSizeEffectsException() {
        super();
    }

    public MultipleVaryingSizeEffectsException(String message) {
        super(message);
    }

    public MultipleVaryingSizeEffectsException(Throwable cause) {
        super(cause);
    }

    public MultipleVaryingSizeEffectsException(String message, Throwable cause) {
        super(message, cause);
    }
}