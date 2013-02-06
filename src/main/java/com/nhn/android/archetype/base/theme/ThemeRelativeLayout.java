package com.nhn.android.archetype.base.theme;

import m2.android.archetype.example.AndroidArchetype_library.R;
import android.content.Context;
import android.content.res.TypedArray;
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
		
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Theme);
		
		final int count = a.getIndexCount();
		for (int i=0;i<count;i++) {
			int attr = a.getIndex(i);
			if (attr == R.styleable.Theme_theme_auto) {
				autoTheme = a.getBoolean(attr, false);
			}
		}
	}

	@Override
	public void setBackgroundResource(int resid) {
		if (autoTheme) {
			super.setBackgroundDrawable(ThemeHelper.getThemeDrawable(resid));
		} else {
			super.setBackgroundResource(resid);
		}
	}
}
