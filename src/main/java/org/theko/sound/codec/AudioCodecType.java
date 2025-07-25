package org.theko.sound.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to define metadata for an audio codec type.
 * This annotation can be used to specify the name, file extension, 
 * and version of an audio codec.
 * 
 * <p>Usage:</p>
 * <pre>
 * &#64;AudioCodecType(name = "WAVE", extension = ".wav", version = "1.0")
 * public class WAVECodec {
 *     // Implementation details
 * }
 * </pre>
 * 
 * <p>Attributes:</p>
 * <ul>
 *   <li><b>name</b> (optional): The name of the audio codec. Defaults to "Unknown".</li>
 *   <li><b>extension</b> (required): The file extension associated with the codec.</li>
 *   <li><b>version</b> (optional): The version of the codec. Defaults to "1.0".</li>
 * </ul>
 * 
 * <p>Retention Policy:</p>
 * This annotation is retained at runtime.
 * 
 * <p>Target:</p>
 * This annotation can only be applied to types (classes, interfaces, etc.).
 * 
 * @see AudioCodec
 * 
 * @since v1.3.1
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioCodecType {

    String name () default "Unknown";
    String[] extensions ();
    String version () default "1.0";
}
