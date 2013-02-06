package com.nhn.android.archetype.base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ThemeFrameLayout extends FrameLayout {
	public ThemeFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeFrameLayout(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
