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

package org.theko.sound.codecs;

/**
 * This exception is thrown to indicate an error during the creation of an audio codec.
 * It extends {@link AudioCodecException} to provide more specific context for codec creation failures.
 * 
 * @since 0.1.3-beta
 * @author Theko
 */
public class AudioCodecCreationException extends AudioCodecException {
    
    public AudioCodecCreationException() {
        super();
    }

    public AudioCodecCreationException(String message) {
        super(message);
    }

    public AudioCodecCreationException(Throwable cause) {
        super(cause);
    }

    public AudioCodecCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
