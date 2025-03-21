package org.theko.sound.codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AudioCodecType {
    String name() default "Unknown";
    String extension();
    String version() default "1.0";
}
