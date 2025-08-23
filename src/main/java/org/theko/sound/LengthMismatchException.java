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

package org.theko.sound;

/**
 * This exception is thrown when there is a mismatch in the expected length of an audio sample array
 * and the actual length provided. It extends the Exception class to provide specific context for
 * length mismatches in audio processing operations.
 * 
 * @see ChannelsCountMismatchException
 * 
 * @since 2.0.0
 * @author Theko
 */
public class LengthMismatchException extends Exception {
    
    public LengthMismatchException() {
        super();
    }

    public LengthMismatchException(String message) {
        super(message);
    }

    public LengthMismatchException(Throwable cause) {
        super(cause);
    }

    public LengthMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}