package com.nhn.android.archetype.base.image.listener;

import android.graphics.Bitmap;

public interface ProgressiveImageLoadListener extends ImageLoadListener {
	void onPreload(Bitmap result);
}
