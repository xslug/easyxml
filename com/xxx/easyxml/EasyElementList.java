package com.xxx.easyxml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EasyElementList {
	String name() default "";
	
	boolean inline() default false;

	@SuppressWarnings("rawtypes")
	Class type() default void.class;
}
