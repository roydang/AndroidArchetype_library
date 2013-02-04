package com.nhn.android.m2base.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.nhn.android.m2base.object.ApiResponse;
import com.nhn.android.m2base.worker.listener.ApiRequestListener;

public class ImageTaskData {
	private AsyncTask<Void, Void, Bitmap> task;
	
	private String url;
	private int sampleWidth;
	private boolean useFileCache = true;
	private Object maskImageKey;
	private Drawable maskDrawable;	
	private boolean downloadOnly = false;
	
	private List<ApiRequestListener<Bitmap, ApiResponse>> listener;
	
	public ImageTaskData(String url, int sampleWidth, Object maskImageKey, Drawable maskDrawable, boolean downloadOnly) {
		this.url = url;
		this.sampleWidth = sampleWidth;
		this.maskImageKey = maskImageKey;
		this.maskDrawable = maskDrawable;
		this.downloadOnly = downloadOnly;
		this.listener = Collections.synchronizedList(new ArrayList<ApiRequestListener<Bitmap, ApiResponse>>());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getSampleWidth() {
		return sampleWidth;
	}

	public void setSampleWidth(int sampleWidth) {
		this.sampleWidth = sampleWidth;
	}

	public List<ApiRequestListener<Bitmap, ApiResponse>> getListener() {
		return listener;
	}

	public AsyncTask<Void, Void, Bitmap> getTask() {
		return task;
	}

	public void setTask(AsyncTask<Void, Void, Bitmap> task) {
		this.task = task;
	}

	public boolean getUseFileCache() {
		return useFileCache;
	}

	public void setUseFileCache(boolean useFileCache) {
		this.useFileCache = useFileCache;
	}

	public Object getMaskImageKey() {
		return maskImageKey;
	}

	public void setMaskImageKey(Object maskImageKey) {
		this.maskImageKey = maskImageKey;
	}

	public Drawable getMaskDrawable() {
		return maskDrawable;
	}

	public void setMaskDrawable(Drawable maskDrawable) {
		this.maskDrawable = maskDrawable;
	}

	public boolean getDownloadOnly() {
		return downloadOnly;
	}

	public void setDownloadOnly(boolean downloadOnly) {
		this.downloadOnly = downloadOnly;
	}
}
