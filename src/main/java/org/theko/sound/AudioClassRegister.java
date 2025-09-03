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

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backend.AudioBackend;
import org.theko.sound.backend.AudioBackends;
import org.theko.sound.backend.javasound.JavaSoundBackend;
import org.theko.sound.backend.wasapi.WASAPISharedBackend;
import org.theko.sound.codec.AudioCodec;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.formats.WAVECodec;

/**
 * The AudioClassRegister class is responsible for registering audio backend and codec classes.
 * 
 *
 * @since 2.0.0
 * @author Theko
 * 
 * @see AudioBackend
 * @see AudioCodec
 */
public final class AudioClassRegister {

    private AudioClassRegister() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    private static final Logger logger = LoggerFactory.getLogger(AudioClassRegister.class);

    private static final Set<Class<? extends AudioBackend>> definedBackends = Set.of(
        JavaSoundBackend.class,
        WASAPISharedBackend.class
    );

    private static final Set<Class<? extends AudioCodec>> definedCodecs = Set.of(
        WAVECodec.class
    );

    private static Set<Class<? extends AudioBackend>> scannedBackends;
    private static Set<Class<? extends AudioCodec>> scannedCodecs;

    /**
     * Adds a new audio backend class to the scanned backends set.
     * If the class is already in the set, a warning message is logged.
     *
     * @param backend The class of the audio backend to add.
     */
    public static void addBackend(Class<? extends AudioBackend> backend) {
        if (scannedBackends.contains(backend)) {
            logger.warn("Duplicate audio backend: {}", backend.getSimpleName());
            return;
        }
        scannedBackends.add(backend);
    }

    /**
     * Adds a new audio codec class to the scanned codecs set.
     * If the class is already in the set, a warning message is logged.
     *
     * @param codec The class of the audio codec to add.
     */
    public static void addCodec(Class<? extends AudioCodec> codec) {
        if (scannedCodecs.contains(codec)) {
            logger.warn("Duplicate audio codec: {}", codec.getSimpleName());
            return;
        }
        scannedCodecs.add(codec);
    }

    public static void registerClasses() {
        AudioBackends.registerBackends();
        AudioCodecs.registerCodecs();
    }

    /**
     * Returns a set of all audio backend classes available in the system.
     * If class scanning is enabled, it will include both predefined and scanned backends.
     * Otherwise, it will return only the predefined backends.
     *
     * @return A set of audio backend classes.
     */
    public static Set<Class<? extends AudioBackend>> getBackendClasses() {
        return scannedBackends != null ? Collections.unmodifiableSet(scannedBackends) : definedBackends;
    }

    /**
     * Returns a set of all audio codec classes available in the system.
     * If class scanning is enabled, it will include both predefined and scanned codecs.
     * Otherwise, it will return only the predefined codecs.
     *
     * @return A set of audio codec classes.
     */
    public static Set<Class<? extends AudioCodec>> getCodecClasses() {
        return scannedCodecs != null ? Collections.unmodifiableSet(scannedCodecs) : definedCodecs;
    }
}