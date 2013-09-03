package com.xxx.easyxml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EasyRoot {
	   String name() default "";
}