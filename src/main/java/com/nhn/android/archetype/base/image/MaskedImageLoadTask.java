package com.nhn.android.archetype.base.image;

import java.util.List;

import com.nhn.android.archetype.base.image.ImageHelper.MaskData;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class MaskedImageLoadTask extends AsyncTask<Void, Void, Bitmap> {
	private static M2baseLogger logger = M2baseLogger.getLogger(MaskedImageLoadTask.class);
	
	private MaskData maskData;
	private ImageTaskData taskData;
	
	public MaskedImageLoadTask(ImageTaskData taskData) {
		this.taskData = taskData;
		this.maskData = taskData.getMaskData();
	}

	private String getCacheKey() {
		return ImageHelper.getCacheKey(taskData.getUrl(), maskData);
	}
	
	private Bitmap getCachedBitmap() {
		String cacheKey = getCacheKey();
		
		Bitmap bm = ImageCacheManager.getFromCache(cacheKey);
		if (bm != null) {
			return bm;
		}
		
		bm = ImageCacheManager.getFromFile(cacheKey, taskData.getSampleWidth());	
		if (bm != null) {
			return bm;
		}
		
		return null;
	}
	
	private Bitmap loadImage() {
		ImageTaskData newTaskData = new ImageTaskData(taskData.getUrl(), taskData.getSampleWidth(), null, false);
		ImageLoadTask task = new ImageLoadTask(newTaskData);
		return task.doInBackground();
	}
	
	@Override
	protected Bitmap doInBackground(Void... params) {
		if (taskData == null) {
			return null;
		}
		
		logger.d("doInBackground: %s %s", taskData.getUrl(), taskData.getMaskKey());
		
		Bitmap bm = getCachedBitmap();
		if (bm != null) {
			return bm;
		}
		
		bm = loadImage();
		if (bm == null) {
			return null;
		}
		
		int width = maskData.getWidth();
		int height = maskData.getHeight();
		
		if (width == 0 || height == 0) {
			width = bm.getWidth();
			height = bm.getHeight();
		}
		
		Bitmap maskImage = ImageHelper.convertDrawable((Drawable) maskData.getMaskObject(), width, height);
		Bitmap maskedBitmap = ImageHelper.maskBitmap(bm, maskImage);
		
		if (maskedBitmap != null) {
			String cacheKey = getCacheKey();

			ImageCacheManager.putIntoFileCache(cacheKey, maskedBitmap, Bitmap.CompressFormat.PNG);
			ImageHelper.putIntoCache(cacheKey, maskedBitmap);

			return maskedBitmap;
		}
		
		return bm;
	}
	
	@Override
	protected void onPostExecute(final Bitmap result) {
		if (taskData == null) {
			return;
		}

		if (taskData.getUseFileCache()) {
			ImageHelper.putIntoCache(getCacheKey(), result);
		}
		
		ImageLoadManager.clearTask(taskData.getUrl(), taskData.getMaskKey());

		final List<ApiRequestListener<Bitmap, ApiResponse>> listenerList = taskData.getListener();

		if (listenerList != null) {
			final ApiResponse response = new ApiResponse();
			response.setCode("999");
			response.setMessage("Unexpected error");

			for (int i=listenerList.size() - 1; i >= 0; i--) {
				try {
					ApiRequestListener<Bitmap, ApiResponse> listener = listenerList.get(i);
					listenerList.remove(i);

					if (result != null) {
						listener.onSuccess(result);
					} else {
						listener.onError(response);
					}
				} catch (Exception e) {
					logger.e(e);
				}
			}

			taskData.getListener().clear();
		}

		super.onPostExecute(result);
	}
}
