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
 * This exception is thrown to indicate that an error occurred during the creation
 * of an audio backend.
 * 
 * <p>
 * The {@code AudioBackendCreationException} class provides constructors to specify
 * an error message, a cause, or both. It extends the {@code Exception} class,
 * making it a checked exception that must be declared in a method or constructor's
 * {@code throws} clause if it can be thrown during execution.
 * </p>
 * 
 * <p>
 * Usage examples include handling errors when initializing or configuring audio
 * backends in an application.
 * </p>
 *
 * @since v1.0.0
 * @author Theko
 */
public class AudioBackendCreationException extends Exception {
    
    public AudioBackendCreationException() {
        super();
    }

    public AudioBackendCreationException(String message) {
        super(message);
    }

    public AudioBackendCreationException(Throwable cause) {
        super(cause);
    }

    public AudioBackendCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
