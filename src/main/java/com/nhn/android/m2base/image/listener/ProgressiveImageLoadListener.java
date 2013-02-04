package com.nhn.android.m2base.image.listener;

import android.graphics.Bitmap;

public interface ProgressiveImageLoadListener extends ImageLoadListener {
	void onPreload(Bitmap result);
}
