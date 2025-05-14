package org.theko.sound.direct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for audio device types.
 * This annotation is intended to be used on classes that implement the {@code AudioDevice} interface.
 * 
 * <p>Attributes:</p>
 * <ul>
 *   <li>{@code name} - Specifies the name of the audio device type. This is a required attribute.</li>
 *   <li>{@code version} - Specifies the version of the audio device type. Defaults to "1.0" if not provided.</li>
 * </ul>
 * 
 * <p>Usage:</p>
 * <pre>
 * &#64;AudioDeviceType(name = "ExampleDevice", version = "2.0")
 * public class ExampleAudioDevice implements AudioDevice {
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
 * @see AudioDevice
 * @see AudioDeviceInfo
 * 
 * @since v1.4.1
 * 
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioDeviceType {
    String name();
    String version() default "1.0";
}
