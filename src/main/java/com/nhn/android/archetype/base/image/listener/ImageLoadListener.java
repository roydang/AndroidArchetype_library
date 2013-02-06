package com.nhn.android.archetype.base.image.listener;

import android.graphics.Bitmap;

import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

public interface ImageLoadListener extends ApiRequestListener<Bitmap, ApiResponse> {
}
