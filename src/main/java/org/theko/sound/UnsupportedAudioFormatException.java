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

package org.theko.sound;

/**
 * This exception is thrown to indicate that an unsupported audio format
 * has been encountered during audio processing.
 * 
 * <p>It provides constructors to specify an error message, a cause, or both,
 * allowing for detailed exception handling and debugging.
 * 
 * <p>Usage examples include scenarios where an audio file format is not
 * recognized or supported by the application.
 * 
 * @since 1.0.0
 * @author Theko
 */
public class UnsupportedAudioFormatException extends Exception {
    public UnsupportedAudioFormatException() {
        super();
    }

    public UnsupportedAudioFormatException(String message) {
        super(message);
    }

    public UnsupportedAudioFormatException(Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
