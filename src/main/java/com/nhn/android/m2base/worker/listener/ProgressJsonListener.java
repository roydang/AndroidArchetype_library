package com.nhn.android.m2base.worker.listener;

public interface ProgressJsonListener extends JsonListener {
	void onProgress(int progress, int total);
}
