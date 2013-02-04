package com.nhn.android.m2base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class ThemeImageButton extends ImageButton {
	public ThemeImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeImageButton(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
