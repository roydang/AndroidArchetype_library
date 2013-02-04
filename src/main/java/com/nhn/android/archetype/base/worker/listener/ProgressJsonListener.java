package com.nhn.android.archetype.base.worker.listener;

public interface ProgressJsonListener extends JsonListener {
	void onProgress(int progress, int total);
}
