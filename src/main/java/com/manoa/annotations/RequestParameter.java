package com.manoa.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation
 * Request Parameter
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParameter {
    String value() default "";
}