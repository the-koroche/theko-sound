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

package org.theko.sound.codecs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for an audio codec type.
 * This annotation can be used to specify the name, file extension,
 * and version of an audio codec.
 * 
 * <p>Usage:
 * <pre>
 * &#64;AudioCodecType(name = "WAVE", extension = ".wav", version = "1.0")
 * public class WAVECodec {
 *     // Implementation details
 * }
 * </pre>
 * 
 * <p>Attributes:
 * <ul>
 *   <li><b>name</b> (optional): The name of the audio codec. Defaults to "Unknown".</li>
 *   <li><b>extension</b> (required): The file extension associated with the codec.</li>
 *   <li><b>version</b> (optional): The version of the codec. Defaults to "1.0".</li>
 * </ul>
 * 
 * <p>Retention Policy:
 * This annotation is retained at runtime.
 * 
 * <p>Target:
 * This annotation can only be applied to types (classes, interfaces, etc.).
 * 
 * @see AudioCodec
 * 
 * @since 1.3.1
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioCodecType {

    String name() default "Unknown";
    String[] extensions();
    String version() default "1.0";
}
