package com.nhn.android.archetype.base.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class ThemeListView extends ListView {
	public ThemeListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initAttrs(attrs);
	}

	public ThemeListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs);
	}

	public ThemeListView(Context context) {
		super(context);
	}
	
	protected void initAttrs(AttributeSet attrs) {
		ThemeHelper.overrideAttributes(this, attrs);
	}
}
