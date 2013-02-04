package com.nhn.android.archetype.base.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.util.internal.M2baseLogger;
import com.nhn.android.archetype.base.util.internal.M2baseUtility;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {
	private static M2baseLogger logger = M2baseLogger.getLogger(ImageLoadTask.class);

	private ImageTaskData taskData;
	private HttpURLConnection connection;
	
	public ImageLoadTask(ImageTaskData taskData) {
		this.taskData = taskData;
	}
	
	public void disconnect() {
		try {
			if (connection != null) {
				connection.disconnect();
			}
			
			connection = null;
		} catch (Exception e) {
			
		}
		
		this.cancel(true);
	}
	
	public Bitmap postSync() {
		return doInBackground();
	}
	
	protected Bitmap doInBackground(Void... params) {
		ImageTaskData taskData = getTaskData();
		
		if (taskData == null || M2baseUtility.isNullOrEmpty(taskData.getUrl())) {
			return null;
		}

		if (taskData.getUseFileCache()) {
			Bitmap bm = ImageCacheManager.getFromCache(getCacheKey());
			if (bm != null) {
				return bm;
			}
			
			bm = ImageCacheManager.getFromFile(getCacheKey(), taskData.getSampleWidth());	
			if (bm != null) {
				return bm;
			}
		}
		
		if (!taskData.getUrl().startsWith("http")) {
			return null;
		}

		InputStream is = null;
		Bitmap bitmap = null;
		
		try {
			if (M2baseUtility.isUnderFroyo()) {
				System.setProperty("http.keepAlive", "false");
			}
			URI uri = new URI(taskData.getUrl());
			String request = uri.toASCIIString();
			URL url = new URL(request);

			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			
			long startTime = System.currentTimeMillis();
			
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				logger.d("responseCode: %s", responseCode);
				return null;
			}
			
			is = connection.getInputStream();
			
			logger.d("execute: %sms", (System.currentTimeMillis() - startTime));

			ImageCacheManager.putIntoCache(getCacheKey(), is);
		} catch (Exception e) {
			logger.d("url: %s", getCacheKey());
			logger.e(e);
			if (connection != null) {
				connection.disconnect(); 
			}
		} catch (Error err) {
			logger.e(err);
			if (connection != null) {
				connection.disconnect();
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}

		if (bitmap == null && taskData.getDownloadOnly() == false) {
			bitmap = ImageCacheManager.getFromFile(getCacheKey(), taskData.getSampleWidth());
		}

		return bitmap;
	}
	
	protected String getCacheKey() {
		return ImageHelper.getCacheKey(taskData.getUrl(), null);
	}
	
	public ImageTaskData getTaskData() {
		return taskData;
	}
	
	@Override
	protected void onPostExecute(final Bitmap result) {
		if (taskData == null) {
			return;
		}

		if (taskData.getUseFileCache() && result != null) {
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

					if (listener != null) {
						if (taskData.getDownloadOnly() || result != null) {
							listener.onSuccess(result);
						} else {
							listener.onError(response);
						}
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