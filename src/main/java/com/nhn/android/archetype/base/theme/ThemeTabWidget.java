package com.nhn.android.archetype.base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabWidget;

public class ThemeTabWidget extends TabWidget {
	public ThemeTabWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeTabWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeTabWidget(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
