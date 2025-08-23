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

package org.theko.sound.codec;

/**
 * Represents an exception that occurs during audio codec operations.
 * This exception can be used to indicate various issues related to
 * encoding, decoding, or processing audio data.
 * 
 * @since 1.3.0
 * @author Theko
 */
public class AudioCodecException extends Exception {
    
    public AudioCodecException() {
        super();
    }

    public AudioCodecException(String message) {
        super(message);
    }

    public AudioCodecException(Throwable cause) {
        super(cause);
    }

    public AudioCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
