package com.nhn.android.m2base.image.listener;

import android.graphics.Bitmap;

import com.nhn.android.m2base.object.ApiResponse;
import com.nhn.android.m2base.worker.listener.ApiRequestListener;

public interface ImageLoadListener extends ApiRequestListener<Bitmap, ApiResponse> {
}
