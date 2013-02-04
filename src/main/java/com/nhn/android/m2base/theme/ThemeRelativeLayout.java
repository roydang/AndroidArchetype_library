package com.nhn.android.m2base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class ThemeRelativeLayout extends RelativeLayout {
	private boolean autoTheme = false;
	
	public ThemeRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeRelativeLayout(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
