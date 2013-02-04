package com.nhn.android.archetype.base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ThemeImageView extends ImageView {
	public ThemeImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeImageView(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
