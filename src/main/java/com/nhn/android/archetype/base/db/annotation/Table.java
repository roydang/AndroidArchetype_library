package com.nhn.android.archetype.base.db.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)  
public @interface Table {
	String name();
	String key();
	int version() default 1;
}
