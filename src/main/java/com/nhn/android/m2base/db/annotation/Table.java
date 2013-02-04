package com.nhn.android.m2base.db.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)  
public @interface Table {
	String name();
	String key();
	int version() default 1;
}
