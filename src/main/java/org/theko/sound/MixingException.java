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
 * This exception is thrown when there is an error during the mixing of audio samples.
 * It extends RuntimeException to indicate that it is an unchecked exception.
 * 
 * @since 2.0.0
 * @author Theko
 */
public class MixingException extends RuntimeException {
    
    public MixingException() {
        super();
    }

    public MixingException(String message) {
        super(message);
    }

    public MixingException(Throwable cause) {
        super(cause);
    }

    public MixingException(String message, Throwable cause) {
        super(message, cause);
    }
}
