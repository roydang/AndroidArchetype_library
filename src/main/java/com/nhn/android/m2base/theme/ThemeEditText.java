package com.nhn.android.m2base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class ThemeEditText extends EditText {
	public ThemeEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeEditText(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
