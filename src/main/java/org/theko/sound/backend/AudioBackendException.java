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

package org.theko.sound.backend;

/**
 * Represents an exception that occurs in the context of audio backend operations.
 * This is a runtime exception that can be used to signal issues related to
 * audio backend functionality.
 * 
 * @since v1.0.0
 * @author Theko
 */
public class AudioBackendException extends RuntimeException {
    
    public AudioBackendException() {
        super();
    }

    public AudioBackendException(String message) {
        super(message);
    }

    public AudioBackendException(Throwable cause) {
        super(cause);
    }

    public AudioBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
