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

/**
 * This exception is thrown to indicate that an audio backend could not be found.
 * It extends the {@link Exception} class and provides constructors for various use cases.
 * 
 * <p>Possible scenarios where this exception might be used include:
 * <ul>
 *   <li>When attempting to access an audio backend that is not available.</li>
 *   <li>When the system fails to detect a required audio backend.</li>
 * </ul>
 * 
 * @since 0.1.0-beta
 * @author Theko
 */
public class AudioBackendNotFoundException extends Exception {
    
    public AudioBackendNotFoundException() {
        super();
    }

    public AudioBackendNotFoundException(String message) {
        super(message);
    }

    public AudioBackendNotFoundException(Throwable cause) {
        super(cause);
    }

    public AudioBackendNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
