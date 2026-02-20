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

import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.backends.AudioBackend;
import org.theko.sound.backends.AudioBackends;
import org.theko.sound.backends.dummy.DummyAudioBackend;
import org.theko.sound.backends.javasound.JavaSoundBackend;
import org.theko.sound.backends.wasapi.WASAPISharedBackend;
import org.theko.sound.codecs.AudioCodec;
import org.theko.sound.codecs.AudioCodecs;
import org.theko.sound.codecs.formats.WAVECodec;

/**
 * The AudioClassRegister class is responsible for registering audio backend and codec classes.
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
        WASAPISharedBackend.class,
        DummyAudioBackend.class
    );

    private static final Set<Class<? extends AudioCodec>> definedCodecs = Set.of(
        WAVECodec.class
    );

    private static Set<Class<? extends AudioBackend>> registeredBackends;
    private static Set<Class<? extends AudioCodec>> registeredCodecs;

    /**
     * Adds a new audio backend class to the backends set.
     * If the class is already in the set, a warning message is logged.
     *
     * @param backend The class of the audio backend to add.
     */
    public static void addBackend(Class<? extends AudioBackend> backend) {
        if (registeredBackends.contains(backend)) {
            logger.warn("Duplicate audio backend: {}", backend.getSimpleName());
            return;
        }
        registeredBackends.add(backend);
    }

    /**
     * Adds a new audio codec class to the codecs set.
     * If the class is already in the set, a warning message is logged.
     *
     * @param codec The class of the audio codec to add.
     */
    public static void addCodec(Class<? extends AudioCodec> codec) {
        if (registeredCodecs.contains(codec)) {
            logger.warn("Duplicate audio codec: {}", codec.getSimpleName());
            return;
        }
        registeredCodecs.add(codec);
    }

    /**
     * Registers all audio backend and codec classes.
     * This method should be called during the initialization phase of the application.
     */
    public static void registerClasses() {
        AudioBackends.registerBackends();
        AudioCodecs.registerCodecs();
    }

    /**
     * Returns a set of all audio backend classes available in the system.
     *
     * @return A set of audio backend classes.
     */
    public static Set<Class<? extends AudioBackend>> getBackendClasses() {
        return registeredBackends != null ? Collections.unmodifiableSet(registeredBackends) : definedBackends;
    }

    /**
     * Returns a set of all audio codec classes available in the system.
     *
     * @return A set of audio codec classes.
     */
    public static Set<Class<? extends AudioCodec>> getCodecClasses() {
        return registeredCodecs != null ? Collections.unmodifiableSet(registeredCodecs) : definedCodecs;
    }
}