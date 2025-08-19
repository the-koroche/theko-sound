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
 * This exception is thrown to indicate that an audio port link type is not supported by the audio backend.
 * It extends the {@link AudioBackendException} class and provides constructors for various use cases.
 * 
 * @since v2.3.2
 * @author Theko
 */
public class UnsupportedPortLinkException extends AudioBackendException {

    public UnsupportedPortLinkException() {
        super();
    }

    public UnsupportedPortLinkException(String message) {
        super(message);
    }

    public UnsupportedPortLinkException(Throwable cause) {
        super(cause);
    }

    public UnsupportedPortLinkException(String message, Throwable cause) {
        super(message, cause);
    }
}
