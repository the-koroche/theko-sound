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

package org.theko.sound.backends;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.theko.sound.util.PlatformUtilities.Platform;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for audio backend types.
 * This annotation is intended to be used on classes that implement the {@code AudioBackend} interface,
 * to work properly with {@link AudioBackends} manager.
 *
 * <p>Usage:
 * <pre>
 * &#64;AudioBackendType(name = "ExampleBackend",
 *          description = "An example audio backend",
 *          platforms = {Platform.WINDOWS})
 * public class ExampleAudioBackend implements AudioBackend {
 *     // Implementation details
 * }
 * </pre>
 *
 * @see AudioBackend
 * @see AudioBackendInfo
 *
 * @since 0.1.0-beta
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioBackendType {

    /**
     * Specifies the name of the audio backend type.
     */
    String name();

    /**
     * Specifies the description of the audio backend type.
     */
    String description() default "";

    /**
     * Specifies the supported platforms for the audio backend.
     * Defaults to an empty array, indicating cross-platform support.
     */
    Platform[] platforms() default {};

    /**
     * Specifies the priority of the audio backend type.
     * Higher values indicate higher priority.
     */
    int priority() default 0;

    /**
     * Whether this backend can be automatically selected by {@link AudioBackends} 
     * during platform detection.
     * <p>
     * Defaults to true. Set to false to prevent automatic selection (e.g., for Dummy backends).
     */
    boolean autoSelect() default true;

    /**
     * Indicates whether the audio backend supports output functionality.
     */
    boolean output() default true;

    /**
     * Indicates whether the audio backend supports input functionality.
     */
    boolean input() default true;
}
