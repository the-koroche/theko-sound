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
 * This exception is thrown to indicate that a required audio codec is missing.
 * It extends {@link RuntimeException}, allowing it to be used as an unchecked exception.
 *
 * <p>Possible use cases include scenarios where an application attempts to process
 * audio data but the necessary codec for decoding or encoding is unavailable.
 * 
 * @since 1.3.0
 * @author Theko
 */
public class MissingAudioCodecException extends RuntimeException {
    
    public MissingAudioCodecException() {
        super();
    }

    public MissingAudioCodecException(String message) {
        super(message);
    }

    public MissingAudioCodecException(Throwable cause) {
        super(cause);
    }

    public MissingAudioCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
