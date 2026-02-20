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

package org.theko.sound.backends.javasound;

import javax.sound.sampled.Mixer;

import org.theko.sound.backends.AudioPortLink;

/**
 * An audio port link for Java Sound backend.
 * 
 * @see AudioPortLink
 * 
 * @since 0.2.4-beta
 * @author Theko
 */
public class JavaSoundPortLink implements AudioPortLink {
    private final Mixer.Info info;

    public JavaSoundPortLink(Mixer.Info info) {
        this.info = info;
    }

    public Mixer.Info getMixerInfo() {
        return info;
    }

    @Override
    public String toString() {
        return "JavaSoundPortLink{" +
                "info=" + info +
                '}';
    }
}
