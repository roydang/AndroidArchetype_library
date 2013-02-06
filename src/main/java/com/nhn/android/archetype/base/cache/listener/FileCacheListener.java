package com.nhn.android.archetype.base.cache.listener;

import com.nhn.android.archetype.base.cache.FileCache;

public interface FileCacheListener {
	void onSuccess(FileCache cache);
	void onError();
}
