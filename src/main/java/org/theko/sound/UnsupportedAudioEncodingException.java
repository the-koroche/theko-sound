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
 * This exception is thrown to indicate that an unsupported audio encoding
 * format has been encountered.
 * 
 * <p>
 * The {@code UnsupportedAudioEncodingException} class extends the
 * {@code Exception} class and provides constructors to create an exception
 * instance with a custom message, a cause, or both.
 * 
 * 
 * <p>
 * This exception can be used in scenarios where audio processing or decoding
 * encounters an encoding format that is not supported by the application.
 * 
 * 
 * @since 1.0.0
 * @author Theko
 */
public class UnsupportedAudioEncodingException extends Exception {
    public UnsupportedAudioEncodingException() {
        super();
    }

    public UnsupportedAudioEncodingException(String message) {
        super(message);
    }

    public UnsupportedAudioEncodingException(Throwable cause) {
        super(cause);
    }

    public UnsupportedAudioEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
