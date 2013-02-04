package com.nhn.android.archetype.base.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.nhn.android.archetype.base.image.ImageHelper.MaskData;
import com.nhn.android.archetype.base.object.ApiResponse;
import com.nhn.android.archetype.base.worker.listener.ApiRequestListener;

public class ImageTaskData {
	private AsyncTask<Void, Void, Bitmap> task;
	
	private String url;
	private int sampleWidth;
	private boolean useFileCache = true;
	private MaskData maskData;
	private boolean downloadOnly = false;
	
	private List<ApiRequestListener<Bitmap, ApiResponse>> listener;
	
	public ImageTaskData(String url, int sampleWidth, MaskData maskData, boolean downloadOnly) {
		this.url = url;
		this.sampleWidth = sampleWidth;
		this.maskData = maskData;
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
	
	public MaskData getMaskData() {
		return maskData;
	}

	public void setMaskData(MaskData maskData) {
		this.maskData = maskData;
	}
	
	public String getMaskKey() {
		if (maskData != null) {
			return maskData.getMaskKey();
		}
		
		return null;
	}

	public boolean getDownloadOnly() {
		return downloadOnly;
	}

	public void setDownloadOnly(boolean downloadOnly) {
		this.downloadOnly = downloadOnly;
	}
}
