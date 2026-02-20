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

package org.theko.sound.effects;

/**
 * This exception is thrown when an audio effect is incompatible with the expected type.
 * It extends the Exception class to provide a specific context for errors related to
 * audio effects that do not match the required type or format.
 * Usually this exception is used when an effect is offline processing,
 * but it is added to an AudioMixer that expects an real-time effect.
 * 
 * @since 2.0.0
 * @author Theko
 */
public class IncompatibleEffectTypeException extends Exception {
    
    public IncompatibleEffectTypeException() {
        super();
    }

    public IncompatibleEffectTypeException(String message) {
        super(message);
    }

    public IncompatibleEffectTypeException(Throwable cause) {
        super(cause);
    }

    public IncompatibleEffectTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}