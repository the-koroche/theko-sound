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

package org.theko.sound.codecs;

/**
 * This exception is thrown when an audio codec cannot be found.
 * It extends {@link AudioCodecException} to provide more specific
 * error handling for codec-related issues.
 * 
 * <p>Possible use cases include scenarios where the application
 * attempts to process audio data with a codec that is unavailable
 * or unsupported.
 * 
 * @since 1.3.1
 * @author Theko
 */
public class AudioCodecNotFoundException extends AudioCodecException {
    
    public AudioCodecNotFoundException() {
        super();
    }

    public AudioCodecNotFoundException(String message) {
        super(message);
    }

    public AudioCodecNotFoundException(Throwable cause) {
        super(cause);
    }

    public AudioCodecNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
