package com.manoa.annotations;

import com.manoa.utils.FormatApi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * API annotation
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Api {
    FormatApi format() default FormatApi.SIMPLE;
}
