package org.theko.sound.utility;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Indicates that the annotated method mutates (modifies in-place)
 * the float[][] sample buffer passed as argument.
 * 
 * <p>This is a developer-level hint. The method does not guarantee
 * output in a new array, and original input may be altered.
 * 
 * @see SamplesUtilities
 * 
 * @since v2.1.0
 * @author Theko
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface MutatesSamples {
    
}