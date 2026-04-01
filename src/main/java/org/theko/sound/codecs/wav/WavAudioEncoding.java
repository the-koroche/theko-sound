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

package org.theko.sound.codecs.wav;

/**
 * Enum representing the various audio encoding formats used in WAVE files.
 *
 * @since 0.3.0-beta
 * @author Theko
 */
public enum WavAudioEncoding {

    PCM_SIGNED_16,
    PCM_SIGNED_24,
    PCM_SIGNED_32,
    PCM_UNSIGNED_8,
    IEEE_FLOAT_32,
    IEEE_FLOAT_64,
    ALAW,
    ULAW
}
