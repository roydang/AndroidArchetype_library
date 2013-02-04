package com.nhn.android.m2base.cache.listener;

import com.nhn.android.m2base.cache.FileCache;

public interface FileCacheListener {
	void onSuccess(FileCache cache);
	void onError();
}
