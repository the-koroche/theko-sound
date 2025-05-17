package org.theko.sound.backend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for audio backend types.
 * This annotation is intended to be used on classes that implement the {@code AudioBackend} interface.
 * 
 * <p>Attributes:</p>
 * <ul>
 *   <li>{@code name} - Specifies the name of the audio backend type. This is a required attribute.</li>
 *   <li>{@code version} - Specifies the version of the audio backend type. Defaults to "1.0" if not provided.</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * &#64;AudioBackendType(name = "ExampleBackend", version = "2.0")
 * public class ExampleAudioBackend implements AudioBackend {
 *     // Implementation details
 * }
 * </pre>
 * 
 * <p>Retention Policy:</p>
 * This annotation is retained at runtime, allowing it to be accessed via reflection.
 * 
 * <p>Target:</p>
 * This annotation can only be applied to types (classes, interfaces, etc.).
 * 
 * @see AudioBackend
 * @see AudioBackendInfo
 * 
 * @since v1.0.0
 * 
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioBackendType {
    String name();
    String version() default "1.0";
}
