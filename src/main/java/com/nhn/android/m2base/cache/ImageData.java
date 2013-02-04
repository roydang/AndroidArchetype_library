/*
 * @(#)ImageData.java $$version ${date}
 *
 * Copyright 2007 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.nhn.android.m2base.cache;

import android.graphics.Bitmap;


/**
 * 이미지 구조체.
 * @author nhn
 * 
 * 호환성 유지를 위해 남겨놓은 클래스 com.nhn.android.band.customview.UriImageView 로 사용 권고함
 */
@Deprecated
public class ImageData {
	public static final int PRIORTY_HIGH = 0;
	public static final int PRIORTY_MIDDLE = 10;
	public static final int PRIORTY_LOW = 100;

	// 요청할 이미지 주소.
	private String url;
	// 이미지 객체.
	private Bitmap bitmap;
	// 로직에서 필요한 참조 객체.
	private Object data;
	// 로직에서 필요한 참조 값.
	private int tag;
	private int priorty = PRIORTY_MIDDLE; // not implemented
	private ImageDataListener imageDataListener;

	public void clear() {
		this.bitmap = null;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Bitmap getBitmap() {
		return bitmap;
	}
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;

		if (getImageDataListener() != null) {
			getImageDataListener().onBitmapUpdated(this);
		}
	}
	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public int getTag() {
		return tag;
	}
	public void setTag(int tag) {
		this.tag = tag;
	}
	public int getPriorty() {
		return priorty;
	}
	public void setPriorty(int priorty) {
		this.priorty = priorty;
	}
	public ImageDataListener getImageDataListener() {
		return imageDataListener;
	}
	public void setImageDataListener(ImageDataListener imageDataListener) {
		this.imageDataListener = imageDataListener;
	}

	public interface ImageDataListener {
		void onBitmapUpdated(ImageData imagedata);
	}
}
