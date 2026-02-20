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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for audio backend types.
 * This annotation is intended to be used on classes that implement the {@code AudioBackend} interface.
 * 
 * <p>Attributes:
 * <ul>
 *   <li>{@code name} - Specifies the name of the audio backend type. This is a required attribute.</li>
 *   <li>{@code version} - Specifies the version of the audio backend type. Defaults to "1.0" if not provided.</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * &#64;AudioBackendType(name = "ExampleBackend", version = "2.0")
 * public class ExampleAudioBackend implements AudioBackend {
 *     // Implementation details
 * }
 * </pre>
 * 
 * <p>Retention Policy:
 * This annotation is retained at runtime, allowing it to be accessed via reflection.
 * 
 * <p>Target:
 * This annotation can only be applied to types (classes, interfaces, etc.).
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
     * Indicates whether the audio backend supports output functionality. Defaults to true.
     */
    boolean output() default true;

    /**
     * Indicates whether the audio backend supports input functionality. Defaults to true.
     */
    boolean input() default true;
}
