package org.theko.sound.effects;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a sound effect does not have a fixed size.
 * This can be used to mark classes or components where the size of the effect
 * may vary dynamically.
 *
 * <p>Retention Policy:</p>
 * This annotation is retained at runtime, allowing it to be accessed via reflection.
 *
 * <p>Target:</p>
 * This annotation can only be applied to types (classes, interfaces, etc.).
 * 
 * @since v1.5.0
 * 
 * @author Theko
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NonFixedSizeEffect {
    
}
